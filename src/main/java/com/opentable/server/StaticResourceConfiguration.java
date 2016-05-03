package com.opentable.server;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StaticResourceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(StaticResourceConfiguration.class);

    @Bean
    public FactoryBean<ServletRegistrationBean> staticResourceServlet() {

        final Resource rsrc = Resource.newClassPathResource("/static/");

        if (rsrc == null) {
            LOG.warn("Didn't find '/static' on classpath, not serving static files");
            return new ServletFactoryBean(null);
        }

        DefaultServlet servlet = new DefaultServlet();
        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/static/*");
        bean.addInitParameter("gzip", "true");
        bean.addInitParameter("etags", "true");
        bean.addInitParameter("resourceBase", rsrc.getURI().resolve("..").toString());
        return new ServletFactoryBean(bean);
    }

    static class ServletFactoryBean implements FactoryBean<ServletRegistrationBean> {
        private final ServletRegistrationBean bean;

        ServletFactoryBean(ServletRegistrationBean bean) {
            this.bean = bean;
        }

        @Override
        public ServletRegistrationBean getObject() throws Exception {
            return bean;
        }

        @Override
        public Class<?> getObjectType() {
            return ServletRegistrationBean.class;
        }

        @Override
        public boolean isSingleton() {
            return true;
        }
    }
}
