/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.jdrupes;

import org.jdrupes.internal.Matchable;

/**
 * Represents a communication bus for sending events between components.
 * <P>
 * For ordinary usage, the implementing classes {@link ClassChannel}
 * and {@link NamedChannel} should be sufficient. If another type of
 * <code>Channel</code> is needed, its implementation of this interface 
 * must make sure that {@link Matchable#matches(Object)} returns
 * <code>true</code> if called with <code>Channel.class</code>
 * as parameter, else channels of the new type will not participate
 * in broadcasts.
 * <P>
 * Objects of type <code>Channel</code> must be immutable.
 * 
 * @author mnl
 * @see Channel#BROADCAST
 */
public interface Channel extends Matchable {

	/**
	 * A special channel that can be used to send events to
	 * all components.
	 */
	public static final Channel BROADCAST = new ClassChannel() {

		/**
		 * @return <code>Channel.class</code>
		 * 
		 * @see org.jdrupes.ClassChannel#getMatchKey()
		 */
		@Override
		public Object getMatchKey() {
			return Channel.class;
		}
		
	};

}
