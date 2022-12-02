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

import io.micronaut.core.io.ResourceLoader;
import io.micronaut.testresources.core.LazyTestResourcesPropertySourceLoader;
import io.micronaut.testresources.core.PropertyExpressionProducer;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import static io.micronaut.testresources.client.ConfigFinder.findConfiguration;

/**
 * A property source loader which delegates resolution of properties to the client
 * lazily.
 */
public class TestResourcesClientPropertySourceLoader extends LazyTestResourcesPropertySourceLoader {

    public TestResourcesClientPropertySourceLoader() {
        super(new ClientTestResourcesResolver());
    }

    public final Optional<TestResourcesClient> getClient() {
        return Optional.ofNullable(((ClientTestResourcesResolver) getProducer()).client);
    }

    private static class ClientTestResourcesResolver implements PropertyExpressionProducer {
        private final ReentrantLock lock = new ReentrantLock();
        private TestResourcesClient client;

        @Override
        public List<String> getPropertyEntries() {
            return findClient(null)
                .map(TestResourcesClient::getRequiredPropertyEntries)
                .orElse(Collections.emptyList());
        }

        @Override
        public List<String> produceKeys(ResourceLoader resourceLoader, Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            return findClient(resourceLoader)
                .map(client -> client.getResolvableProperties(propertyEntries, testResourcesConfig))
                .orElse(Collections.emptyList());
        }

        private Optional<TestResourcesClient> findClient(ResourceLoader resourceLoader) {
            lock.lock();
            try {
                if (client == null) {
                    Optional<URL> config = findConfiguration(resourceLoader);
                    client = config.map(TestResourcesClientFactory::configuredAt)
                        .orElseGet(() -> TestResourcesClientFactory.fromSystemProperties().orElse(null));
                }
                return Optional.ofNullable(client);
            } finally {
                lock.unlock();
            }
        }
    }
}
