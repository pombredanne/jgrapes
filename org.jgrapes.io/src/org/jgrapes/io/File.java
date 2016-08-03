/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrapes.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jgrapes.core.Component;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Eof;
import org.jgrapes.io.events.FileOpened;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.OpenFile;
import org.jgrapes.io.events.Read;
import org.jgrapes.io.events.Write;
import org.jgrapes.io.util.ManagedBufferQueue;
import org.jgrapes.io.util.ManagedByteBuffer;

/**
 * A component that reads from or writes to a file. Read events generated
 * by this component are processed by an independent event pipeline.
 * 
 * @author Michael N. Lipp
 */
public class File extends Component {

	private int bufferSize;

	private Map<Connection, FileConnection> connections = Collections
	        .synchronizedMap(new WeakHashMap<>());
	
	/**
	 * Create a new instance using the given size for the read buffers.
	 * 
	 * @param channel the component's channel. Used for sending {@link Read}
	 * events and receiving {@link Write} events 
	 * @param bufferSize the size of the buffers used for reading
	 */
	public File(Channel channel, int bufferSize) {
		super (channel);
		this.bufferSize = bufferSize;
	}

	/**
	 * Create a new instance using the default buffer size of 4096.
	 * 
	 * @param channel the component's channel. Used for sending {@link Read}
	 * events and receiving {@link Write} events 
	 */
	public File(Channel channel) {
		this(channel, 4096);
	}
	
	@Handler
	public void onOpen(OpenFile event) throws InterruptedException {
		if (connections.containsKey(event.getConnection())) {
			event.getConnection().getResponsePipeline().fire(new IOError(event, 
					new IllegalStateException("File is already open.")));
		}
		connections.put(event.getConnection(), new FileConnection(event));
	}

	@Handler
	public void onWrite(Write<ManagedByteBuffer> event) {
		FileConnection connection = connections.get(event.getConnection());
		if (connection == null) {
			return;
		}
		connection.write(event);
	}
	
	@Handler
	public void onClose(Close event) throws InterruptedException {
		FileConnection connection = connections.get(event.getConnection());
		if (connection == null) {
			return;
		}
		connection.close(event);
	}

	@Handler
	public void onStop(Stop event) throws InterruptedException {
		while (connections.size() > 0) {
			FileConnection connection = connections.entrySet().iterator().next()
			        .getValue();
			connection.close(event);
		}
	}

	private class FileConnection {

		/**
		 * The write context needs to be finer grained than the general file
		 * connection context because an asynchronous write may be only
		 * partially successful, i.e. not all data provided by the write event
		 * may successfully be written in one asynchronous write invocation.
		 */
		private class WriteContext {
			public ManagedByteBuffer buffer;
			public long pos;

			public WriteContext(ManagedByteBuffer buffer, long pos) {
				this.buffer = buffer;
				this.pos = pos;
			}
		}

		private final Connection connection;
		private EventPipeline downPipeline;
		private Path path;
		private AsynchronousFileChannel ioChannel = null;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> ioBuffers;
		private long offset = 0;
		private CompletionHandler<Integer, ManagedByteBuffer> readCompletionHandler = new ReadCompletionHandler();
		private CompletionHandler<Integer, WriteContext> writeCompletionHandler = new WriteCompletionHandler();
		private int outstandingAsyncs = 0;
		private boolean reading = false;

