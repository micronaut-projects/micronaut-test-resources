/*
 * Copyright 2003-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.test.extensions.testresources;

import io.micronaut.testresources.client.TestResourcesClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestResourcesClientHolderTest {

    @AfterEach
    void cleanup() {
        TestResourcesClientHolder.set(null);
    }

    @Test
    void lazyClientReturnsDefaultsWhenNoClientIsInjected() {
        TestResourcesClientHolder.set(null);

        TestResourcesClient client = TestResourcesClientHolder.lazy();

        assertEquals(List.of(), client.getResolvableProperties());
        assertEquals(List.of(), client.getResolvableProperties(Map.of(), Map.of()));
        assertEquals(Optional.empty(), client.resolve("missing", Map.of(), Map.of()));
        assertEquals(List.of(), client.getRequiredProperties("missing"));
        assertEquals(List.of(), client.getRequiredPropertyEntries());
        assertTrue(client.closeAll());
        assertTrue(client.closeScope(null));
    }

    @Test
    void lazyClientDelegatesToInjectedClient() {
        TestResourcesClient injected = new DelegatingTestResourcesClient();
        TestResourcesClientHolder.set(injected);

        TestResourcesClient client = TestResourcesClientHolder.lazy();

        assertSame(injected, TestResourcesClientHolder.get());
        assertEquals(List.of("resolvable"), client.getResolvableProperties());
        assertEquals(List.of("resolvable"), client.getResolvableProperties(Map.of(), Map.of()));
        assertEquals(Optional.of("value"), client.resolve("name", Map.of(), Map.of()));
        assertEquals(List.of("required"), client.getRequiredProperties("expression"));
        assertEquals(List.of("required-entry"), client.getRequiredPropertyEntries());
        assertTrue(client.closeAll());
        assertTrue(client.closeScope("scope"));
    }

    private static final class DelegatingTestResourcesClient implements TestResourcesClient {
        @Override
        public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            return List.of("entry");
        }

        @Override
        public Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
            return Optional.of("value");
        }

        @Override
        public List<String> getRequiredProperties(String expression) {
            return List.of("required");
        }

        @Override
        public List<String> getRequiredPropertyEntries() {
            return List.of("required-entry");
        }

        @Override
        public boolean closeAll() {
            return true;
        }

        @Override
        public boolean closeScope(String id) {
            return true;
        }

        @Override
        public List<String> getResolvableProperties() {
            return List.of("resolvable");
        }
    }
}
