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
import io.micronaut.testresources.core.TestResourcesResolver;
import io.micronaut.testresources.core.ToggableTestResourcesResolver;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A property source loader responsible for resolving test resources.
 * This delegates to test resources resolver loaded via service loading.
 */
public class EmbeddedTestResourcesPropertySourceLoader extends LazyTestResourcesPropertySourceLoader {
    private static final Pattern CAMEL_CASE = Pattern.compile("([a-z])([A-Z])");

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
        public List<String> produceKeys(ResourceLoader resourceLoader, Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            return loader.getResolvers()
                .stream()
                .filter(r -> isEnabled(r, testResourcesConfig))
                .flatMap(r -> r.getResolvableProperties(propertyEntries, testResourcesConfig)
                    .stream().map(key -> assertValidKey(key, r))
                ).distinct()
                .toList();
        }

        private static String assertValidKey(String key, TestResourcesResolver r) {
            Matcher m = CAMEL_CASE.matcher(key);
            if (m.find()) {
                throw new IllegalArgumentException("Test resources resolver [" + r.getClass().getName() + "] : Property key [" + key + "] is not valid. Property keys must be in kebab case.");
            }
            return key;
        }

        private static boolean isEnabled(TestResourcesResolver resolver, Map<String, Object> testResourcesConfig) {
            if (resolver instanceof ToggableTestResourcesResolver toggable) {
                return toggable.isEnabled(testResourcesConfig);
            }
            return true;
        }
    }
}
