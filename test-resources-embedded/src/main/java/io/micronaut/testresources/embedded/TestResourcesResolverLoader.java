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

import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.testresources.core.TestResourcesResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is responsible for loading {@link TestResourcesResolver} instances
 * via service loading and caching them.
 */
public final class TestResourcesResolverLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestResourcesResolverLoader.class);

    private final List<TestResourcesResolver> resolvers;

    public TestResourcesResolverLoader() {
        SoftServiceLoader<TestResourcesResolver> loader = SoftServiceLoader.load(TestResourcesResolver.class);
        ArrayList<TestResourcesResolver> values = new ArrayList<>();
        loader.collectAll(values);
        resolvers = OrderUtil.sort(values.stream()).collect(Collectors.toList());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Loaded {} test resources resolvers: {}", resolvers.size(), resolvers.stream().map(Object::getClass).map(Class::getName).collect(Collectors.joining(", ")));
        }
    }

    public List<TestResourcesResolver> getResolvers() {
        return resolvers;
    }

    static TestResourcesResolverLoader getInstance() {
        return Initializer.INSTANCE;
    }

    private static class Initializer {
        private static final TestResourcesResolverLoader INSTANCE = new TestResourcesResolverLoader();
    }
}
