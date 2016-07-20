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
package org.jdrupes.httpcodec.internal;

/**
 * @author Michael N. Lipp
 *
 */
public class Encoder {


	public void encode(Message message) {
	}

	public static class Result extends EncoderResult {

		public final static Result OVERFLOW = new Result(true, false, false);
		public final static Result UNDERFLOW = new Result(false, true, false);
		public final static Result CLOSE_CONNECTION = new Result(false, false,
		        true);
		public final static Result PROCEED = new Result(false, false, false);

		/**
		 * Creates a new result with the given values.
		 * 
		 * @param overflow
		 * @param underflow
		 * @param closeConnection
		 */
		protected Result(boolean overflow, boolean underflow, 
				boolean closeConnection) {
			super(overflow, underflow, closeConnection);
		}
		
	}
}
