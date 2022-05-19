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

import io.micronaut.context.env.PropertyExpressionResolver;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.value.PropertyResolver;

import java.util.Optional;

/**
 * A property expression resolver which delegates to another
 * resolver if the property expression to be resolved starts
 * with the expected prefix. The delegate is called with the
 * prefix removed.
 */
public class LazyTestResourcesExpressionResolver implements PropertyExpressionResolver {
    public static final String PLACEHOLDER_PREFIX = "auto.test.resources.";
    private final PropertyExpressionResolver delegate;

    public LazyTestResourcesExpressionResolver(PropertyExpressionResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> Optional<T> resolve(PropertyResolver propertyResolver, ConversionService<?> conversionService, String expression, Class<T> requiredType) {
        if (expression.startsWith(PLACEHOLDER_PREFIX)) {
            return delegate.resolve(propertyResolver, conversionService, expression.substring(PLACEHOLDER_PREFIX.length()), requiredType);
        }
        return Optional.empty();
    }
}
