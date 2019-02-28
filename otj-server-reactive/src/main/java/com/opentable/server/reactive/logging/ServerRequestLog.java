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
package com.opentable.server.reactive.logging;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.net.HttpHeaders;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import com.opentable.httpheaders.OTHeaders;
import com.opentable.logging.CommonLogHolder;
import com.opentable.logging.jetty.JsonRequestLog;
import com.opentable.logging.jetty.JsonRequestLogConfig;
import com.opentable.logging.otl.HttpV1;

/**
 * Logs {@link HttpV1} events for incoming server requests using the underlying Jetty RequestLog.
 *
 * The primary difference between this class and {@link JsonRequestLog} is that this class pulls the OT-RequestId
 * header off of the incoming server request, instead of the response.
 *
 * See {@link com.opentable.server.EmbeddedJettyBase} for details on how this is wired up to the underlying
 * Jetty server.
 */
public class ServerRequestLog extends JsonRequestLog {

    private final Clock clock;

    ServerRequestLog(Clock clock, JsonRequestLogConfig config) {
        super(clock, config);
        this.clock = clock;
    }

    @Override
    @Nonnull
    protected HttpV1 createEvent(Request request, Response response) {
        final String query = request.getQueryString();
        return HttpV1.builder()
                .logName("request")
                .serviceType(CommonLogHolder.getServiceType())
                .uuid(UUID.randomUUID())
                .timestamp(Instant.ofEpochMilli(request.getTimeStamp()))

                .method(request.getMethod())
                .status(response.getStatus())
                .incoming(true)
                .url(fullUrl(request))
                .urlQuerystring(query)

                .duration(TimeUnit.NANOSECONDS.toMicros(
                        Duration.between(
                                Instant.ofEpochMilli(request.getTimeStamp()),
                                clock.instant())
                                .toNanos()))

                .bodySize(request.getContentLengthLong())
                .responseSize(response.getContentCount())

                .acceptLanguage(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE))
                .anonymousId(request.getHeader(OTHeaders.ANONYMOUS_ID))
                .referer(request.getHeader(HttpHeaders.REFERER))
                .referringHost(request.getHeader(OTHeaders.REFERRING_HOST))
                .referringService(request.getHeader(OTHeaders.REFERRING_SERVICE))
                .remoteAddress(request.getRemoteAddr())
                .requestId(optUuid(request.getHeader(OTHeaders.REQUEST_ID)))
                .sessionId(request.getHeader(OTHeaders.SESSION_ID))
                .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                .userId(request.getHeader(OTHeaders.USER_ID))
                .headerOtOriginaluri(request.getHeader(OTHeaders.ORIGINAL_URI))

                .headerOtDomain(request.getHeader(OTHeaders.DOMAIN))
                .headerHost(request.getHeader(HttpHeaders.HOST))
                .headerAccept(request.getHeader(HttpHeaders.ACCEPT))

                .headerXForwardedFor(request.getHeader(HttpHeaders.X_FORWARDED_FOR))
                .headerXForwardedPort(request.getHeader(HttpHeaders.X_FORWARDED_PORT))
                .headerXForwardedProto(request.getHeader(HttpHeaders.X_FORWARDED_PROTO))

                .build();
    }
}
