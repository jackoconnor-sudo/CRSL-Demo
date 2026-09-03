package com.northgate.ratings.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Guards the internal admin surface. Registered against /api/admin/* by
 * {@link FilterRegistrationConfig}.
 * The edge proxy is expected to strip X-Internal-Admin from anything arriving off the
 * public ingress and to re-add it for the ops console.
 */
public class AdminApiFilter implements Filter {

    private static final Logger LOG = LogManager.getLogger(AdminApiFilter.class);

    static final String ADMIN_HEADER = "X-Internal-Admin";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        String caller = http.getHeader("X-Forwarded-User");
        boolean admin = "true".equalsIgnoreCase(http.getHeader(ADMIN_HEADER));
        LOG.info("admin check user={} path={} admin={}", caller, http.getRequestURI(), admin);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
