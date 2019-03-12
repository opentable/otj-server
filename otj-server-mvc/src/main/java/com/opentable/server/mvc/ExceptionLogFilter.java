package com.opentable.server.mvc;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.io.QuietException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.NestedServletException;


@Component
public class ExceptionLogFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionLogFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
        LOG.info("Exception log filter enabled.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (ServletException e) {
            final String url = ((HttpServletRequest)request).getRequestURI();
            LOG.warn("{}", url, e);
            throw new QuietServletException(e.getRootCause()); //NOPMD
        }
    }

    @Override
    public void destroy() {

    }

    public static class QuietServletException extends NestedServletException implements QuietException {

        private static final long serialVersionUID = 1L;

        QuietServletException(Throwable rootCause) {
            super(null, rootCause);
        }
    }
}
