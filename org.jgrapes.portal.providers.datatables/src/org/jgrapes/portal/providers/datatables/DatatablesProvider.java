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

package org.jgrapes.portal.providers.datatables;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.portal.PageResourceProvider;
import org.jgrapes.portal.PortalSession;
import org.jgrapes.portal.PortalView;
import org.jgrapes.portal.events.AddPageResources;
import org.jgrapes.portal.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.events.PortalReady;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;

//// Localize data tables
//
//// Add item
//$.fn.dataTable.defaults.oLanguage._hungarianMap["lengthAll"] = "sLengthAll";
//// Fallback
//$.extend( $.fn.dataTable.defaults.oLanguage, {
//    "sLengthAll":      'all',
//} );
//// From l10n
//$.extend( $.fn.dataTable.defaults.oLanguage, ${_("DataTablesL10n")?no_esc} );
//


/**
 * 
 */
public class DatatablesProvider extends PageResourceProvider {

	/**
	 * Creates a new component with its channel set to the given 
	 * channel.
	 * 
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 */
	public DatatablesProvider(Channel componentChannel) {
		super(componentChannel);
	}

	@Handler(priority=100)
	public void onPortalReady(PortalReady event, PortalSession portalSession) 
			throws TemplateNotFoundException, MalformedTemplateNameException, 
			ParseException, IOException {
		portalSession.respond(new AddPageResources()
				.addScriptResource(new ScriptResource()
						.setProvides(new String[] {"datatables.net"})
						.setScriptUri(event.renderSupport().pageResource(
								"datatables/datatables.min.js")))
				.addScriptResource(new ScriptResource()
						.setRequires(new String[] {"datatables.net"})
						.setScriptUri(event.renderSupport().pageResource(
								PortalView.uriFromPath(										
										"datatables/processing().js")))));
	}

}
