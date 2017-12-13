package com.opentable.jaxrs.referrer;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.opentable.jackson.OpenTableJacksonConfiguration;
import com.opentable.jaxrs.JaxRsClientConfiguration;

/**
 * Sometimes, folks test with more minimal JAX-RS setups than a full service.  This test ensures that the referrer
 * filter functionality doesn't blow up in these circumstances.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {
        OpenTableJacksonConfiguration.class,
        JaxRsClientConfiguration.class,
})
public class JaxrsReferrerMinimalTest {
    @Inject
    private ClientReferrerFilter filter;

    @Test
    public void test() {
        Assertions.assertThat(filter.isActive()).isFalse();
    }
}
