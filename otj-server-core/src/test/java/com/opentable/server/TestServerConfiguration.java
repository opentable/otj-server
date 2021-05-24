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

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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
            LOG.debug("Writing hello world response.");
            response.getWriter().print(HELLO_WORLD);
        }
    }

    @WebServlet(urlPatterns = {"/threadname/*"}, loadOnStartup = 1)
    public static class ThreadServlet extends HttpServlet
    {

        private static final long serialVersionUID = 1L;

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            LOG.debug("Writing thread name response.");
            response.getWriter().print(Thread.currentThread().getName());
        }
    }


    public static void main(String[] args) {
        SpringApplication.run(TestServerConfiguration.class, args);
    }

}
