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

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    public ServletRegistrationBean<DefaultServlet> staticResourceServlet() {
        try(Resource rsrc = Resource.newClassPathResource(staticPath())){
            LOG.debug("Found static resources at {}", rsrc);

            if (rsrc == null) {
                LOG.info("Didn't find '/static' on classpath, not serving static files");
                ServletRegistrationBean<DefaultServlet> servletRegistrationBean =  new ServletRegistrationBean<>(new DefaultServlet());
                servletRegistrationBean.setName("static-inactive");
                servletRegistrationBean.setEnabled(false);
                return servletRegistrationBean;
            }

            DefaultServlet servlet = new DefaultServlet();
            ServletRegistrationBean<DefaultServlet> bean = new ServletRegistrationBean<>(servlet, staticPath() + "*");
            bean.addInitParameter("gzip", "true");
            bean.addInitParameter("etags", "true");
            bean.addInitParameter("resourceBase", StringUtils.substringBeforeLast(rsrc.toString(), staticPathName));
            LOG.debug("Configuring static resources: {}", bean.getInitParameters());
            return bean;
        }
    }
}
