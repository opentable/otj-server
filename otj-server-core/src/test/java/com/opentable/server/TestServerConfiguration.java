package com.opentable.server;

import java.io.IOException;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.opentable.service.ServiceInfo;

@Configuration
@CoreHttpServerCommon
public class TestServerConfiguration {

    public static final String HELLO_WORLD = "Hello, world!";
    private static final Logger LOG = LoggerFactory.getLogger(TestServerConfiguration.class);

    @Bean
    public ServiceInfo getServiceInfo() {
        return new ServiceInfo() {
            @Override
            public String getName() {
                return "test";
            }
        };
    }

    @Bean
    public Function<Handler, Handler> gzipAllTheThings() {
        return h -> {
            LOG.info("Installing Hello World handler");
            return new AbstractHandler() {
                @Override
                public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
                    response.setContentType("text/html;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    response.getWriter().print(HELLO_WORLD);
                }
            };
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(TestServerConfiguration.class, args);
    }

}
