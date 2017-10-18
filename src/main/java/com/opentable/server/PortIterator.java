package com.opentable.server;

import java.util.function.IntSupplier;

import org.springframework.core.env.PropertyResolver;

class PortIterator implements IntSupplier {
    private final PropertyResolver pr;

    int portIndex = 0;

    PortIterator(PropertyResolver pr) {
        this.pr = pr;
    }

    @Override
    public int getAsInt() {
        final String portN = pr.getProperty("PORT" + portIndex);
        if (portN == null) {
            if (portIndex == 0) {
                // Default is that a single port with no injected ports picks an arbitrary port: "dev mode"
                portIndex++;
                return 0;
            }
            throw new IllegalStateException("PORT" + portIndex + " not set but needed for connector configuration");
        }
        portIndex++;
        return Integer.parseInt(portN);
    }
}
