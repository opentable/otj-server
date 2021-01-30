/*
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

import org.junit.Assert;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.opentable.service.ServiceInfo;

// Not a test per, but can be adapted to show the jvm hook kicks in. This is helpful for closed context,
// but not terribly reliable for ApplicationFailed, since the event is only sent if we registered in time
// Currently doesn't work with AppFailed, so I modified it to emulate a contextclosed forced shutdown
public class StartupFailedDemo {
    public static void main(String[] args) throws InterruptedException {
        try {
            ConfigurableApplicationContext context = SpringApplication.run(new Class[] {App.class}, args);
            context.close();
        } catch (@SuppressWarnings("unused") BeanCreationException e) {
            Assert.fail(); // this catch really only applies for appfailedevent
        }
        // No problem... wait for the T-1000.
        Thread.sleep(StartupShutdownFailedHandler.timeout.toMillis() * 2);
        throw new RuntimeException("should never be reached, since hook should prevent it");
    }

    @Configuration
    @JAXRSServer
    public static class App {
        @Configuration
        public static class BustedConfiguration {

            @Bean
            public ServiceInfo serviceInfo() {
                return () -> "bye";
            }

        }
    }
}
