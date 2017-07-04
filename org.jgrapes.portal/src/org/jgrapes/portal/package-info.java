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

/**
 * The portal package provides a portal implementation for the
 * JGrapes framework. The {@link org.jgrapes.portal.Portal} component 
 * is conceptually the main component. It exchanges events 
 * with the portlets, usually using a channel that is independent
 * of the  channel used for HTTP Input/Output.
 *
 * When created, a {@link org.jgrapes.portal.Portal} component automatically 
 * instantiates a child component of type {@link org.jgrapes.portal.PortalView}
 * which handles the HTTP side of the portal. You can think of the 
 * {@link org.jgrapes.portal.PortalView}/{@link org.jgrapes.portal.Portal}
 * pair as a gateway that translates the Input/Output related events on the 
 * HTTP side to portal/portlet related events on the portlet side and 
 * vice versa.
 * 
 * The portal is implemented as a single page application. There is only
 * one initial HTML document that provides the basic structure of the portal.
 * Aside from requests for static resources like JavaScript libraries, CSS,
 * images etc. all information is then exchanged using a web socket connection
 * that is established immediately after the initial HTML has been loaded.
 * 
 * The following diagram shows the details for the portal bootstrap.
 * 
 * ![Event Sequence](PortalBootSeq.svg)
 * 
 * After the portal page has loaded and the web socket connection has been
 * established, all information is exchanged using 
 * [JSON RPC notifications](http://www.jsonrpc.org/specification#notification). 
 * The {@link org.jgrapes.portal.PortalView} processes 
 * {@link org.jgrapes.io.events.Input} events with a serialized JSON RPC data
 * from the web socket until the complete JSON RPCnotification has been 
 * received. The notification (a {@link org.jgrapes.portal.events.JsonRequest}
 * from the servers point of view) is then fired on the on HTTP side (due 
 * to its similarity to a HTTP request), which allows it to be intercepted
 * by additional components. Usually, however, it is handled by the 
 * {@link org.jgrapes.portal.PortalView} that either processes it itself 
 * (if it affects the complete portal, e.g. a change of language or theme) or
 * converts it to an  event that is fired on the portal 
 * channel (in case the notification is directed at the portlets).
 * 
 * Portlets trigger actions on the browser by firing events on the portal 
 * channel. The events are forward to the {@link org.jgrapes.portal.PortalView}
 * that converts them to JSON RPCs that are serialized and sent on the web 
 * socket (as {@link org.jgrapes.io.events.Output} events). 
 * 
 * Details about the handling of the different events can be found in their 
 * respective JavaDoc. Note that the documentation of the events uses a 
 * slightly simplified version of the sequence diagram that combines the 
 * {@link org.jgrapes.portal.PortalView} and the 
 * {@link org.jgrapes.portal.Portal} into a single object.
 * 
 * @startuml PortalBootSeq.svg
 * hide footbox
 * 
 * Browser -> PortalView: "GET <portal URL>"
 * activate PortalView
 * PortalView -> Browser: "HTML document"
 * deactivate PortalView
 * activate Browser
 * Browser -> PortalView: "GET <resource1 URL>"
 * activate PortalView
 * PortalView -> Browser: Resource
 * deactivate PortalView
 * Browser -> PortalView: "GET <resource2 URL>"
 * activate PortalView
 * PortalView -> Browser: Resource
 * deactivate PortalView
 * Browser -> PortalView: "GET <Upgrade to WebSocket>"
 * activate PortalView
 * Browser -> PortalView: JSON RPC (Input)
 * activate PortalView
 * Browser -> PortalView: JSON RPC (Input)
 * deactivate Browser
 * PortalView -> PortalView: JsonRequest("portalReady")
 * deactivate PortalView
 * PortalView -> Portal: fire(PortalReady)
 * activate Portal
 * Portal -> Portlet: PortalReady
 * deactivate Portal
 * activate Portlet
 * Portlet -> Portal: AddPortletType
 * deactivate Portlet
 * activate Portal
 * Portal -> PortalView: toJsonRpc(AddPortletType)
 * deactivate Portal
 * activate PortalView
 * PortalView -> Browser: JSON RPC (Output)
 * deactivate PortalView
 * activate Browser
 * deactivate Browser
 * 
 * @enduml
 */
@org.osgi.annotation.versioning.Version("${api_version}")
package org.jgrapes.portal;