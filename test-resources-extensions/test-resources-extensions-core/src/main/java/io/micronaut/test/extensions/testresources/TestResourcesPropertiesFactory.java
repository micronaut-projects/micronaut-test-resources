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

import io.micronaut.test.extensions.testresources.annotation.TestResourcesProperties;
import io.micronaut.test.support.TestPropertyProvider;
import io.micronaut.test.support.TestPropertyProviderFactory;
import io.micronaut.testresources.client.TestResourcesClientFactory;
import io.micronaut.testresources.codec.Result;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestResourcesPropertiesFactory implements TestPropertyProviderFactory {
    @Override
    public TestPropertyProvider create(Map<String, Object> properties, Class<?> testClass) {
        return new TestResourcesTestPropertyProvider(testClass, properties);

    }

    private static TestResourcesPropertyProvider instantitateProvider(Class<? extends TestResourcesPropertyProvider> provider) {
        try {
            return provider.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Test resources property provider must have a public constructor without arguments", e);
        }
    }

    private static class TestResourcesTestPropertyProvider implements TestPropertyProvider {
        public static final String TEST_RESOURCES_PROPERTY_PREFIX = "test-resources.";
        private final Class<?> testClass;
        private final Map<String, Object> properties;

        public TestResourcesTestPropertyProvider(Class<?> testClass, Map<String, Object> properties) {
            this.testClass = testClass;
            this.properties = properties;
        }

        @Override
        public Map<String, String> getProperties() {
            TestResourcesProperties annotation = testClass.getAnnotation(TestResourcesProperties.class);
            if (annotation != null) {
                String[] requestedProperties = annotation.value();
                var client = TestResourcesClientFactory.fromSystemProperties()
                    .orElse(TestResourcesClientHolder.lazy());
                var testResourcesConfig = properties.entrySet()
                    .stream()
                    .filter(e -> e.getKey().startsWith(TEST_RESOURCES_PROPERTY_PREFIX))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                Map<String, String> resolvedProperties = Stream.of(requestedProperties)
                    .map(v -> new Object() {
                        private final String key = v;
                        private final String value = client.resolve(v, Map.of(), testResourcesConfig).map(Result::value).orElse(null);
                    })
                    .filter(o -> o.value != null)
                    .collect(Collectors.toMap(e -> e.key, e -> e.value));

                // Result represents what properties we're going to expose to tests
                Map<String, String> result = new HashMap<>(resolvedProperties);
                // Context represents what is available to resolvers for them to
                // compute results
                Map<String, Object> context = new HashMap<>(properties);
                context.putAll(resolvedProperties);
                result.putAll(resolvedProperties);
                Class<? extends TestResourcesPropertyProvider>[] providers = annotation.providers();
                for (Class<? extends TestResourcesPropertyProvider> provider : providers) {
                    var testResourcesPropertyProvider = instantitateProvider(provider);
                    Map<String, String> map = testResourcesPropertyProvider.provide(Collections.unmodifiableMap(context));
                    context.putAll(map);
                    result.putAll(map);
                }
                return result;
            }
            return Map.of();
        }
    }
}
