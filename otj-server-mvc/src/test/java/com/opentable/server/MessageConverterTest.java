package com.opentable.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class MessageConverterTest extends AbstractTest {

    @Autowired
    private TestRestTemplate restTemplate;


    private TestMvcServerConfiguration.MrBean getMrBean(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForObject("/api/map",
            new HttpEntity<>(body, headers), TestMvcServerConfiguration.MrBean.class);
    }

    @Test
    public void testDeserialization() {
        TestMvcServerConfiguration.MrBean mrBean = restTemplate.getForObject("/api/rsp", TestMvcServerConfiguration.MrBean.class);
        assertEquals("1", mrBean.getBar());
        assertEquals("2", mrBean.getFoo());
    }
    
    @Test
    public void testParameterNames() {
        TestMvcServerConfiguration.MrBean mrBean = getMrBean("{\"bar\":\"1\",\"foo\":\"2\"}");
        assertEquals("1", mrBean.getBar());
        assertEquals("2", mrBean.getFoo());
    }

    @Test
    public void testBadParameterName() {
        TestMvcServerConfiguration.MrBean mrBean = getMrBean("{\"bax\":\"1\",\"foo\":\"2\"}");
        assertNull(mrBean.getBar());
        assertEquals("2", mrBean.getFoo());
    }

}
