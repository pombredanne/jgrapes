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
package org.jgrapes.core;

import java.util.Arrays;

import org.jgrapes.core.events.HandlingError;
import org.jgrapes.core.internal.EventBase;
import org.jgrapes.core.internal.Matchable;

/**
 * The base class for all events. Event classes form a hierarchy.
 * By default (i.e. as implemented by this class), the event's class 
 * (type) is used for matching. A handler is invoked if the class of the
 * event handled by it is equal to or a base class of the class of the event 
 * to be handled. 
 * <P>
 * This default behavior can be changed by overriding the methods
 * from {@link Matchable}. See {@link NamedEvent} as an example.
 * 
 * @author Michael N. Lipp
 */
public class Event extends EventBase {

	/* (non-Javadoc)
	 * @see org.jdrupes.core.internal.Matchable#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return getClass();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.Matchable#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(Object handlerKey) {
		return Class.class.isInstance(handlerKey)
				&& ((Class<?>)handlerKey).isAssignableFrom(getClass());
	}

	/**
	 * Implements the default behavior for handling events thrown
	 * by a handler. Fires a {@link HandlingError handling error} event
	 * for this event and the given throwable.
	 * 
	 * @see HandlingError
	 */
	@Override
	protected void handlingError
		(EventPipeline eventProcessor, Throwable throwable) {
		eventProcessor.add(new HandlingError(this, throwable), getChannels());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getMatchKey().hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Event other = (Event) obj;
		if (getMatchKey() == null) {
			if (other.getMatchKey() != null)
				return false;
		} else if (!getMatchKey().equals(other.getMatchKey()))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName() + " [");
		result.append("matchKey="
				+ ((getMatchKey() instanceof Class)
						?  ((Class<?>)getMatchKey()).getSimpleName() + ".class"
						: getMatchKey()));
		if (getChannels() != null) {
			result.append(", " + "channels=" + Arrays.toString(getChannels())); 
		}
		result.append("]");
		return result.toString();
	}	
}
