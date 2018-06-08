package com.opentable.server;

import java.util.Map;

@FunctionalInterface
public interface ServletInitParameters {

    /**
     * Get a map of servlet init params to set on the servlet
     * @return a map of servlet init params
     */
    Map<String, String> getInitParams();

}

