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
import java.util.UUID;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import com.opentable.httpheaders.OTHeaders;
import com.opentable.logging.jetty.JsonRequestLog;
import com.opentable.logging.jetty.JsonRequestLogConfig;
import com.opentable.logging.otl.HttpV1;

/**
 * NOTE: With the introduction of otj-conservedheaders-reactive, this class is no longer needed, and implementing
 * reactive servers should now use or extend {@link JsonRequestLog} directly.
 *
 * We are leaving this here for the time being as some reactive servers are directly extending this class. It will
 * be removed in the future once servers are updated to a parent pom version that supports otj-conservedheaders-reactive.
 *
 * ------
 *
 * Logs {@link HttpV1} events for incoming server requests using the underlying Jetty RequestLog.
 *
 * The *original* difference between this class and {@link JsonRequestLog} is that this class pulls the OT-RequestId
 * header off of the incoming server request, instead of the response.
 *
 * EDIT: This behavior has been changed to match the original behavior of {@link JsonRequestLog}, by pulling
 * the request id from the response.
 *
 * @deprecated Users of this class should extend JsonRequestLog directly instead of extending ServerRequestLog.
 */
@Deprecated
public class ServerRequestLog extends JsonRequestLog {

    public ServerRequestLog(Clock clock, JsonRequestLogConfig config) {
        super(clock, config);
    }

    @Override
    protected UUID getRequestIdFrom(Request request, Response response) {
        return optUuid(response.getHeader(OTHeaders.REQUEST_ID));
    }
}
