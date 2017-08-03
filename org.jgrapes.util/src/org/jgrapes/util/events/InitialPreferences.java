/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

import org.jgrapes.util.PreferencesStore;

/**
 * A special {@link UpdatePreferences} event that is used for
 * reporting the properties on startup. Components that distinguish
 * the initial value propagation from real changes simply handle
 * the {@link UpdatePreferences} events. 
 */
public class InitialPreferences extends UpdatePreferences {

	private String applicationPath;
	
	/**
	 * Create a new event. The path to the application's preferences
	 * (i.e. the base path without the trailing "`PreferencesStore`",
	 * see {@link PreferencesStore}) is passed as additional information.
	 */
	public InitialPreferences(String applicationPath) {
		this.applicationPath = applicationPath;
	}

	/**
	 * The absolute path of the application's preferences.
	 * 
	 * @return path 
	 */
	public String applicationPath() {
		return applicationPath;
	}
}
