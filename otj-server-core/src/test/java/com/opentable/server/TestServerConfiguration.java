package com.opentable.server;

import java.io.IOException;

import javax.inject.Named;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.jaxrs.JaxRsClientFactory;
import com.opentable.service.ServiceInfo;

@Configuration
@CoreHttpServerCommon
@Import(LoopbackRequest.class)
@ServletComponentScan
public class TestServerConfiguration {

    public static final String HELLO_WORLD = "Hello, world!";
    private static final Logger LOG = LoggerFactory.getLogger(TestServerConfiguration.class);

    @Bean
    public ServiceInfo getServiceInfo() {
        return () -> "test";
    }

    @WebServlet(urlPatterns = {"/hello/*"}, loadOnStartup = 1)
    public static class HelloWorldServlet extends HttpServlet
    {

        private static final long serialVersionUID = -2041933419195692364L;

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.getWriter().print(HELLO_WORLD);
        }
    }

    /*
    @WebServlet(urlPatterns = {"/health"}, loadOnStartup = 1)
    public static class OtHealthServlet extends HealthCheckServlet {
    }
    */

    @Bean
    @Named("test")
    Client testClient() {
        return JaxRsClientFactory.testBuilder().build();
    }

    public static void main(String[] args) {
        SpringApplication.run(TestServerConfiguration.class, args);
    }

}
