package com.opentable.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
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
        LOG.debug("Found static resources at {}", rsrc);

        if (rsrc == null) {
            LOG.warn("Didn't find '/static' on classpath, not serving static files");
            return new ServletRegistrationBean() {
                // TODO: less hacky way to do this?
                @Override
                public void onStartup(ServletContext servletContext) throws ServletException {
                }
            };
        }

        DefaultServlet servlet = new DefaultServlet();
        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/static/*");
        bean.addInitParameter("gzip", "true");
        bean.addInitParameter("etags", "true");
        bean.addInitParameter("resourceBase", StringUtils.substringBeforeLast(rsrc.toString(), "static"));
        LOG.debug("Configuring static resources: {}", bean.getInitParameters());
        return bean;
    }
}
