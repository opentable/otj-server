package com.opentable.vault;

import java.time.Duration;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.LoginToken;
import org.springframework.vault.support.VaultToken;

public class UnwrapApiAuthentication implements ClientAuthentication {
    private static final Logger LOG = LoggerFactory.getLogger(UnwrapApiAuthentication.class);

    private final String endpoint;
    private final VaultToken vgmToken;
    private final Client http;

    public UnwrapApiAuthentication(String endpoint, VaultToken vgmToken, Client http) {
        this.endpoint = endpoint;
        this.vgmToken = vgmToken;
        this.http = http;
    }

    @Override
    public VaultToken login() throws VaultException {
        final String t = vgmToken.getToken();
        try {
            return login0();
        } catch (WebApplicationException e) {
            throw new VaultException("Unable to complete VGM token '" + t + "' unwrap: " + e.getResponse().readEntity(String.class), e);
        } catch (RuntimeException e) {
            throw new VaultException("Unable to complete VGM token '" + t + "' unwrap", e);
        }
    }

    private VaultToken login0() {
        final VaultResponse r = http.target(endpoint)
                .request(MediaType.APPLICATION_JSON)
                .header("X-Vault-Token", vgmToken.getToken())
                .post(Entity.text(""), VaultResponse.class);

        final AuthResponse auth = r.getAuth();
        final String token = auth.getToken();
        final Set<String> policies = auth.getPolicies();
        final int leaseDuration = auth.getLeaseDurationSeconds();
        final boolean renewable = auth.isRenewable();

        LOG.info("Vault exchange complete, used VGM token {} to get Vault token {} duration {} policies {} renewable {}",
                vgmToken.getToken(), token, Duration.ofSeconds(leaseDuration), policies, renewable);

        if (renewable) {
            return LoginToken.renewable(token, leaseDuration);
        } else {
            return LoginToken.of(token, leaseDuration);
        }
    }

    public interface VaultResponse {
        AuthResponse getAuth();
    }

    public interface AuthResponse {
        @JsonProperty("client_token")
        String getToken();

        Set<String> getPolicies();

        @JsonProperty("lease_duration")
        int getLeaseDurationSeconds();

        boolean isRenewable();
    }
}
