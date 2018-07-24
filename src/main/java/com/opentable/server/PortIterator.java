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
