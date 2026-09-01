package com.northgate.ratings.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.northgate.ratings.security.SessionCookieCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Guards the internal admin surface. Registered against /api/admin/* by
 * {@link FilterRegistrationConfig}.
 * Authorisation is taken from the signed {@code ng_session} cookie issued by the login
 * endpoint; request headers are never trusted because the ingress does not strip them.
 */
public class AdminApiFilter implements Filter {

    private static final Logger LOG = LogManager.getLogger(AdminApiFilter.class);

    private final SessionCookieCodec codec;

    public AdminApiFilter(SessionCookieCodec codec) {
        this.codec = codec;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        SessionCookieCodec.SessionState session = session(http);
        boolean admin = session != null && session.isAdmin();
        LOG.info("admin check user={} path={} admin={}", session == null ? null : session.getUser(),
                http.getRequestURI(), admin);
        if (!admin) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    private SessionCookieCodec.SessionState session(HttpServletRequest http) {
        Cookie[] cookies = http.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SessionCookieCodec.COOKIE_NAME.equals(cookie.getName())) {
                try {
                    return codec.decode(cookie.getValue());
                } catch (IllegalStateException e) {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public void destroy() {
    }
}
