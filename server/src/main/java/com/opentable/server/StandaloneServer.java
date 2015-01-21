/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.server;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

import org.apache.commons.lang3.time.StopWatch;

import com.opentable.config.Config;
import com.opentable.config.ConfigModule;
import com.opentable.jmx.JmxModule;
import com.opentable.lifecycle.Lifecycle;
import com.opentable.lifecycle.LifecycleStage;
import com.opentable.lifecycle.guice.LifecycleModule;
import com.opentable.logging.AssimilateForeignLogging;
import com.opentable.logging.Log;
import com.opentable.serverinfo.ServerInfo;
import com.opentable.util.JvmFallbackShutdown;

/**
 * Standalone main class.
 *
 * Environment properties:
 *
 * <ul>
 *  <li>ot.config.location - An URI to load configuration from</li>
 *  <li>ot.config - A configuration path to load from the config URI</li>
 * </ul>
 *
 * This installs a basic set of Guice modules that every server should use:
 * <ul>
 *  <li>configuration</li>
 *  <li>lifecycle</li>
 *  <li>log control over jmx</li>
 *  <li>jmx export</li>
 *  <li>jmx starter</li>
 * </ul>
 */
public abstract class StandaloneServer
{
    private static final Log LOG = Log.findLog();

    private final Config config;
    private final String serverToken;

    private final Thread shutdownThread = new Thread("Server Shutdown Thread")
    {
        @Override
        public void run() {
            LOG.info("Shutting Service down");
            doStopServer(true);
        }
    };

    @Inject
    private Lifecycle lifecycle;

    @Inject
    private Injector injector;

    @Inject(optional = true)
    private PortNumberProvider portNumberProvider;

    private boolean started = false;
    private boolean stopped = false;

    /**
     * Create a StandaloneServer with a given config.
     * If no config is provided (null) then the config
     * is loaded from the system environment via
     * {@link Config#getConfig()}.
     */
    public StandaloneServer(@Nullable final Config config)
    {
        this.config = config == null ? Config.getConfig() : config;
        this.serverToken = UUID.randomUUID().toString();

        // Suck java.util.logging into log4j
        AssimilateForeignLogging.assimilate();
    }

    /**
     * Returns the main guice module for the server.
     */
    protected abstract Module getMainModule();

    /**
     * Returns the server template module.
     */
    protected abstract Module getServerTemplateModule();

    /**
     * Returns the server type. Must be set so that the server info contains
     * the right server type.
     */
    protected abstract String getServerType();

    public void startServer()
    {
        Preconditions.checkState(!started, "Server was already started, double-start denied!");

        ServerInfo.add(ServerInfo.SERVER_TYPE, getServerType());
        ServerInfo.add(ServerInfo.SERVER_TOKEN, getServerToken());

        final Object binaryVersion = ServerInfo.get(ServerInfo.SERVER_BINARY);

        LOG.info("Service startup begins (type: %s, token: %s)", ServerInfo.get(ServerInfo.SERVER_TYPE),
                                                                 ServerInfo.get(ServerInfo.SERVER_TOKEN));

        if (binaryVersion != null) {
            LOG.info("Binary: %s, version: %s, running in %s mode.", binaryVersion,
                                                                     ServerInfo.get(ServerInfo.SERVER_VERSION),
                                                                     ServerInfo.get(ServerInfo.SERVER_MODE));
        }

        final StopWatch timer = new StopWatch();
        timer.start();

        injector = getInjector();
        injector.injectMembers(this);

        timer.stop();
        final long injectorTime = timer.getTime();
        timer.reset();

        Preconditions.checkNotNull(lifecycle, "No Lifecycle Object was injected!");

        Runtime.getRuntime().addShutdownHook(shutdownThread);

        LOG.info("Starting Service");
        timer.start();
        try {
            lifecycle.executeTo(getStartStage());
        } catch (RuntimeException e) {
            fallbackTerminate();
            try {
                removeShutdownHook();
                lifecycle.execute(getStopStage());
            } catch (Exception innerExc) {
                LOG.error(innerExc, "Failed to stop");
            }
            throw e;
        }
        timer.stop();

        started = true;
        LOG.info("Service startup completed; %d ms in module initialization and %d ms to start lifecycle.", injectorTime, timer.getTime());
    }

    public void stopServer()
    {
        fallbackTerminate();
        doStopServer(false);
    }

    private void doStopServer(boolean fromHook) {
        Preconditions.checkState(!stopped, "Server was already stopped, double-stop denied!");

        Preconditions.checkNotNull(lifecycle, "No Lifecycle Object was injected!");

        LOG.info("Stopping Service");
        lifecycle.executeTo(getStopStage());
        if (!fromHook) {
            removeShutdownHook();
        }

        stopped = true;
    }

    private void removeShutdownHook() {
        Runtime.getRuntime().removeShutdownHook(shutdownThread);
    }

    public boolean isStarted()
    {
        return started;
    }

    public boolean isStopped()
    {
        return stopped;
    }

    /**
     * Can be overridden in tests.
     */
    public Module getPlumbingModules()
    {
        return new Module() {
            @Override
            public void configure(final Binder binder) {
                binder.bind(Clock.class).toInstance(Clock.systemUTC());

                binder.install(new ConfigModule(config));
                binder.install(getLifecycleModule());

                binder.install(new JmxModule());

                binder.install(new JvmPauseAlarmModule());
            }
        };
    }

    /**
     * @return the configuration this server uses
     */
    protected final Config getConfig()
    {
        return config;
    }

    public final Injector getInjector()
    {
        if (injector != null) {
            return injector;
        }

        // Initialize Guice off the main module. Add a tiny
        // bit of special sauce to ensure explicit bindings.

        injector = Guice.createInjector(
            Stage.PRODUCTION,
            getPlumbingModules(),
            getMainModule(),
            getServerTemplateModule(),

            new Module() {
                @Override
                public void configure(final Binder binder) {
                    binder.requireExplicitBindings();
                    binder.disableCircularProxies();
                }
            });

        return injector;
    }

    protected LifecycleStage getStartStage()
    {
        return LifecycleStage.START_STAGE;
    }

    protected LifecycleStage getStopStage()
    {
        return LifecycleStage.STOP_STAGE;
    }

    protected Module getLifecycleModule()
    {
        return new LifecycleModule();
    }

    protected String getServerToken()
    {
        return serverToken;
    }

    public final int getPort() throws IOException
    {
        if (portNumberProvider == null) {
            throw new IllegalStateException("You need to bind a PortNumberProvider for getPort to work");
        }
        return portNumberProvider.getPort();
    }

    private void fallbackTerminate()
    {
        if (config.getConfiguration().getBoolean("ot.fallback-terminate", false)) {
            JvmFallbackShutdown.fallbackTerminate(Duration.ofSeconds(10));
        }
    }
}
