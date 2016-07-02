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
package org.jgrapes.io.util;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.stream.IntStream;

/**
 * @author Michael N. Lipp
 *
 */
public class ManagedCharBuffer extends ManagedBuffer<CharBuffer> {

	public ManagedCharBuffer(CharBuffer buffer, BufferCollector manager) {
		super(buffer, manager);
	}

	/**
	 * @param c
	 * @return
	 * @see java.nio.CharBuffer#append(char)
	 */
	public CharBuffer append(char c) {
		return buffer.append(c);
	}

	/**
	 * @param csq
	 * @param start
	 * @param end
	 * @return
	 * @see java.nio.CharBuffer#append(java.lang.CharSequence, int, int)
	 */
	public CharBuffer append(CharSequence csq, int start, int end) {
		return buffer.append(csq, start, end);
	}

	/**
	 * @param csq
	 * @return
	 * @see java.nio.CharBuffer#append(java.lang.CharSequence)
	 */
	public CharBuffer append(CharSequence csq) {
		return buffer.append(csq);
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#array()
	 */
	@Override
	public final char[] array() {
		return buffer.array();
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#arrayOffset()
	 */
	public final int arrayOffset() {
		return buffer.arrayOffset();
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#asReadOnlyBuffer()
	 */
	public CharBuffer asReadOnlyBuffer() {
		return buffer.asReadOnlyBuffer();
	}

	/**
	 * @param index
	 * @return
	 * @see java.nio.CharBuffer#charAt(int)
	 */
	public final char charAt(int index) {
		return buffer.charAt(index);
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#chars()
	 */
	public IntStream chars() {
		return buffer.chars();
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#compact()
	 */
	public CharBuffer compact() {
		return buffer.compact();
	}

	/**
	 * @param that
	 * @return
	 * @see java.nio.CharBuffer#compareTo(java.nio.CharBuffer)
	 */
	public int compareTo(CharBuffer that) {
		return buffer.compareTo(that);
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#duplicate()
	 */
	public CharBuffer duplicate() {
		return buffer.duplicate();
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#get()
	 */
	public char get() {
		return buffer.get();
	}

	/**
	 * @param dst
	 * @param offset
	 * @param length
	 * @return
	 * @see java.nio.CharBuffer#get(char[], int, int)
	 */
	public CharBuffer get(char[] dst, int offset, int length) {
		return buffer.get(dst, offset, length);
	}

	/**
	 * @param dst
	 * @return
	 * @see java.nio.CharBuffer#get(char[])
	 */
	public CharBuffer get(char[] dst) {
		return buffer.get(dst);
	}

	/**
	 * @param index
	 * @return
	 * @see java.nio.CharBuffer#get(int)
	 */
	public char get(int index) {
		return buffer.get(index);
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#hasArray()
	 */
	public final boolean hasArray() {
		return buffer.hasArray();
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#isDirect()
	 */
	public boolean isDirect() {
		return buffer.isDirect();
	}

	/**
	 * @return
	 * @see java.nio.Buffer#isReadOnly()
	 */
	public boolean isReadOnly() {
		return buffer.isReadOnly();
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#length()
	 */
	public final int length() {
		return buffer.length();
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#order()
	 */
	public ByteOrder order() {
		return buffer.order();
	}

	/**
	 * @param c
	 * @return
	 * @see java.nio.CharBuffer#put(char)
	 */
	public CharBuffer put(char c) {
		return buffer.put(c);
	}

	/**
	 * @param src
	 * @param offset
	 * @param length
	 * @return
	 * @see java.nio.CharBuffer#put(char[], int, int)
	 */
	public CharBuffer put(char[] src, int offset, int length) {
		return buffer.put(src, offset, length);
	}

	/**
	 * @param src
	 * @return
	 * @see java.nio.CharBuffer#put(char[])
	 */
	public final CharBuffer put(char[] src) {
		return buffer.put(src);
	}

	/**
	 * @param src
	 * @return
	 * @see java.nio.CharBuffer#put(java.nio.CharBuffer)
	 */
	public CharBuffer put(CharBuffer src) {
		return buffer.put(src);
	}

	/**
	 * @param src
	 * @return
	 * @see java.nio.CharBuffer#put(java.nio.CharBuffer)
	 */
	public CharBuffer put(ManagedCharBuffer src) {
		return buffer.put(src.getBuffer());
	}

	/**
	 * @param index
	 * @param c
	 * @return
	 * @see java.nio.CharBuffer#put(int, char)
	 */
	public CharBuffer put(int index, char c) {
		return buffer.put(index, c);
	}

	/**
	 * @param src
	 * @param start
	 * @param end
	 * @return
	 * @see java.nio.CharBuffer#put(java.lang.String, int, int)
	 */
	public CharBuffer put(String src, int start, int end) {
		return buffer.put(src, start, end);
	}

	/**
	 * @param src
	 * @return
	 * @see java.nio.CharBuffer#put(java.lang.String)
	 */
	public final CharBuffer put(String src) {
		return buffer.put(src);
	}

	/**
	 * @param target
	 * @return
	 * @throws IOException
	 * @see java.nio.CharBuffer#read(java.nio.CharBuffer)
	 */
	public int read(CharBuffer target) throws IOException {
		return buffer.read(target);
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#slice()
	 */
	public CharBuffer slice() {
		return buffer.slice();
	}

	/**
	 * @param start
	 * @param end
	 * @return
	 * @see java.nio.CharBuffer#subSequence(int, int)
	 */
	public CharBuffer subSequence(int start, int end) {
		return buffer.subSequence(start, end);
	}

	/**
	 * @return
	 * @see java.nio.CharBuffer#toString()
	 */
	public String toString() {
		return buffer.toString();
	}
	
}
