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

package org.jgrapes.http;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.ServerEngine;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.EndOfRequest;
import org.jgrapes.http.events.OptionsRequest;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.BufferCollector;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.net.TcpServer;
import org.jgrapes.net.events.Accepted;

/**
 * A converter component that receives and sends byte buffers on a 
 * network channel and sends HTTP requests and receives HTTP 
 * responses on its own channel.
 */
public class HttpServer extends Component {

	private List<Class<? extends Request>> providedFallbacks;
	private int matchLevels = 1;
	boolean acceptNoSni = false;

	/**
	 * Create a new server that uses the {@code networkChannel} for network
	 * level I/O.
	 * <P>
	 * As a convenience the server can provide fall back handlers for the
	 * specified types of requests. The fall back handler simply returns 404 (
	 * "Not found").
	 * 
	 * @param componentChannel
	 *            this component's channel
	 * @param networkChannel
	 *            the channel for network level I/O
	 * @param fallbacks
	 *            the requests for which a fall back handler is provided
	 */
	@SafeVarargs
	public HttpServer(Channel componentChannel, Channel networkChannel,
	        Class<? extends Request>... fallbacks) {
		super(componentChannel);
		this.providedFallbacks = Arrays.asList(fallbacks);
		Handler.Evaluator.add(
				this, "onAccepted", networkChannel.defaultCriterion());
		Handler.Evaluator.add(
				this, "onInput", networkChannel.defaultCriterion());
	}

	/**
	 * Create a new server that creates its own {@link TcpServer} with the given
	 * address and uses it for network level I/O.
	 * 
	 * @param componentChannel
	 *            this component's channel
	 * @param serverAddress the address to listen on
	 * @param fallbacks fall backs
	 */
	@SafeVarargs
	public HttpServer(Channel componentChannel, SocketAddress serverAddress,
	        Class<? extends Request>... fallbacks) {
		super(componentChannel);
		this.providedFallbacks = Arrays.asList(fallbacks);
		TcpServer server = new TcpServer().setServerAddress(serverAddress);
		attach(server);
		Handler.Evaluator.add(
				this, "onAccepted", server.defaultCriterion());
		Handler.Evaluator.add(
				this, "onInput", server.defaultCriterion());
	}

	/**
	 * @return the matchLevels
	 */
	public int matchLevels() {
		return matchLevels;
	}

	/**
	 * Sets the number of elements from the request path used in the match value
	 * of the generated events (see {@link Request#defaultCriterion()}), defaults
	 * to 1.
	 * 
	 * @param matchLevels the matchLevels to set
	 * @return the http server for easy chaining
	 */
	public HttpServer setMatchLevels(int matchLevels) {
		this.matchLevels = matchLevels;
		return this;
	}

	/**
	 * Determines if request from secure (TLS) connections without
	 * SNI are accepted.
	 *  
	 * Secure (TLS) requests usually transfer the name of the server that
	 * they want to connect to during handshake. The HTTP server checks
	 * that the `Host` header field of decoded requests matches the
	 * name used to establish the connection. If, however, the connection
	 * is made using the IP-address, the client does not have a host name.
	 * If such connections are to be accepted, this flag, which
	 * defaults to `false`, must be set.
	 * 
	 * Note that in request accepted without SNI, the `Host` header field
	 * will be modified to contain the IP-address of the indicated host
	 * to prevent accidental matching wit virtual host names.  
	 * 
	 * @param acceptNoSni the value to set
	 * @return the http server for easy chaining
	 */
	public HttpServer setAcceptNoSni(boolean acceptNoSni) {
		this.acceptNoSni = acceptNoSni;
		return this;
	}

	/**
	 * Returns if secure (TLS) requests without SNI are allowed.
	 * 
	 * @return the result
	 */
	public boolean acceptNoSni() {
		return acceptNoSni;
	}

	/**
	 * Creates a new downstream connection as {@link LinkedIOSubchannel} of the network
	 * connection, a {@link HttpRequestDecoder} and a
	 * {@link HttpResponseEncoder}.
	 * 
	 * @param event
	 *            the accepted event
	 */
	@Handler(dynamic=true)
	public void onAccepted(Accepted event, IOSubchannel channel) {
		new HttpConn(event, channel);
	}

	/**
	 * Handles data from the client (from upstream). The data is send through 
	 * the {@link HttpRequestDecoder} and events are sent downstream according
	 * to the decoding results.
	 * 
	 * @param event the event
	 * @throws ProtocolException if a protocol exception occurs
	 */
	@Handler(dynamic=true)
	public void onInput(Input<ManagedByteBuffer> event, IOSubchannel netChannel)
		throws ProtocolException {
		final HttpConn httpConn 
			= (HttpConn) LinkedIOSubchannel.lookupLinked(netChannel);
		if (httpConn == null 
				|| httpConn.converterComponent() != this) {
			return;
		}
		httpConn.handleInput(event);
	}

