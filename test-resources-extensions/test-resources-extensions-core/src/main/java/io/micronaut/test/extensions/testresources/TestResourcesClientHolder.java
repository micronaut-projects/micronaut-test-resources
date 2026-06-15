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
package io.micronaut.test.extensions.testresources;

import io.micronaut.core.annotation.Internal;
import io.micronaut.testresources.client.TestResourcesClient;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * An internal class which can be used to inject a fake
 * test resources client, for testing purposes.
 */
@Internal
public final class TestResourcesClientHolder {
    private static @Nullable TestResourcesClient client;

    private TestResourcesClientHolder() {

    }

    public static void set(@Nullable TestResourcesClient client) {
        TestResourcesClientHolder.client = client;
    }

    public static @Nullable TestResourcesClient get() {
        return client;
    }

    public static TestResourcesClient lazy() {
        return new LazyTestResourcesClient();
    }

    private static final class LazyTestResourcesClient implements TestResourcesClient {

        private static <T> T nullSafe(Function<TestResourcesClient, T> value, T defaultValue) {
            TestResourcesClient current = client;
            if (current == null) {
                return defaultValue;
            }
            return value.apply(current);
        }

        @Override
        public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            return nullSafe(TestResourcesClient::getResolvableProperties, List.of());
        }

        @Override
        public Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
            return nullSafe(client -> client.resolve(name, properties, testResourcesConfig), Optional.empty());
        }

        @Override
        public List<String> getRequiredProperties(String expression) {
            return nullSafe(client -> client.getRequiredProperties(expression), List.of());
        }

        @Override
        public List<String> getRequiredPropertyEntries() {
            return nullSafe(TestResourcesClient::getRequiredPropertyEntries, List.of());
        }

        @Override
        public boolean closeAll() {
            return nullSafe(TestResourcesClient::closeAll, true);
        }

        @Override
        public boolean closeScope(@Nullable String id) {
            return nullSafe(client -> client.closeScope(id), true);
        }

        @Override
        public List<String> getResolvableProperties() {
            return nullSafe(TestResourcesClient::getResolvableProperties, List.of());
        }

    }
}
