/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

package org.jgrapes.util.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jgrapes.core.Event;

/**
 * An event that triggers updates or deletions in a key/value store.
 */
public class KeyValueStoreUpdate extends Event<Void> {

	private List<Action> actions = new ArrayList<>();

	/**
	 * Adds a new update action to the event.
	 * 
	 * @param key the key
	 * @param value the value
	 * @return the event for easy chaining
	 */
	public KeyValueStoreUpdate update(String key, String value) {
		actions.add(new Update(key, value));
		return this;
	}

	/**
	 * Adds a new update action to the event that stores the given value
	 * on the path formed by the path segments.
	 * 
	 * @param value the value
	 * @param segments the path segments
	 * @return the event for easy chaining
	 */
	public KeyValueStoreUpdate storeAs(String value, String... segments) {
		actions.add(new Update("/" + String.join("/", segments), value));
		return this;
	}

	/**
	 * Adds a new deletion action to the event.
	 * 
	 * @param key the key
	 * @return the event for easy chaining
	 */
	public KeyValueStoreUpdate delete(String key) {
		actions.add(new Deletion(key));
		return this;
	}
	
	/**
	 * Adds a new deletion action that clears all keys with the given
	 * path prefix.
	 * 
	 * @param segments the path segments
	 * @return the event for easy chaining
	 */
	public KeyValueStoreUpdate clearAll(String... segments) {
		actions.add(new Deletion("/" + String.join("/", segments)));
		return this;
	}

	/**
	 * Returns the actions.
	 * 
	 * @return the actions
	 */
	public List<Action> actions() {
		return Collections.unmodifiableList(actions);
	}
	
	public abstract static class Action {
		public String key;

		public Action(String key) {
			this.key = key;
		}
		
		public String key() {
			return key;
		}
	}
	
	public static class Deletion extends Action {
		public Deletion(String key) {
			super(key);
		}
	}
	
	public static class Update extends Action {
		private String value;

		public Update(String key, String value) {
			super(key);
			this.value = value;
		}
		
		public String value() {
			return value;
		}
	}
}
