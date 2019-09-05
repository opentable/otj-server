package com.opentable.server.reactive.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.opentable.server.reactive.TestReactiveServerConfiguration;
import com.opentable.servicesecurity.api.interfaces.keys.ImmutableKeyEnvelope;
import com.opentable.servicesecurity.api.interfaces.keys.PublicKeySupplier;
import com.opentable.servicesecurity.api.utils.MockPublicKeySupplier;

/**
 * Test server configuration with public keys. These will be loaded by Credentials Management integration.
 */
@Import({
        TestReactiveServerConfiguration.class
})
public class TestServerWithKeys {

    private static final Logger LOG = LoggerFactory.getLogger(TestServerWithKeys.class);

    // Secret: Public key for token verification
    @Bean
    public PublicKeySupplier publicKeySupplier() {
        ImmutableKeyEnvelope.Builder builder = ImmutableKeyEnvelope.builder().id("1");

        try {
            String publicKey = IOUtils.resourceToString("/pubkey.pem", StandardCharsets.UTF_8);
            builder.publicKey(publicKey);
        } catch (IOException e) {
            LOG.error("Unable to load test key files to construct PublicKeySupplier");
        }

        return new MockPublicKeySupplier(builder.build());
    }
}
