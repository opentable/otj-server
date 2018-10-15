package com.opentable.server;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.opentable.jaxrs.JaxRsClientConfiguration;

// Install JaxRS client and filters only if configured to do so
@Configuration
@Conditional(ResteasyAutoConfiguration.InstallJAXRS.class)
@Import(JaxRsClientConfiguration.class)
public class JaxRSClientShimConfiguration {
    public static class InstallJAXRS implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            // Only if both or specifically jaxrs
            final String clientType = context.getEnvironment().
                    getProperty("ot.client.type", "all");
            return "all".equals(clientType) || "jaxrs".equals(clientType);
        }
    }
}
