package com.opentable.server;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import com.opentable.conservedheaders.ConservedHeader;
import com.opentable.conservedheaders.ConservedHeaders;

public class ConservedHeadersTest extends ServerTestBase {
    private final static String RID = ConservedHeader.REQUEST_ID.getHeaderName();
    private final static String AID = ConservedHeader.ANONYMOUS_ID.getHeaderName();

    @Test
    public void createRequestIdIndex() throws IOException {
        sanityCheck(request("/").get());
    }

    @Test
    public void createRequestId404() throws IOException {
        sanityCheck(request("/404/not/found").get());
    }

    @Test
    public void conserveRequestId() throws IOException {
        final String rid = UUID.randomUUID().toString();
        final Response resp = request("/").header(RID, rid).get();
        sanityCheck(resp);
        Assert.assertEquals(rid, resp.getHeaderString(RID));
    }

    @Test
    public void replaceBadRequestId() throws IOException {
        final String badRid = "not a valid UUID";
        final Response resp = request("/").header(RID, badRid).get();
        sanityCheck(resp);
        Assert.assertNotEquals(badRid, resp.getHeaderString(RID));
    }

    @Test
    public void conserveAnonymousId() throws IOException {
        final String aid = "fgsfds";
        final Response resp = request("/").header(AID, aid).get();
        sanityCheck(resp);
        Assert.assertEquals(aid, resp.getHeaderString(AID));
    }

    private void sanityCheck(final Response resp) {
        final MultivaluedMap<String, Object> headers = resp.getHeaders();
        final List<Object> requestIds = headers.get(RID);
        Assert.assertNotNull(requestIds);
        Assert.assertEquals(requestIds.size(), 1);
        Assert.assertNotNull(requestIds.get(0));
    }
}
