/*
 * Copyright 2003-2021 the original author or authors.
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
package io.micronaut.testresources.embedded;

import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.testresources.core.TestResourcesResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class TestResourcesResolverLoader {

    static TestResourcesResolverLoader getInstance() {
        return Initializer.INSTANCE;
    }

    private final List<TestResourcesResolver> resolvers;

    private TestResourcesResolverLoader() {
        SoftServiceLoader<TestResourcesResolver> loader = SoftServiceLoader.load(TestResourcesResolver.class);
        ArrayList<TestResourcesResolver> values = new ArrayList<>();
        loader.collectAll(values);
        resolvers = Collections.unmodifiableList(values);
    }

    public List<TestResourcesResolver> getResolvers() {
        return resolvers;
    }

    private static class Initializer {
        private static final TestResourcesResolverLoader INSTANCE = new TestResourcesResolverLoader();
    }
}
