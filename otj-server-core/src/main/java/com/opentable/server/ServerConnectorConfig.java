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

public interface ServerConnectorConfig {
    default String getProtocol() {
        return "http";
    }

    default String getBindAddress() {
        return null;
    }

    default int getPort() {
        return -1;
    }

    default boolean isForceSecure() {
        return false;
    }

    default String getKeystore() {
        return null;
    }

    default String getKeystorePassword() {
        return "changeit";
    }

    default long getIdleTimeout() {
        return 0;
    }

    default boolean isUseDirectBuffers() {
        return false;
    }

    default boolean isSniRequired() {
        return false;
    }

    default boolean isSniHostCheck() {
        return true;
    }

    default boolean isAllowEmptySni() {
        return true;
    }
}
