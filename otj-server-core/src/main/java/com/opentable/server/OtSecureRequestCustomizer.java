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

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.bucket.BucketLog;

@SuppressWarnings({"PMD.MoreThanOneLogger"})
public class OtSecureRequestCustomizer extends SecureRequestCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(HttpChannel.class);
    private static final Logger BUCKET_LOG = BucketLog.of(HttpChannel.class, 1, Duration.ofSeconds(10)); // 1 per 10 second
    private final ServerConnectorConfig config;
    private Optional<BiConsumer<SSLEngine, Request>> sniErrorCallback = Optional.empty();

    public OtSecureRequestCustomizer(ServerConnectorConfig config) {
        super(config.isSniRequired(), config.isSniHostCheck(), -1, false);
        this.config = config;
    }

    @Override
    protected void customize(SSLEngine sslEngine, Request request) {
        final String sniHost = (String) sslEngine.getSession().getValue(SslContextFactory.Server.SNI_HOST);
        if ((sniHost != null) || !config.isAllowEmptySni()) {
            try {
                super.customize(sslEngine, request);  // will default to jetty 10 defaults ie - different sni behaviour from 9
            } catch (BadMessageException ex) {
                LOG.error("Invalid SNI: Host={}, SNI={}, SNI Certificate={}, peerHost={}, peerPort={}",
                    request.getServerName(),
                    sniHost,
                    sslEngine.getSession().getValue(X509_CERT),
                    sslEngine.getPeerHost(),
                    sslEngine.getPeerPort());
                sniErrorCallback.ifPresent(c -> c.accept(sslEngine, request));
                throw ex;
            }
        } else {
            BUCKET_LOG.warn("SNIHOST: Host={}, SNI=null, SNI Certificate={}, peerHost={}, peerPort={}",
                request.getServerName(),
                sslEngine.getSession().getValue(X509_CERT),
                sslEngine.getPeerHost(),
                sslEngine.getPeerPort());
        }
    }

    public void setSniErrorCallback(BiConsumer<SSLEngine, Request> sniErrorCallback) {
        this.sniErrorCallback = Optional.ofNullable(sniErrorCallback);
    }
}
