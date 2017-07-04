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

package org.jgrapes.portal.events;

import java.util.Set;
import org.jgrapes.core.Event;

import static org.jgrapes.portal.Portlet.*;

/**
 * Send to the portal view for adding or updating a complete portlet view.
 */
public abstract class RenderPortlet extends Event<Void> {

	private String portletId;
	private RenderMode renderMode;
	private Set<RenderMode> supportedModes;

	/**
	 * Creates a new event.
	 * 
	 * @param portletId the id of the portlet
	 * @param mode the view mode that is to be updated
	 * @param supportedModes the modes supported by the portlet
	 */
	public RenderPortlet(String portletId, RenderMode mode,
			Set<RenderMode> supportedModes) {
		super();
		this.portletId = portletId;
		this.renderMode = mode;
		this.supportedModes = supportedModes;
	}

	/**
	 * Returns the portlet id
	 * 
	 * @return the portlet id
	 */
	public String portletId() {
		return portletId;
	}

	/**
	 * Returns the render mode.
	 * 
	 * @return the render mode
	 */
	public RenderMode renderMode() {
		return renderMode;
	}

	/**
	 * Returns the supported modes.
	 * 
	 * @return the supported modes
	 */
	public Set<RenderMode> supportedRenderModes() {
		return supportedModes;
	}
}