		private FileConnection(OpenFile event) throws InterruptedException {
			connection = event.getConnection();
			downPipeline = newEventPipeline();
			path = event.getPath();
			try {
				ioChannel = AsynchronousFileChannel
				        .open(event.getPath(), event.getOptions());
			} catch (IOException e) {
				downPipeline.fire(new IOError(event, e));
			}
			offset = 0;
			if (Arrays.asList(event.getOptions())
			        .contains(StandardOpenOption.WRITE)) {
				// Writing to file
				reading = false;
				downPipeline.fire(new FileOpened(connection, event.getPath(),
				        event.getOptions()));
			} else {
				// Reading from file
				reading = true;
				ioBuffers = new ManagedBufferQueue<>(ManagedByteBuffer.class,
				        ByteBuffer.allocateDirect(bufferSize),
				        ByteBuffer.allocateDirect(bufferSize));
				ManagedByteBuffer buffer = ioBuffers.acquire();
				registerAsGenerator();
				downPipeline.fire(new FileOpened(connection, event.getPath(),
				        event.getOptions()));
				ioChannel.read(buffer.getBacking(), offset, buffer,
				        readCompletionHandler);
				synchronized (ioChannel) {
					outstandingAsyncs += 1;
				}
			}
		}

		private abstract class BaseCompletionHandler<C>
		        implements CompletionHandler<Integer, C> {

			@Override
			public void failed(Throwable exc, C context) {
				try {
					if (!(exc instanceof AsynchronousCloseException)) {
						downPipeline.fire(new IOError(null, exc));
					}
				} finally {
					handled();
				}
			}

			protected void handled() {
				synchronized (ioChannel) {
					if (--outstandingAsyncs == 0) {
						ioChannel.notifyAll();
					}
				}
			}
		}

		private class ReadCompletionHandler
		        extends BaseCompletionHandler<ManagedByteBuffer> {

			@Override
			public void completed(Integer result, ManagedByteBuffer buffer) {
				try {
					if (!connections.containsKey(connection)) {
						return;
					}
					if (result == -1) {
						downPipeline.fire(new Eof(connection));
						downPipeline.fire(new Close(connection));
						return;
					}
					buffer.flip();
					downPipeline.fire(new Read<>(connection, buffer));
					offset += result;
					try {
						ManagedByteBuffer nextBuffer = ioBuffers.acquire();
						nextBuffer.clear();
						ioChannel.read(nextBuffer.getBacking(), offset,
						        nextBuffer,
						        readCompletionHandler);
						synchronized (ioChannel) {
							outstandingAsyncs += 1;
						}
					} catch (InterruptedException e) {
					}
				} finally {
					handled();
				}
			}
		}

		private void write(Write<ManagedByteBuffer> event) {
			ManagedByteBuffer buffer = event.getBuffer();
			int written = buffer.remaining();
			if (written == 0) {
				return;
			}
			buffer.lockBuffer();
			synchronized (ioChannel) {
				ioChannel.write(buffer.getBacking(), offset,
				        new WriteContext(buffer, offset),
				        writeCompletionHandler);
				outstandingAsyncs += 1;
			}
			offset += written;
		}

		private class WriteCompletionHandler
		        extends BaseCompletionHandler<WriteContext> {

			@Override
			public void completed(Integer result, WriteContext context) {
				ManagedByteBuffer buffer = context.buffer;
				if (buffer.hasRemaining()) {
					ioChannel.write(buffer.getBacking(),
					        context.pos + buffer.position(),
					        context, writeCompletionHandler);
					return;
				}
				buffer.unlockBuffer();
				handled();
			}

		}

		public void close(Event<?> event) throws InterruptedException {
			try {
				synchronized (ioChannel) {
					while (outstandingAsyncs != 0) {
						ioChannel.wait();
					}
					ioChannel.close();
				}
				downPipeline.fire(new Closed(connection));
			} catch (ClosedChannelException e) {
			} catch (IOException e) {
				downPipeline.fire(new IOError(event, e));
			}
			if (reading) {
				unregisterAsGenerator();
			}
			connections.remove(connection);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("FileConnection [");
			if (connection != null) {
				builder.append("connection=");
				builder.append(connection);
				builder.append(", ");
			}
			if (path != null) {
				builder.append("path=");
				builder.append(path);
				builder.append(", ");
			}
			builder.append("offset=");
			builder.append(offset);
			builder.append("]");
			return builder.toString();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("File [");
		if (connections != null) {
			builder.append(connections.values());
		}
		builder.append("]");
		return builder.toString();
	}
}
