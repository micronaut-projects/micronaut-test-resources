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

import io.micronaut.context.env.PropertyExpressionResolver;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.value.PropertyResolver;
import io.micronaut.testresources.core.LazyTestResourcesExpressionResolver;
import io.micronaut.testresources.core.TestResourcesResolver;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EmbeddedTestResourcesPropertyExpressionResolver extends LazyTestResourcesExpressionResolver {
    public EmbeddedTestResourcesPropertyExpressionResolver() {
        super(new PropertyExpressionResolver() {
            private final TestResourcesResolverLoader loader = TestResourcesResolverLoader.getInstance();

            @Override
            public <T> Optional<T> resolve(PropertyResolver propertyResolver, ConversionService<?> conversionService, String expression, Class<T> requiredType) {
                List<TestResourcesResolver> resolvers = loader.getResolvers();
                for (TestResourcesResolver resolver : resolvers) {
                    Set<String> supported = new HashSet<>(resolver.listProperties());
                    if (supported.contains(expression)) {
                        Optional<String> resolve = resolver.resolve(expression);
                        if (resolve.isPresent()) {
                            return conversionService.convert(resolve.get(), requiredType);
                        }
                    }
                }
                return Optional.empty();
            }
        });
    }
}
