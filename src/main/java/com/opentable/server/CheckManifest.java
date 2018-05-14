package com.opentable.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckManifest {
    private static final String COMMIT = "X-BasePOM-Git-Commit-Id";

    private static final Logger LOG = LoggerFactory.getLogger(CheckManifest.class);

    public void readManifests() throws IOException {
        final Enumeration<URL> urlsEnum = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
        while (urlsEnum.hasMoreElements()) {
            readManifest(urlsEnum.nextElement());
        }
    }

    private void readManifest(final URL url) throws IOException {
        try (InputStream is = url.openStream()) {
            if (is !=  null) {
                final Manifest mf = new Manifest();
                mf.read(is);
                final Attributes atts = mf.getMainAttributes();
                Optional.ofNullable(atts.getValue(Attributes.Name.IMPLEMENTATION_TITLE)).ifPresent(t -> LOG.debug("Implementation Title: {}", t));
                Optional.ofNullable(atts.getValue(Attributes.Name.IMPLEMENTATION_VERSION)).ifPresent(t -> LOG.debug("Implementation Version: {}", t));
                Optional.ofNullable(atts.getValue(COMMIT)).ifPresent(t -> LOG.debug("Git commit: {}", t));
            }
        }
    }

    @PostConstruct
    public void start() {
        try {
            readManifests();
        } catch (IOException e) {
            LOG.debug("Error while reading manifest", e);
        }
    }
}
