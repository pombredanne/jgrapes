/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.io.events;

import org.jgrapes.core.Event;

/**
 * Fired by an initiator of connections that maintains a pool of 
 * such connections if no more connections are available.
 * 
 * Receiving (downstream) connections should check if the connection
 * can be closed. If this is the case, they should respond with
 * a {@link Close} event. If the decision cannot be made by the
 * component, it must forward the event downstream.
 */
public class Purge extends Event<Void> {

}
