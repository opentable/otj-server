package com.opentable.vault;

import java.net.URI;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.support.VaultToken;

import com.opentable.jaxrs.JaxRsClientFactory;
import com.opentable.jaxrs.StandardFeatureGroup;
import com.opentable.service.AppInfo;

@Configuration
public class OTVaultConfiguration extends AbstractVaultConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(OTVaultConfiguration.class);

    @Value("${ot.vault.vgm-token:srvc://vgm/token}")
    private URI vgmTokenUri;

    @Value("${ot.vault.uri:https://vault-server-pp.otenv.com}")
    private URI vaultUri;

    @Inject
    AppInfo appInfo;

    @Inject
    private JaxRsClientFactory factory;

    @Override
    @Singleton
    public VaultEndpoint vaultEndpoint() {
        return VaultEndpoint.from(vaultUri);
    }

    @Override
    @Singleton
    public ClientAuthentication clientAuthentication() {
        return new UnwrapApiAuthentication(vaultUri + "/v1/sys/wrapping/unwrap", initialToken(), factory.newClient("vault-unwrap", StandardFeatureGroup.PUBLIC));
    }

    private VaultToken initialToken() {
        final Client vgm = factory.newClient("vgm", StandardFeatureGroup.PLATFORM_INTERNAL);
        try {
            final VgmTokenResponse response = vgm.target(vgmTokenUri)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(Collections.singletonMap("task_id", appInfo.getTaskId())), VgmTokenResponse.class);
            LOG.info("VGM response: {} status '{}' token '{}'",
                    response.isOk() ? "success" : "FAILURE", response.getStatus(), response.getToken());
            return VaultToken.of(response.getToken());
        } catch (WebApplicationException e) {
            final Response r = e.getResponse();
            LOG.error("VGM cubbyhole authentication failure: {} {}",
                    r.getStatusInfo(), r.readEntity(String.class));
            return VaultToken.of("invalid"); // don't blow up startup, but all future Vault operations fail
        } catch (Exception e) {
            LOG.error("VGM cubbyhole authentication failure", e);
            return VaultToken.of("invalid"); // don't blow up startup, but all future Vault operations fail
        } finally {
            vgm.close();  // Damn you JAX-RS for not implementing Closeable
        }
    }

    public interface VgmTokenResponse {
        String getStatus();
        boolean isOk();
        String getToken();
    }
}
