/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.server;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.io.QuietException;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;
import org.springframework.web.util.NestedServletException;

import com.opentable.components.filterorder.OrderDeclaration;
import com.opentable.scopes.threaddelegate.servlet.ThreadDelegatingScopeFilter;


@Component
@Conditional(ExceptionLogFilterConfiguration.InstallExceptionLogFilter.class)
@Import(ExceptionLogFilterConfiguration.ExceptionLogFilter.class)
public class ExceptionLogFilterConfiguration {

    public static class InstallExceptionLogFilter implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            final String value = context.getEnvironment().
                getProperty("ot.server.exception-log-filter", "true");
            return "true".equals(value);
        }
    }

    @Component
    public static class ExceptionLogFilter implements Filter {
        private static final Logger LOG = LoggerFactory.getLogger(ServletHandler.class);
        @Override
        public void init(FilterConfig filterConfig) {
            LOG.info("Exception log filter enabled.");
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            try {
                chain.doFilter(request, response);
            } catch (RuntimeException e) {
                final String url = ((HttpServletRequest)request).getRequestURI();
                LOG.warn("{}", url, e);
                throw new QuietServletException(ExceptionUtils.getRootCause(e)); //NOPMD
            } catch (ServletException e) {
                final String url = ((HttpServletRequest)request).getRequestURI();
                LOG.warn("{}", url, e);
                throw new QuietServletException(e.getRootCause()); //NOPMD
            }
        }

        @Override
        public void destroy() {
        }
    }

    public static class QuietServletException extends NestedServletException implements QuietException {
        private static final long serialVersionUID = 1L;
        QuietServletException(Throwable rootCause) {
            super(null, rootCause);
        }
    }

    @Bean
    public FilterRegistrationBean<ExceptionLogFilter> exceptionLogFilterRegistration(final ExceptionLogFilter filter) {
        return new FilterRegistrationBean<>(filter);
    }

    @Bean
    public OrderDeclaration exceptionLogFilterOrderDeclaration() {
        return OrderDeclaration.of(ExceptionLogFilter.class).dependsOn(ThreadDelegatingScopeFilter.class);
    }
}
