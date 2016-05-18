package com.opentable.server;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StaticResourceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(StaticResourceConfiguration.class);

    @Bean
    public ServletRegistrationBean staticResourceServlet() {

        final Resource rsrc = Resource.newClassPathResource("/static/");

        if (rsrc == null) {
            LOG.warn("Didn't find '/static' on classpath, not serving static files");
            return new ServletRegistrationBean();
        }

        DefaultServlet servlet = new DefaultServlet();
        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/static/*");
        bean.addInitParameter("gzip", "true");
        bean.addInitParameter("etags", "true");
        bean.addInitParameter("resourceBase", rsrc.getURI().resolve("..").toString());
        LOG.info("Configuring static resources: {}", bean.getInitParameters());
        return bean;
    }
}
