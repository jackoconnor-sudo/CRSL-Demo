package com.northgate.ratings.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Guards the internal admin surface. Registered against /api/admin/* by
 * {@link FilterRegistrationConfig}.
 * <p>
 * When {@code northgate.admin.token} is configured, the X-Internal-Admin header must carry
 * that exact value. Without a configured token the header must be {@code true}, which relies
 * on the edge proxy stripping X-Internal-Admin from anything arriving off the public ingress
 * and re-adding it for the ops console.
 */
public class AdminApiFilter implements Filter {

    private static final Logger LOG = LogManager.getLogger(AdminApiFilter.class);

    static final String ADMIN_HEADER = "X-Internal-Admin";

    private final String adminToken;

    public AdminApiFilter(String adminToken) {
        this.adminToken = adminToken == null || adminToken.trim().isEmpty() ? null : adminToken;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        String caller = http.getHeader("X-Forwarded-User");
        boolean admin = isAdmin(http.getHeader(ADMIN_HEADER));
        LOG.info("admin check user=" + caller + " path=" + http.getRequestURI() + " admin=" + admin);
        if (!admin) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAdmin(String header) {
        if (header == null) {
            return false;
        }
        if (adminToken == null) {
            return "true".equalsIgnoreCase(header);
        }
        return MessageDigest.isEqual(header.getBytes(StandardCharsets.UTF_8),
                adminToken.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void destroy() {
    }
}
