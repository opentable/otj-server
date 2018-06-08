package com.opentable.server;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.opentable.conservedheaders.ConservedHeader;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestServer.class
})
public class ConservedHeadersTest {
    private final static String RID = ConservedHeader.REQUEST_ID.getHeaderName();
    private final static String AID = ConservedHeader.ANONYMOUS_ID.getHeaderName();

    @Inject
    LoopbackRequest request;

    @Test
    public void createRequestIdIndex() {
        sanityCheck(request.of("/").request().get());
    }

    @Test
    public void createRequestId404() {
        sanityCheck(request.of("/404/not/found").request().get());
    }

    @Test
    public void conserveRequestId() {
        final String rid = UUID.randomUUID().toString();
        try(final Response resp = request.of("/").request().header(RID, rid).get()){
            sanityCheck(resp);
            Assert.assertEquals(rid, resp.getHeaderString(RID));
        }
    }

    @Test
    public void replaceBadRequestId() {
        final String badRid = "not a valid UUID";
        try(final Response resp = request.of("/").request().header(RID, badRid).get()){
            sanityCheck(resp);
            Assert.assertNotEquals(badRid, resp.getHeaderString(RID));
        }
    }

    @Test
    public void conserveAnonymousId() {
        final String aid = "fgsfds";
        try(final Response resp = request.of("/").request().header(AID, aid).get()){
            sanityCheck(resp);
            Assert.assertEquals(aid, resp.getHeaderString(AID));
        }
    }

    private void sanityCheck(final Response resp) {
        final MultivaluedMap<String, Object> headers = resp.getHeaders();
        final List<Object> requestIds = headers.get(RID);
        Assert.assertNotNull(requestIds);
        Assert.assertEquals(requestIds.size(), 1);
        Assert.assertNotNull(requestIds.get(0));
    }
}
