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
import io.micronaut.testresources.codec.Result;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An internal class which can be used to inject a fake
 * test resources client, for testing purposes.
 */
@Internal
public final class TestResourcesClientHolder {
    private static TestResourcesClient CLIENT;

    private TestResourcesClientHolder() {

    }

    public static void set(TestResourcesClient client) {
        CLIENT = client;
    }

    public static TestResourcesClient get() {
        return CLIENT;
    }

    public static TestResourcesClient lazy() {
        return new LazyTestResourcesClient();
    }

    private static class LazyTestResourcesClient implements TestResourcesClient {

        private static <T> T nullSafe(Supplier<T> value) {
            if (CLIENT == null) {
                return null;
            }
            return value.get();
        }

        @Override
        public Result<List<String>> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            return nullSafe(CLIENT::getResolvableProperties);
        }

        @Override
        public Optional<Result<String>> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
            return nullSafe(() -> CLIENT.resolve(name, properties, testResourcesConfig));
        }

        @Override
        public Result<List<String>> getRequiredProperties(String expression) {
            return nullSafe(() -> CLIENT.getRequiredProperties(expression));
        }

        @Override
        public Result<List<String>> getRequiredPropertyEntries() {
            return nullSafe(CLIENT::getRequiredPropertyEntries);
        }

        @Override
        public Result<Boolean> closeAll() {
            return nullSafe(CLIENT::closeAll);
        }

        @Override
        public Result<Boolean> closeScope(String id) {
            return nullSafe(() -> CLIENT.closeScope(id));
        }

        @Override
        public Result<List<String>> getResolvableProperties() {
            return nullSafe(CLIENT::getResolvableProperties);
        }

    }
}
