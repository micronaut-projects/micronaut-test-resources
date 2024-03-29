/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.client;

import io.micronaut.context.ApplicationContext;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

/**
 * An application component which sends periodic keep alive requests to the server.
 */
@Singleton
public class TestResourcesKeepAlive {
    /**
     * Sends periodic keep alive requests to the server.
     * @param applicationContext the application context
     */
    @Scheduled(fixedRate = "1m")
    public void keepAlive(ApplicationContext applicationContext) {
        var client = TestResourcesClientFactory.extractFrom(applicationContext);
        if (client != null) {
            // Call any method to keep the service alive
            client.getResolvableProperties();
        }
    }
}
