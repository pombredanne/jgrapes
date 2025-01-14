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

package org.jgrapes.http;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.net.HttpCookie;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.CacheControlDirectives;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.CookieList;
import org.jdrupes.httpcodec.types.Directive;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.internal.EventBase;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.DiscardSession;
import org.jgrapes.http.events.ProtocolSwitchAccepted;
import org.jgrapes.http.events.Request;
import org.jgrapes.io.IOSubchannel;

/**
 * A base class for session managers. A session manager associates 
 * {@link Request} events with a {@link Session} object using 
 * `Session.class` as association identifier. The {@link Request}
 * handler has a default priority of 1000.
 * 
 * Managers track requests using a cookie with a given name and path. The 
 * path is a prefix that has to be matched by the request, often "/".
 * If no cookie with the given name (see {@link #idName()}) is found,
 * a new cookie with that name and the specified path is created.
 * The cookie's value is the unique session id that is used to lookup
 * the session object.
 * 
 * Session managers provide additional support for web sockets. If a
 * web socket is accepted, the session associated with the request
 * is automatically associated with the {@link IOSubchannel} that
 * is subsequently used for the web socket events. This allows
 * handlers for web socket messages to access the session like
 * {@link Request} handlers.
 * 
 * @see EventBase#setAssociated(Object, Object)
 * @see "[OWASP Session Management Cheat Sheet](https://www.owasp.org/index.php/Session_Management_Cheat_Sheet)"
 */
@SuppressWarnings({ "PMD.DataClass", "PMD.AvoidPrintStackTrace" })
public abstract class SessionManager extends Component {

    private static SecureRandom secureRandom = new SecureRandom();

    private String idName = "id";
    private String path = "/";
    private long absoluteTimeout = 9 * 60 * 60 * 1000;
    private long idleTimeout = 30 * 60 * 1000;
    private int maxSessions = 1000;

    /**
     * Creates a new session manager with its channel set to
     * itself and the path set to "/". The manager handles
     * all {@link Request} events.
     */
    public SessionManager() {
        this("/");
    }

    /**
     * Creates a new session manager with its channel set to
     * itself and the path set to the given path. The manager
     * handles all requests that match the given path, using the
     * same rules as browsers do for selecting the cookies that
     * are to be sent.
     * 
     * @param path the path
     */
    public SessionManager(String path) {
        this(Channel.SELF, path);
    }

    /**
     * Creates a new session manager with its channel set to
     * the given channel and the path to "/". The manager handles
     * all {@link Request} events.
     * 
     * @param componentChannel the component channel
     */
    public SessionManager(Channel componentChannel) {
        this(componentChannel, "/");
    }

    /**
     * Creates a new session manager with the given channel and path.
     * The manager handles all requests that match the given path, using
     * the same rules as browsers do for selecting the cookies that
     * are to be sent.
     *  
     * @param componentChannel the component channel
     * @param path the path
     */
    public SessionManager(Channel componentChannel, String path) {
        this(componentChannel, derivePattern(path), 1000, path);
    }

    /**
     * Derives the resource pattern from the path.
     *
     * @param path the path
     * @return the pattern
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    protected static String derivePattern(String path) {
        String pattern;
        if ("/".equals(path)) {
            pattern = "/**";
        } else {
            String patternBase = path;
            if (patternBase.endsWith("/")) {
                patternBase = path.substring(0, path.length() - 1);
            }
            pattern = path + "," + path + "/**";
        }
        return pattern;
    }

    /**
     * Creates a new session manager using the given channel and path.
     * The manager handles only requests that match the given pattern.
     * The handler is registered with the given priority.
     * 
     * This constructor can be used if special handling of top level
     * requests is needed.
     *
     * @param componentChannel the component channel
     * @param pattern the path part of a {@link ResourcePattern}
     * @param priority the priority
     * @param path the path
     */
    public SessionManager(Channel componentChannel, String pattern,
            int priority, String path) {
        super(componentChannel);
        this.path = path;
        RequestHandler.Evaluator.add(this, "onRequest", pattern, priority);
        MBeanView.addManager(this);
    }

    /**
     * The name used for the session id cookie. Defaults to "`id`".
     * 
     * @return the id name
     */
    public String idName() {
        return idName;
    }

    /**
     * @param idName the id name to set
     * 
     * @return the session manager for easy chaining
     */
    public SessionManager setIdName(String idName) {
        this.idName = idName;
        return this;
    }

    /**
     * Set the maximum number of sessions. If the value is zero or less,
     * an unlimited number of sessions is supported. The default value
     * is 1000.
     * 
     * If adding a new session would exceed the limit, first all
     * sessions older than {@link #absoluteTimeout()} are removed.
     * If this doesn't free a slot, the least recently used session
     * is removed.
     * 
     * @param maxSessions the maxSessions to set
     * @return the session manager for easy chaining
     */
    public SessionManager setMaxSessions(int maxSessions) {
        this.maxSessions = maxSessions;
        return this;
    }

