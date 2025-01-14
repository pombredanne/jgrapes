/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.http.events;

import java.util.Optional;

import org.jdrupes.httpcodec.protocols.websocket.WsCloseFrame;
import org.jgrapes.core.Channel;

/**
 * An event that provides the close information when a WebSockt is closed.
 * Note that this is fired in addition to the connection close event.
 */
public class WebSocketClose extends MessageReceived<Void> {

    private final Optional<Integer> statusCode;
    private final Optional<String> reason;

    /**
     * @param closeFrame the close frame
     * @param channels
     */
    public WebSocketClose(WsCloseFrame closeFrame, Channel... channels) {
        super(channels);
        statusCode = closeFrame.statusCode();
        reason = closeFrame.reason();
    }

    /**
     * @return the statusCode
     */
    public Optional<Integer> statusCode() {
        return statusCode;
    }

    /**
     * @return the reason
     */
    public Optional<String> reason() {
        return reason;
    }

}
