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
package io.micronaut.testresources.embedded;

import io.micronaut.core.io.ResourceLoader;
import io.micronaut.testresources.core.LazyTestResourcesPropertySourceLoader;
import io.micronaut.testresources.core.PropertyExpressionProducer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A property source loader responsible for resolving test resources.
 * This delegates to test resources resolver loaded via service loading.
 */
public class EmbeddedTestResourcesPropertySourceLoader extends LazyTestResourcesPropertySourceLoader {
    public EmbeddedTestResourcesPropertySourceLoader() {
        super(new EmbeddedTestResourcesProducer());
    }

    private static class EmbeddedTestResourcesProducer implements PropertyExpressionProducer {
        private final TestResourcesResolverLoader loader = TestResourcesResolverLoader.getInstance();

        @Override
        public List<String> getPropertyEntries() {
            return loader.getResolvers()
                .stream()
                .flatMap(resolver -> resolver.getRequiredPropertyEntries().stream())
                .distinct()
                .collect(Collectors.toList());
        }

        @Override
        public List<String> produceKeys(ResourceLoader resourceLoader, Map<String, Collection<String>> propertyEntries) {
            return loader.getResolvers()
                .stream()
                .flatMap(r -> r.getResolvableProperties(propertyEntries).stream())
                .distinct()
                .collect(Collectors.toList());
        }
    }
}