	/**
	 * Handles a response event from downstream by sending it through an
	 * {@link HttpResponseEncoder} that generates the data (encoded information)
	 * and sends it upstream with {@link Output} events. Depending on whether 
	 * the response has a body, subsequent {@link Output} events can
	 * follow.
	 * 
	 * @param event
	 *            the response event
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onResponse(Response event, HttpConn downChannel)
			throws InterruptedException {
		final IOSubchannel netChannel = downChannel.upstreamChannel();
		final ServerEngine<HttpRequest,HttpResponse> engine 
			= downChannel.engine;
		final HttpResponse response = event.response();

		// Start sending the response
		engine.encode(response);
		boolean hasBody = response.messageHasBody();
		while (true) {
			downChannel.outBuffer = netChannel.bufferPool().acquire();
			final ManagedByteBuffer buffer = downChannel.outBuffer;
			Codec.Result result = engine.encode(
					Codec.EMPTY_IN, buffer.backingBuffer(), !hasBody);
			if (result.isOverflow()) {
				fire(new Output<>(buffer, false), netChannel);
				continue;
			}
			if (hasBody) {
				// Keep buffer with incomplete response to be further
				// filled by Output events
				break;
			}
			// Response is complete
			if (buffer.position() > 0) {
				fire(new Output<>(buffer, false), netChannel);
			} else {
				buffer.unlockBuffer();
			}
			downChannel.outBuffer = null;
			if (result.closeConnection()) {
				fire(new Close(), netChannel);
			}
			break;
		}
	}

	/**
	 * Receives the message body of a response. A {@link Response} event that
	 * has a message body can be followed by one or more {@link Output} events
	 * from downstream that contain the data. An {@code Output} event
	 * with the end of record flag set signals the end of the message body.
	 * 
	 * @param event
	 *            the event with the data
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onOutput(Output<ManagedBuffer<?>> event, HttpConn downChannel)
	        throws InterruptedException {
		downChannel.sendUpstream(event);
	}

	/**
	 * Handles a close event from downstream by sending a {@link Close} 
	 * event upstream.
	 * 
	 * @param event
	 *            the close event
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onClose(Close event, HttpConn downChannel) 
			throws InterruptedException {
		final IOSubchannel netChannel = downChannel.upstreamChannel();
		netChannel.respond(new Close());
	}

	/**
	 * Checks whether the request has been handled (status code of response has
	 * been set). If not, send the default response ("Not implemented") to the
	 * client.
	 * 
	 * @param event
	 *            the request completed event
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onRequestCompleted(
			Request.Completed event, IOSubchannel channel)
	        throws InterruptedException {
		final Request requestEvent = event.event();
		if (requestEvent.isStopped()) {
			// Has been handled
			return;
		}
		final HttpResponse response 
			= requestEvent.request().response().get();

		if (response.statusCode() != HttpStatus.NOT_IMPLEMENTED.statusCode()) {
			// Some other component takes care
			return;
		}

		// No component has taken care of the request, provide
		// fallback response
		fireResponse(response, channel, HttpStatus.NOT_IMPLEMENTED.statusCode(), 
				HttpStatus.NOT_IMPLEMENTED.reasonPhrase());
	}

	/**
	 * Provides a fallback handler for an OPTIONS request with asterisk. Simply
	 * responds with "OK".
	 * 
	 * @param event the event
	 */
	@Handler(priority = Integer.MIN_VALUE)
	public void onOptions(OptionsRequest event, IOSubchannel channel) {
		if (event.requestUri() == HttpRequest.ASTERISK_REQUEST) {
			HttpResponse response = event.request().response().get();
			response.setStatus(HttpStatus.OK);
			channel.respond(new Response(response));
			event.stop();
		}
	}

	/**
	 * Provides a fall back handler (lowest priority) for the request types
	 * specified in the constructor.
	 * 
	 * @param event the event
	 * @throws ParseException if the request contains illegal header fields
	 */
	@Handler(priority = Integer.MIN_VALUE)
	public void onRequest(Request event, IOSubchannel channel)
			throws ParseException {
		if (providedFallbacks == null
		        || !providedFallbacks.contains(event.getClass())) {
			return;
		}
		fireResponse(event.request().response().get(), channel, 
				HttpStatus.NOT_FOUND.statusCode(), 
				HttpStatus.NOT_FOUND.reasonPhrase());
		event.stop();
	}

