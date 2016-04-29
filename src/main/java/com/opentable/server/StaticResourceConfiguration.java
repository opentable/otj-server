package com.opentable.server;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StaticResourceConfiguration {
    @Bean
    public ServletRegistrationBean staticResourceServlet() {
        DefaultServlet servlet = new DefaultServlet();
        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/static/*");
        bean.addInitParameter("gzip", "true");
        bean.addInitParameter("etags", "true");
        bean.addInitParameter("resourceBase", Resource.newClassPathResource("/static/").getURI().resolve("..").toString());
        return bean;
    }
}
