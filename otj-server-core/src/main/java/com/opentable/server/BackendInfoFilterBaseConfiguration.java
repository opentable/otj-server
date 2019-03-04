package com.opentable.server;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import com.opentable.service.AppInfo;
import com.opentable.service.ServiceInfo;

/**
 * Base configuration for a Backend Info Filter.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class BackendInfoFilterBaseConfiguration {
    public static final String HEADER_PREFIX = "X-OT-Backend-";

    /**
     * @return map of headers we'll add to responses; unavailable information will result in headers
     * not being set
     */
    public static Map<String, String> assembleInfo(final AppInfo appInfo, final ServiceInfo serviceInfo) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.put(named("Service-Name"), serviceInfo.getName());

        if (appInfo.getBuildTag() != null) {
            builder.put(named("Build-Tag"), appInfo.getBuildTag());
        }

        final Integer instanceNo = appInfo.getInstanceNumber();
        if (instanceNo != null) {
            builder.put(named("Instance-No"), instanceNo.toString());
        }

        if (appInfo.getTaskHost() != null) {
            builder.put(named("Task-Host"), appInfo.getTaskHost());
        }

        return builder.build();
    }

    private static String named(final String name) {
        return HEADER_PREFIX + name;
    }
}
