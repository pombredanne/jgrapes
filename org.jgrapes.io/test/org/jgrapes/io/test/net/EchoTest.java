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
package org.jgrapes.io.test.net;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Utils;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.Server;
import org.jgrapes.io.events.Read;
import org.jgrapes.io.events.Ready;
import org.jgrapes.io.events.Write;
import org.jgrapes.io.test.WaitFor;
import org.junit.Test;

public class EchoTest {

	public class EchoServer extends AbstractComponent {

		/**
		 * @throws IOException 
		 */
		public EchoServer() throws IOException {
			super(Server.DEFAULT_CHANNEL);
			attach(new Server(null));
		}

		@Handler
		public void onRead(Read<ByteBuffer> event) throws InterruptedException {
			ByteBuffer out = event.getConnection().acquireWriteBuffer();
			out.put(event.getBuffer());
			fire(new Write<>(event.getConnection(), out));
		}
	}

	@Test
	public void test() throws IOException, InterruptedException, 
			ExecutionException {
		EchoServer app = new EchoServer();
		app.attach(new NioDispatcher());
		WaitFor wf = new WaitFor
				(app, Ready.class, Server.DEFAULT_CHANNEL.getMatchKey());
		Utils.start(app);
		Ready readyEvent = (Ready) wf.get();
		if (!(readyEvent.getListenAddress() instanceof InetSocketAddress)) {
			fail();
		}
		InetSocketAddress serverAddr 
			= ((InetSocketAddress)readyEvent.getListenAddress());
		try (Socket client = new Socket(serverAddr.getAddress(),
		        serverAddr.getPort())) {
			Thread sender = new Thread() {
				@Override
				public void run() {
					try {
						OutputStream toServer = client.getOutputStream();
						for (int i = 0; i < 16; i++) {
							String line = i + ":Hello World!\n";
							toServer.write(line.getBytes("ascii"));
							toServer.flush();
							try {
								// If we're too fast, data will be appended
								// tp previous, not yet flushed out chunk
								Thread.sleep(5);
							} catch (InterruptedException e) {
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			sender.start();

			InputStream fromServer = client.getInputStream();
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(fromServer, "ascii"));
			int expected = 0;
			while (expected < 16) {
				String line = in.readLine();
				String[] parts = line.split(":");
				assertEquals(expected, Integer.parseInt(parts[0]));
				assertEquals("Hello World!", parts[1]);
				expected += 1;
			}
		}
	
		Utils.manager(app).fire(new Stop(), Channel.BROADCAST);
		Utils.awaitExhaustion();
	}

}
