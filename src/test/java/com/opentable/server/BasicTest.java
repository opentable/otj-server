package com.opentable.server;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestServer.class
})
public class BasicTest {
    @Inject
    LoopbackRequest request;

    @Test
    public void testHello() throws IOException {
        Assert.assertEquals(TestServer.HELLO_WORLD, request.of("/").request().get().readEntity(String.class));
    }

    @Test
    public void testMissing() throws IOException {
        Response r = request.of("/not/found/omg/wtf/bbq").request().get();
        Assert.assertEquals(404, r.getStatus());
    }

    @Test
    public void testStatic_txt() throws IOException {
        testStatic("static/test.txt", "text/plain");
    }

    @Test
    public void testStatic_png() throws IOException {
        testStatic("static/test.png", "image/png");
    }

    private void testStatic(final String path, final String expectedContentType) throws IOException {
        final byte[] expected = IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream(path));
        final Response r = request.of(path).request(expectedContentType).get();
        assertEquals(expectedContentType, r.getHeaderString(HttpHeaders.CONTENT_TYPE));
        final byte[] actual = r.readEntity(byte[].class);
        Assert.assertArrayEquals(expected, actual);
    }
}
