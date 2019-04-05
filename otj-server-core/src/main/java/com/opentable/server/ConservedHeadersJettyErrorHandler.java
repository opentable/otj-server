package com.opentable.server;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;

import com.opentable.conservedheaders.ConservedHeadersFilter;

class ConservedHeadersJettyErrorHandler extends ErrorPageErrorHandler {

    private final ErrorHandler delegate;

    ConservedHeadersJettyErrorHandler(ErrorHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (this.delegate != null) {
            ConservedHeadersFilter.extractHeaders(request)
                .forEach((header, value) -> response.setHeader(header.getHeaderName(), value));
            this.delegate.handle(target, baseRequest, request, response);
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


