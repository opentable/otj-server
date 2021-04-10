package com.opentable.server;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class WireBackendInfo implements Condition {

    public static final String OT_SERVER_BACKEND_INFO_ENABLED = "ot.server.backend.info.enabled";

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        return Boolean.parseBoolean(conditionContext.getEnvironment().getProperty(OT_SERVER_BACKEND_INFO_ENABLED, "true"));
    }
}
