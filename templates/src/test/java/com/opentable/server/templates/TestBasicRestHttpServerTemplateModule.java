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
package com.opentable.server.templates;

import com.google.inject.Guice;
import com.google.inject.Stage;

import org.junit.Test;
import org.weakref.jmx.testing.TestingMBeanModule;

import com.opentable.config.Config;
import com.opentable.config.ConfigModule;
import com.opentable.lifecycle.guice.LifecycleModule;

public class TestBasicRestHttpServerTemplateModule
{
    @Test
    public void testSimple()
    {
        final Config config = Config.getEmptyConfig();

        Guice.createInjector(Stage.PRODUCTION,
                             new TestingMBeanModule(),
                             new LifecycleModule(),
                             new ConfigModule(config),
                             new BasicRestHttpServerTemplateModule(config));
    }
}
