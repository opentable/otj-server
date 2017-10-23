package com.opentable.server;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TODO Don't serve directory indexes.

@Configuration
public class StaticResourceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(StaticResourceConfiguration.class);

    static final String DEFAULT_PATH_NAME = "static";
    private static final String PATH_CONFIG_VALUE = "${ot.httpserver.static-path:" + DEFAULT_PATH_NAME + "}";

    private final String staticPathName;

    @Inject
    StaticResourceConfiguration(@Value(PATH_CONFIG_VALUE) final String staticPathName) {
        this.staticPathName = staticPathName;
    }

    public String staticPath() {
        return staticPath("");
    }

    public String staticPath(final String rest) {
        return String.format("/%s/%s", staticPathName, rest);
    }

    @Bean
    public ServletRegistrationBean staticResourceServlet() {
        final Resource rsrc = Resource.newClassPathResource(staticPath());
        LOG.debug("Found static resources at {}", rsrc);

        if (rsrc == null) {
            LOG.info("Didn't find '/static' on classpath, not serving static files");
            return new ServletRegistrationBean() {
                // TODO: less hacky way to do this?
                @Override
                public void onStartup(ServletContext servletContext) throws ServletException {
                }
            };
        }

        DefaultServlet servlet = new DefaultServlet();
        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, staticPath() + "*");
        bean.addInitParameter("gzip", "true");
        bean.addInitParameter("etags", "true");
        bean.addInitParameter("resourceBase", StringUtils.substringBeforeLast(rsrc.toString(), staticPathName));
        LOG.debug("Configuring static resources: {}", bean.getInitParameters());
        return bean;
    }
}