	private void fireResponse(HttpResponse response, IOSubchannel channel,
			int statusCode, String reasonPhrase) {
		response.setStatusCode(statusCode).setReasonPhrase(reasonPhrase)
			.setMessageHasBody(true).setField(
					HttpField.CONTENT_TYPE,
					MediaType.builder().setType("text", "plain")
					.setParameter("charset", "utf-8").build());
		fire(new Response(response), channel);
		try {
			fire(Output.wrap((statusCode + " " + reasonPhrase)
					.getBytes("utf-8"), true), channel);
		} catch (UnsupportedEncodingException e) {
			// Supported by definition
		}
	}
	
	private class HttpConn extends LinkedIOSubchannel {
		public ServerEngine<HttpRequest,HttpResponse> engine;
		public ManagedByteBuffer outBuffer;
		private boolean secure;
		private List<String> snis = Collections.emptyList();

		public HttpConn(Accepted event, IOSubchannel upstreamChannel) {
			super(HttpServer.this, upstreamChannel);
			engine = new ServerEngine<>(
					new HttpRequestDecoder(), new HttpResponseEncoder());
			secure = event.isSecure();
			if (secure) {
				snis = new ArrayList<>();
				for (SNIServerName sni: event.requestedServerNames()) {
					if (sni instanceof SNIHostName) {
						snis.add(((SNIHostName)sni).getAsciiName());
					}
				}
			}
		}
		
		public void handleInput(Input<ManagedByteBuffer> event) 
				throws ProtocolException {
			// Send the data from the event through the decoder.
			ByteBuffer in = event.buffer().backingBuffer();
			ManagedByteBuffer bodyData = null;
			while (in.hasRemaining()) {
				Decoder.Result<HttpResponse> result = engine.decode(in,
				        bodyData == null ? null : bodyData.backingBuffer(),
				        event.isEndOfRecord());
				if (result.response().isPresent()) {
					// Feedback required, send it
					respond(new Response(result.response().get()));
					if (result.response().get().isFinal()) {
						break;
					}
					if (result.isResponseOnly()) {
						continue;
					}
				}
				if (result.isHeaderCompleted()) {
					HttpRequest request = engine.currentRequest().get();
					if (secure) {
						if (!snis.contains(request.host())) {
							if (acceptNoSni && snis.isEmpty()) {
								convertHostToNumerical(request);
							} else {
								fireResponse(request.response().get(), 
										this, 421, "Misdirected Request");
								break;
							}
						}
					}
					fire(Request.fromHttpRequest(request,
							secure, matchLevels), this);
				}
				if (bodyData != null && bodyData.position() > 0) {
					bodyData.flip();
					fire(new Input<>(bodyData, !result.isOverflow() 
							&& !result.isUnderflow()), this);
				}
				if (result.isOverflow()) {
					bodyData = new ManagedByteBuffer(
					        ByteBuffer.allocate(in.capacity()),
					        BufferCollector.NOOP_COLLECTOR);
					continue;
				}
				if (!result.isUnderflow()
				        && engine.currentRequest().get().messageHasBody()) {
					fire(new EndOfRequest(), this);
				}
			}
		}

		private void convertHostToNumerical(HttpRequest request) {
			int port = request.port();
			String host;
			try {
				InetAddress addr = InetAddress.getByName(
						request.host());
				host = addr.getHostAddress();
				if (!(addr instanceof Inet4Address)) {
					host = "[" + host + "]";
				}
			} catch (UnknownHostException e) {
				host = "127.0.0.1";
			}
			request.setHostAndPort(host, port);
		}
		
		public void sendUpstream(Output<ManagedBuffer<?>> event) 
				throws InterruptedException {
			Buffer in = event.buffer().backingBuffer();
			if (!(in instanceof ByteBuffer)) {
				return;
			}
			ByteBuffer input = ((ByteBuffer)in).duplicate();
			if (outBuffer == null) {
				outBuffer = upstreamChannel().bufferPool().acquire();
			}
			while (input.hasRemaining()) {
				Codec.Result result = engine.encode(input,
				        outBuffer.backingBuffer(), event.isEndOfRecord());
				if (result.isOverflow()) {
					upstreamChannel().respond(new Output<>(outBuffer, false));
					outBuffer = upstreamChannel().bufferPool().acquire();
					continue;
				}
				if (event.isEndOfRecord() || result.closeConnection()) {
					if (outBuffer.position() > 0) {
						upstreamChannel().respond(new Output<>(outBuffer, false));
					} else {
						outBuffer.unlockBuffer();
					}
					outBuffer = null;
					if (result.closeConnection()) {
						upstreamChannel().respond(new Close());
					}
					break;
				}
			}
			
		}
	}

}
