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

import java.util.function.Consumer;

import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

@Configuration
@Conditional(EmbeddedJettyLowResourceMonitor.InstallEmbeddedJettyLowResourceMonitor.class)
public class EmbeddedJettyLowResourceMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedJettyLowResourceMonitor.class);

    /**
     * periodMS The period in ms to monitor for low resources
     */
    @Value("${ot.server.low-resource-monitor.period-ms:100}")
    private Integer periodMS;

    /**
     * lowResourcesIdleTimeoutMS The timeout in ms to apply to EndPoints when in the low resources state.
     */
    @Value("${ot.server.low-resource-monitor.low-resource-idle-timeout-ms:100}")
    private Integer lowResourcesIdleTimeoutMS;

    /**
     * monitorThreads If true, check connectors executors to see if they are
     * {@link ThreadPool} instances that are low on threads.
     */
    @Value("${ot.server.low-resource-monitor.monitor-threads:true}")
    private Boolean monitorThreads;

    /**
     * maxLowResourcesTimeMS The time in milliseconds that a low resource state can persist before the low resource idle timeout is reapplied to all connections
     */
    @Value("${ot.server.low-resource-monitor.max-low-resources-time-ms:100}")
    private Integer maxLowResourcesTimeMS;

    /**
     * acceptingInLowResources if false, new connections are not accepted while in low resources
     */
    @Value("${ot.server.low-resource-monitor.accepting-in-low-resources:false}")
    private Boolean acceptingInLowResources;

    public static class InstallEmbeddedJettyLowResourceMonitor implements Condition {
        @Override
        public boolean matches(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
            final String value = context.getEnvironment().
                    getProperty("ot.server.low-resource-monitor.enabled", "false");
            return Boolean.parseBoolean(value);
        }
    }

    LowResourceMonitor lowResourceMonitor(Server server) {
        LowResourceMonitor lowResourcesMonitor = new LowResourceMonitor(server);
        if (periodMS != null) {
            lowResourcesMonitor.setPeriod(periodMS);
        }
        if (lowResourcesIdleTimeoutMS != null) {
            lowResourcesMonitor.setLowResourcesIdleTimeout(lowResourcesIdleTimeoutMS);
        }
        if (monitorThreads != null) {
            lowResourcesMonitor.setMonitorThreads(monitorThreads);
        }
        if (maxLowResourcesTimeMS != null) {
            lowResourcesMonitor.setMaxLowResourcesTime(maxLowResourcesTimeMS);
        }
        if (acceptingInLowResources != null) {
            lowResourcesMonitor.setAcceptingInLowResources(acceptingInLowResources);
        }
        LOG.debug("Creating Jetty low resources monitor: {}", lowResourcesMonitor);
        return lowResourcesMonitor;
    }

    @Bean
    public Consumer<Server> lowResourcesCustomizer() {
        return  s -> s.addBean(lowResourceMonitor(s));
    }

}
