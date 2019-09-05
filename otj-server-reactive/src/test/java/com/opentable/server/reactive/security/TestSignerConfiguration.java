package com.opentable.server.reactive.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.servicesecurity.api.interfaces.keys.ImmutableKeyEnvelope;
import com.opentable.servicesecurity.api.interfaces.keys.PrivateKeySupplier;
import com.opentable.servicesecurity.api.utils.MockPrivateKeySupplier;
import com.opentable.servicesecurity.sign.ServiceSecuritySigner;

@ServiceSecuritySigner
@Import({TestSigner.class})
@Configuration
public class TestSignerConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(TestSignerConfiguration.class);

    // Secret: Private + Public keys for token signing
    @Bean
    public PrivateKeySupplier privateKeySupplier() {
        ImmutableKeyEnvelope.Builder builder = ImmutableKeyEnvelope.builder().id("1");

        try {
            String privateKey = IOUtils.resourceToString("/mykey.pem", StandardCharsets.UTF_8);
            String publicKey = IOUtils.resourceToString("/pubkey.pem", StandardCharsets.UTF_8);
            builder.privateKey(privateKey).publicKey(publicKey);
        } catch (IOException e) {
            LOG.error("Unable to load test key files to construct PrivateKeySupplier");
        }

        return new MockPrivateKeySupplier(builder.build());
    }
}