    /**
     * @return the maxSessions
     */
    public int maxSessions() {
        return maxSessions;
    }

    /**
     * Sets the absolute timeout for a session in seconds. The absolute
     * timeout is the time after which a session is invalidated (relative
     * to its creation time). Defaults to 9 hours. Zero or less disables
     * the timeout.
     * 
     * @param absoluteTimeout the absolute timeout
     * @return the session manager for easy chaining
     */
    public SessionManager setAbsoluteTimeout(int absoluteTimeout) {
        this.absoluteTimeout = absoluteTimeout * 1000;
        return this;
    }

    /**
     * @return the absolute session timeout (in seconds)
     */
    public int absoluteTimeout() {
        return (int) (absoluteTimeout / 1000);
    }

    /**
     * Sets the idle timeout for a session in seconds. Defaults to 30 minutes.
     * Zero or less disables the timeout. 
     * 
     * @param idleTimeout the absolute timeout
     * @return the session manager for easy chaining
     */
    public SessionManager setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout * 1000;
        return this;
    }

    /**
     * @return the idle timeout (in seconds)
     */
    public int idleTimeout() {
        return (int) (idleTimeout / 1000);
    }

    /**
     * Associates the event with a {@link Session} object
     * using `Session.class` as association identifier.
     * 
     * @param event the event
     */
    @RequestHandler(dynamic = true)
    public void onRequest(Request.In event) {
        if (event.associated(Session.class).isPresent()) {
            return;
        }
        final HttpRequest request = event.httpRequest();
        Optional<String> requestedSessionId = request.findValue(
            HttpField.COOKIE, Converters.COOKIE_LIST)
            .flatMap(cookies -> cookies.stream().filter(
                cookie -> cookie.getName().equals(idName()))
                .findFirst().map(HttpCookie::getValue));
        if (requestedSessionId.isPresent()) {
            String sessionId = requestedSessionId.get();
            synchronized (this) {
                Optional<Session> session = lookupSession(sessionId);
                if (session.isPresent()) {
                    Instant now = Instant.now();
                    if ((absoluteTimeout <= 0
                        || Duration.between(session.get().createdAt(),
                            now).toMillis() < absoluteTimeout)
                        && (idleTimeout <= 0
                            || Duration.between(session.get().lastUsedAt(),
                                now).toMillis() < idleTimeout)) {
                        event.setAssociated(Session.class, session.get());
                        session.get().updateLastUsedAt();
                        return;
                    }
                    // Invalidate, too old
                    removeSession(sessionId);
                }
            }
        }
        String sessionId = createSessionId(request.response().get());
        Session session = createSession(sessionId);
        event.setAssociated(Session.class, session);
    }

    /**
     * Creates a new session with the given id.
     * 
     * @param sessionId
     * @return the session
     */
    protected abstract Session createSession(String sessionId);

    /**
     * Lookup the session with the given id.
     * 
     * @param sessionId
     * @return the session
     */
    protected abstract Optional<Session> lookupSession(String sessionId);

    /**
     * Removed the given session.
     * 
     * @param sessionId the session id
     */
    protected abstract void removeSession(String sessionId);

    /**
     * Return the number of established sessions.
     * 
     * @return the result
     */
    protected abstract int sessionCount();

    /**
     * Creates a session id and adds the corresponding cookie to the
     * response.
     * 
     * @param response the response
     * @return the session id
     */
    protected String createSessionId(HttpResponse response) {
        StringBuilder sessionIdBuilder = new StringBuilder();
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        for (byte b : bytes) {
            sessionIdBuilder.append(Integer.toHexString(b & 0xff));
        }
        String sessionId = sessionIdBuilder.toString();
        HttpCookie sessionCookie = new HttpCookie(idName(), sessionId);
        sessionCookie.setPath(path);
        sessionCookie.setHttpOnly(true);
        response.computeIfAbsent(HttpField.SET_COOKIE, CookieList::new)
            .value().add(sessionCookie);
        response.computeIfAbsent(
            HttpField.CACHE_CONTROL, CacheControlDirectives::new)
            .value().add(new Directive("no-cache", "SetCookie, Set-Cookie2"));
        return sessionId;
    }

    /**
     * Discards the given session.
     * 
     * @param event the event
     */
    @Handler(channels = Channel.class)
    public void discard(DiscardSession event) {
        removeSession(event.session().id());
    }

    /**
     * Associates the channel with the session from the upgrade request.
     * 
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = 1000)
    public void onProtocolSwitchAccepted(
            ProtocolSwitchAccepted event, IOSubchannel channel) {
        event.requestEvent().associated(Session.class)
            .ifPresent(session -> {
                channel.setAssociated(Session.class, session);
            });
    }

    /**
     * An MBean interface for getting information about the 
     * established sessions.
     */
    @SuppressWarnings("PMD.CommentRequired")
    public interface SessionManagerMXBean {

        String getComponentPath();

        String getPath();

        int getMaxSessions();

        int getAbsoluteTimeout();

        int getIdleTimeout();

        int getSessionCount();
    }

    /**
     * The session manager information.
     */
    public static class SessionManagerInfo implements SessionManagerMXBean {

        private static MBeanServer mbs
            = ManagementFactory.getPlatformMBeanServer();

        private ObjectName mbeanName;
        private final WeakReference<SessionManager> sessionManagerRef;

        /**
         * Instantiates a new session manager info.
         *
         * @param sessionManager the session manager
         */
        @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
            "PMD.EmptyCatchBlock" })
        public SessionManagerInfo(SessionManager sessionManager) {
            try {
                mbeanName = new ObjectName("org.jgrapes.http:type="
                    + SessionManager.class.getSimpleName() + ",name="
                    + ObjectName.quote(Components.simpleObjectName(
                        sessionManager)));
            } catch (MalformedObjectNameException e) {
                // Won't happen
            }
            sessionManagerRef = new WeakReference<>(sessionManager);
            try {
                mbs.unregisterMBean(mbeanName);
            } catch (Exception e) {
                // Just in case, should not work
            }
            try {
                mbs.registerMBean(this, mbeanName);
            } catch (InstanceAlreadyExistsException | MBeanRegistrationException
                    | NotCompliantMBeanException e) {
                // Have to live with that
            }
        }

        /**
         * Returns the session manager.
         *
         * @return the optional session manager
         */
        @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
            "PMD.EmptyCatchBlock" })
        public Optional<SessionManager> manager() {
            SessionManager manager = sessionManagerRef.get();
            if (manager == null) {
                try {
                    mbs.unregisterMBean(mbeanName);
                } catch (MBeanRegistrationException
                        | InstanceNotFoundException e) {
                    // Should work.
                }
            }
            return Optional.ofNullable(manager);
        }

        @Override
        public String getComponentPath() {
            return manager().map(mgr -> mgr.componentPath())
                .orElse("<removed>");
        }

        @Override
        public String getPath() {
            return manager().map(mgr -> mgr.path).orElse("<unknown>");
        }

        @Override
        public int getMaxSessions() {
            return manager().map(mgr -> mgr.maxSessions()).orElse(0);
        }

        @Override
        public int getAbsoluteTimeout() {
            return manager().map(mgr -> mgr.absoluteTimeout()).orElse(0);
        }

        @Override
        public int getIdleTimeout() {
            return manager().map(mgr -> mgr.idleTimeout()).orElse(0);
        }

        @Override
        public int getSessionCount() {
            return manager().map(mgr -> mgr.sessionCount()).orElse(0);
        }
    }

    /**
     * An MBean interface for getting information about all session
     * managers.
     * 
     * There is currently no summary information. However, the (periodic)
     * invocation of {@link SessionManagerSummaryMXBean#getManagers()} ensures
     * that entries for removed {@link SessionManager}s are unregistered.
     */
    public interface SessionManagerSummaryMXBean {

        /**
         * Gets the managers.
         *
         * @return the managers
         */
        Set<SessionManagerMXBean> getManagers();
    }

    /**
     * The MBean view.
     */
    private static class MBeanView implements SessionManagerSummaryMXBean {
        private static Set<SessionManagerInfo> managerInfos = new HashSet<>();

        /**
         * Adds a manager.
         *
         * @param manager the manager
         */
        public static void addManager(SessionManager manager) {
            synchronized (managerInfos) {
                managerInfos.add(new SessionManagerInfo(manager));
            }
        }

        @Override
        public Set<SessionManagerMXBean> getManagers() {
            Set<SessionManagerInfo> expired = new HashSet<>();
            synchronized (managerInfos) {
                for (SessionManagerInfo managerInfo : managerInfos) {
                    if (!managerInfo.manager().isPresent()) {
                        expired.add(managerInfo);
                    }
                }
                managerInfos.removeAll(expired);
            }
            @SuppressWarnings("unchecked")
            Set<SessionManagerMXBean> result
                = (Set<SessionManagerMXBean>) (Object) managerInfos;
            return result;
        }
    }

    static {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName mxbeanName = new ObjectName("org.jgrapes.http:type="
                + SessionManager.class.getSimpleName() + "s");
            mbs.registerMBean(new MBeanView(), mxbeanName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException
                | MBeanRegistrationException | NotCompliantMBeanException e) {
            // Does not happen
            e.printStackTrace();
        }
    }
}
