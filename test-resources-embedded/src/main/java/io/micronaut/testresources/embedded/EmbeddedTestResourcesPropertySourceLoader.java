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
import io.micronaut.testresources.core.TestResourcesResolver;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EmbeddedTestResourcesPropertySourceLoader extends LazyTestResourcesPropertySourceLoader {
    public EmbeddedTestResourcesPropertySourceLoader() {
        super(new Function<ResourceLoader, List<String>>() {
            private final TestResourcesResolverLoader loader = TestResourcesResolverLoader.getInstance();

            @Override
            public List<String> apply(ResourceLoader resourceLoader) {
                List<TestResourcesResolver> resolvers = loader.getResolvers();
                return resolvers.stream()
                    .flatMap(r -> r.listProperties().stream())
                    .distinct()
                    .collect(Collectors.toList());
            }
        });
    }
}
