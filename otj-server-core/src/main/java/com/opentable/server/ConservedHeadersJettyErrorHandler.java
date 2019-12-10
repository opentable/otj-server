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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;

import com.opentable.conservedheaders.ConservedHeadersFilter;
import com.opentable.httpheaders.HeaderBlacklist;

class ConservedHeadersJettyErrorHandler extends ErrorPageErrorHandler {
    private static final HeaderBlacklist HEADER_BLACKLIST = HeaderBlacklist.INSTANCE;
    private final ErrorHandler delegate;


    ConservedHeadersJettyErrorHandler(ErrorHandler delegate, boolean showStacks) {
        this.delegate = delegate;
        setShowStacks(showStacks);
    }

    @Override
    public void setShowStacks(boolean showStacks) {
        super.setShowStacks(showStacks);
        if (this.delegate != null) {
            this.delegate.setShowStacks(showStacks);
        }
    }

    @Override
    @SuppressWarnings("PMD.AvoidRethrowingException")
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (this.delegate != null) {
            ConservedHeadersFilter.extractHeaders(request)
                .forEach((header, value) -> {
                    if (HEADER_BLACKLIST.canCopyToResponse(header.getHeaderName())) {
                        response.setHeader(header.getHeaderName(), value);
                    }
                });
            // ServletException wont be thrown in future jetty, so catch, wrap
            try {
                this.delegate.handle(target, baseRequest, request, response);
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setErrorPages(Map<String, String> errorPages) {
        super.setErrorPages(errorPages);
        if (delegate instanceof ErrorPageErrorHandler) {
            ((ErrorPageErrorHandler)delegate).setErrorPages(errorPages);
        }
    }

    @Override
    public void addErrorPage(Class<? extends Throwable> exception, String uri) {
        super.addErrorPage(exception, uri);
        if (delegate instanceof ErrorPageErrorHandler) {
            ((ErrorPageErrorHandler)delegate).addErrorPage(exception, uri);
        }
    }

    @Override
    public void addErrorPage(String exceptionClassName, String uri) {
        super.addErrorPage(exceptionClassName, uri);
        if (delegate instanceof ErrorPageErrorHandler) {
            ((ErrorPageErrorHandler)delegate).addErrorPage(exceptionClassName, uri);
        }
    }

    @Override
    public void addErrorPage(int code, String uri) {
        super.addErrorPage(code, uri);
        if (delegate instanceof ErrorPageErrorHandler) {
            ((ErrorPageErrorHandler)delegate).addErrorPage(code, uri);
        }
    }

    @Override
    public void addErrorPage(int from, int to, String uri) {
        super.addErrorPage(from, to, uri);
        if (delegate instanceof ErrorPageErrorHandler) {
            ((ErrorPageErrorHandler)delegate).addErrorPage(from, to, uri);
        }
    }
}


