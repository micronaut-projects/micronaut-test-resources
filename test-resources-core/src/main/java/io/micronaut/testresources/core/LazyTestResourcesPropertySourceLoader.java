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

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * A property source loader which works in conjunction with the {@link LazyTestResourcesExpressionResolver}
 * in order to resolve properties lazily.
 */
public class LazyTestResourcesPropertySourceLoader implements PropertySourceLoader {
    private final Function<ResourceLoader, List<String>> producer;

    public LazyTestResourcesPropertySourceLoader(Function<ResourceLoader, List<String>> producer) {
        this.producer = producer;
    }

    @Override
    public Optional<PropertySource> load(String resourceName, ResourceLoader resourceLoader) {
        List<String> keys = producer.apply(resourceLoader);
        if (keys.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new LazyPropertySource(keys));
    }

    @Override
    public Optional<PropertySource> loadEnv(String resourceName, ResourceLoader resourceLoader, ActiveEnvironment activeEnvironment) {
        return load(resourceName, resourceLoader);
    }

    @Override
    public Map<String, Object> read(String name, InputStream input) {
        return Collections.emptyMap();
    }

    private static class LazyPropertySource implements PropertySource, Ordered {
        private final List<String> keys;

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE;
        }

        private LazyPropertySource(List<String> keys) {
            this.keys = keys;
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
            return keys.iterator();
        }
    }
}
