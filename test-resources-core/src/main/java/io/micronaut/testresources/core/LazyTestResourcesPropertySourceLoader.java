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
package io.micronaut.testresources.core;

import io.micronaut.context.env.ActiveEnvironment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.value.PropertyResolver;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A property source loader which works in conjunction with the {@link LazyTestResourcesExpressionResolver}
 * in order to resolve properties lazily.
 */
public class LazyTestResourcesPropertySourceLoader implements PropertySourceLoader {
    private final PropertyExpressionProducer producer;

    public LazyTestResourcesPropertySourceLoader(PropertyExpressionProducer producer) {
        this.producer = producer;
    }

    @Override
    public Optional<PropertySource> load(String resourceName, ResourceLoader resourceLoader) {
        return Optional.of(new LazyPropertySource(resourceLoader));
    }

    @Override
    public Optional<PropertySource> loadEnv(String resourceName, ResourceLoader resourceLoader, ActiveEnvironment activeEnvironment) {
        return load(resourceName, resourceLoader);
    }

    @Override
    public Map<String, Object> read(String name, InputStream input) {
        return Collections.emptyMap();
    }

    private final class LazyPropertySource implements PropertySource, Ordered {
        private final ResourceLoader resourceLoader;

        private List<String> keys;

        private LazyPropertySource(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        @Override
        public int getOrder() {
            return LOWEST_PRECEDENCE;
        }

        @Override
        public String getName() {
            return "test resources";
        }

        @Override
        public Object get(String key) {
            return "${" + LazyTestResourcesExpressionResolver.PLACEHOLDER_PREFIX + key + "}";
        }

        @Override
        public Iterator<String> iterator() {
            computeKeys();
            return keys.iterator();
        }

        private void computeKeys() {
            if (keys == null) {
                Map<String, Object> testResourcesConfig = null;
                if (resourceLoader instanceof PropertyResolver) {
                    PropertyResolver propertyResolver = (PropertyResolver) resourceLoader;
                    Map<String, Collection<String>> entries = producer.getPropertyEntries()
                        .stream()
                        .collect(Collectors.toMap(k -> k, propertyResolver::getPropertyEntries));
                    testResourcesConfig = propertyResolver.getProperties(TestResourcesResolver.TEST_RESOURCES_PROPERTY);
                    keys = producer.produceKeys(resourceLoader, entries, testResourcesConfig)
                        .stream()
                        // We use "containsProperties" here because "containsProperty"
                        // has a caching side effect which we don't want!
                        .filter(key -> !propertyResolver.containsProperties(key))
                        .collect(Collectors.toList());
                } else {
                    keys = producer.produceKeys(resourceLoader, Collections.emptyMap(), testResourcesConfig);
                }
            }
        }

    }

    private static class NoOpPropertySource implements PropertySource {
        private static final PropertySource INSTANCE = new NoOpPropertySource();

        @Override
        public String getName() {
            return "no-op";
        }

        @Override
        public Object get(String key) {
            return null;
        }

        @Override
        public Iterator<String> iterator() {
            return Collections.emptyIterator();
        }
    }
}
