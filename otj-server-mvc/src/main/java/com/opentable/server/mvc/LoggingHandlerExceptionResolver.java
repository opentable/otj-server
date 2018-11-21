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
package com.opentable.server.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@Component
@Conditional(LoggingHandlerExceptionResolver.InstallLoggingHandlerExceptionResolver.class)
public class LoggingHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

    public static class InstallLoggingHandlerExceptionResolver implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            final String value = context.getEnvironment().
                getProperty("ot.server.mvc.log-resolved-exception", "true");
            return "true".equals(value);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(LoggingHandlerExceptionResolver.class);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Log stack trace for all exceptions which going to be resolved by
     * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}
     */
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            log.warn("", ex);
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            log.warn("", ex);
        }
        if (ex instanceof HttpMediaTypeNotAcceptableException) {
            log.warn("", ex);
        }
        if (ex instanceof MissingPathVariableException) {
            log.warn("", ex);
        }
        if (ex instanceof MissingServletRequestParameterException) {
            log.warn("", ex);
        }
        if (ex instanceof ServletRequestBindingException) {
            log.warn("", ex);
        }
        if (ex instanceof ConversionNotSupportedException) {
            log.warn("", ex);
        }
        if (ex instanceof TypeMismatchException) {
            log.warn("", ex);
        }
        if (ex instanceof HttpMessageNotReadableException) {
            log.warn("", ex);
        }
        if (ex instanceof HttpMessageNotWritableException) {
            log.warn("", ex);
        }
        if (ex instanceof MethodArgumentNotValidException) {
            log.warn("", ex);
        }
        if (ex instanceof MissingServletRequestPartException) {
            log.warn("", ex);
        }
        if (ex instanceof BindException) {
            log.warn("", ex);
        }
        if (ex instanceof NoHandlerFoundException) {
            log.warn("", ex);
        }
        if (ex instanceof AsyncRequestTimeoutException) {
            log.warn("", ex);
        }
        return null;
    }
}
