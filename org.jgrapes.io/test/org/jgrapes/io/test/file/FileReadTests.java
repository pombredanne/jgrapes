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
package org.jgrapes.io.test.file;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.Component;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.Connection;
import org.jgrapes.io.File;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Eof;
import org.jgrapes.io.events.OpenFile;
import org.jgrapes.io.events.Opened;
import org.jgrapes.io.events.Read;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Michael N. Lipp
 *
 */
public class FileReadTests {

	public static int msgCount = 0;
	
	static long collected = 0;
	static StringBuilder collectedText = new StringBuilder();
	
	public static class Consumer extends Component {
		
		public Consumer() {
			super(Channel.SELF);
		}
		
		@Handler
		public void dataRead(Read<ManagedByteBuffer> event) 
				throws UnsupportedEncodingException {
			int length = event.getBuffer().limit();
			collected += length;
			byte[] bytes = new byte[length];
			event.getBuffer().get(bytes);
			collectedText.append(new String(bytes, "ascii"));
		}
		
	}

	public static class StateChecker extends Component {
		
		public enum State { NEW, CLOSED, OPENED, READING, EOF, CLOSING };
		public State state = State.NEW;

		public StateChecker() {
			super(Channel.BROADCAST);
		}
		
		@Handler
		public void opened(Opened event) {
			assertTrue(state == State.NEW);
			state = State.OPENED;
		}
		
		@Handler
		public void reading(Read<ManagedByteBuffer> event) {
			assertTrue(state == State.OPENED || state == State.READING );
			state = State.READING;
		}

		@Handler
		public void eof(Eof event) {
			assertTrue(state == State.READING);
			state = State.EOF;
		}

		@Handler
		public void closing(Close event) {
			assertTrue(state == State.EOF);
			state = State.CLOSING;
		}

		@Handler
		public void closed(Closed event) {
			assertTrue(state == State.CLOSING);
			state = State.CLOSED;
		}
	}
	
	@Test
	public void testRead() 
		throws URISyntaxException, InterruptedException, ExecutionException {
		Consumer consumer = new Consumer();
		Path filePath = Paths.get(getClass().getResource("test.txt").toURI());
		long fileSize = filePath.toFile().length();
		File app = new File(consumer, 512);
		app.attach(consumer);
		StateChecker sc = new StateChecker();
		app.attach(sc);
		Components.start(app);
		app.fire(new OpenFile(Connection.newConnection(consumer), filePath,
		        StandardOpenOption.READ), consumer).get();
		Components.awaitExhaustion();
		assertEquals(fileSize, collected);
		assertEquals(StateChecker.State.CLOSED, sc.state);
	}
	
}
