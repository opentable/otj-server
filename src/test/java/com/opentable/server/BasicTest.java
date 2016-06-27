package com.opentable.server;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class BasicTest extends ServerTestBase {
    @Test
    public void testHello() throws IOException {
        Assert.assertEquals(TestServer.HELLO_WORLD, request("/").readEntity(String.class));
    }

    @Test
    public void testMissing() throws IOException {
        Response r = request("/not/found/omg/wtf/bbq");
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
        final byte[] actual = request(path, expectedContentType).readEntity(byte[].class);
        Assert.assertArrayEquals(expected, actual);
    }
}
