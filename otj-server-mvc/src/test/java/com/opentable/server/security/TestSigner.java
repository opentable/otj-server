package com.opentable.server.security;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import com.opentable.servicesecurity.api.interfaces.jwt.ImmutableOTClaims;
import com.opentable.servicesecurity.api.interfaces.jwt.OTClaims;
import com.opentable.servicesecurity.api.interfaces.operations.Signer;
import com.opentable.servicesecurity.api.interfaces.operations.service.JWTHttpHeader;
import com.opentable.servicesecurity.api.model.IdType;
import com.opentable.servicesecurity.api.model.ImmutablePrincipal;
import com.opentable.servicesecurity.api.model.PrincipalType;

/**
 * Test class that injects a signer and signs an OTClaims
 */
public class TestSigner {

    private final Signer signer;

    public TestSigner(Signer signer) {
        this.signer = signer;
    }

    public CompletableFuture<JWTHttpHeader> sign(boolean expired) {
        OTClaims otClaims = ImmutableOTClaims.builder()
                .audience("audience")
                .issuer("TestSigner")
                .expiration(expiration(expired))
                .subject("subject")
                .principal(ImmutablePrincipal.builder()
                        .version(1)
                        .type(PrincipalType.USER)
                        .idType(IdType.GPID)
                        .id("12345")
                        .name("testuser")
                        .build())
                .build();
        return signer.sign(otClaims);
    }

    private Instant expiration(boolean expired) {
        return !expired ? Instant.now().plusSeconds(600) : Instant.now().minusSeconds(600);
    }
}
