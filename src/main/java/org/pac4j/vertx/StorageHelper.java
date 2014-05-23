package org.pac4j.vertx;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.ServerCookieEncoder;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.profile.CommonProfile;
import org.vertx.java.core.http.HttpServerRequest;

public final class StorageHelper {

    private static ConcurrentMap<String, Object> map = new ConcurrentHashMap<>();

    /**
     * Get a session identifier and generates it if no session exists.
     * 
     * @param session
     * @return the session identifier
     */
    public static String getOrCreateSessionId(final HttpServerRequest req) {
        String sessionId = req.params().get(Constants.SESSION_ID);
        if (sessionId != null) {
            return sessionId;
        }
        String value = req.headers().get("Cookie");
        if (value != null) {
            Set<Cookie> cookies = CookieDecoder.decode(value);
            for (final Cookie cookie : cookies) {
                if (Constants.VERTX_SESSION_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        sessionId = generateSessionId();
        req.params().add(Constants.SESSION_ID, sessionId);
        Cookie cookie = new DefaultCookie(Constants.VERTX_SESSION_COOKIE, sessionId);
        cookie.setPath("/");
        req.response().putHeader("Set-Cookie", ServerCookieEncoder.encode(cookie));
        return sessionId;
    }

    /**
     * Generate a session identifier.
     * 
     * @return a session identifier
     */
    public static String generateSessionId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Get the profile from storage.
     * 
     * @param sessionId
     * @return the user profile
     */
    public static CommonProfile getProfile(final String sessionId) {
        if (sessionId != null) {
            return (CommonProfile) get(sessionId);
        }
        return null;
    }

    /**
     * Save a user profile in storage.
     * 
     * @param sessionId
     * @param profile
     */
    public static void saveProfile(final String sessionId, final CommonProfile profile) {
        if (sessionId != null) {
            save(sessionId, profile, Config.getProfileTimeout());
        }
    }

    /**
     * Remove a user profile from storage.
     * 
     * @param sessionId
     */
    public static void removeProfile(final String sessionId) {
        if (sessionId != null) {
            remove(sessionId);
        }
    }

    /**
     * Get a requested url from storage.
     * 
     * @param sessionId
     * @param clientName
     * @return the requested url
     */
    public static String getRequestedUrl(final String sessionId) {
        return (String) get(sessionId, Constants.REQUESTED_URL);
    }

    /**
     * Save a requested url to storage.
     * 
     * @param sessionId
     * @param clientName
     * @param requestedUrl
     */
    public static void saveRequestedUrl(final String sessionId, final String requestedUrl) {
        save(sessionId, Constants.REQUESTED_URL, requestedUrl);
    }

    /**
     * Get an object from storage.
     * 
     * @param sessionId
     * @param key
     * @return the object
     */
    public static Object get(final String sessionId, final String key) {
        if (sessionId != null) {
            return get(sessionId + Constants.SEPARATOR + key);
        }
        return null;
    }

    /**
     * Save an object in storage.
     * 
     * @param sessionId
     * @param key
     * @param value
     */
    public static void save(final String sessionId, final String key, final Object value) {
        if (sessionId != null) {
            save(sessionId + Constants.SEPARATOR + key, value, Config.getSessionTimeout());
        }
    }

    /**
     * Remove an object in storage.
     * 
     * @param sessionId
     * @param key
     */
    public static void remove(final String sessionId, final String key) {
        remove(sessionId + Constants.SEPARATOR + key);
    }

    /**
     * Get an object from storage.
     * 
     * @param key
     * @return the object
     */
    public static Object get(final String key) {
        return map.get(getCacheKey(key));
    }

    /**
     * Save an object in storage.
     * 
     * @param key
     * @param value
     * @param timeout
     */
    public static void save(final String key, final Object value, final int timeout) {
        if (value == null) {
            map.remove(getCacheKey(key));
        } else {
            map.put(getCacheKey(key), value);
        }
    }

    /**
     * Remove an object from storage.
     * 
     * @param key
     */
    public static void remove(final String key) {
        map.remove(getCacheKey(key));
    }

    static String getCacheKey(final String key) {
        return (StringUtils.isNotBlank(Config.getCacheKeyPrefix())) ? Config.getCacheKeyPrefix() + ":" + key : key;
    }
}
