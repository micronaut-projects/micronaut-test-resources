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
package io.micronaut.testresources.aws;

import io.micronaut.core.annotation.Internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base implementation for AWS emulator services which expose one endpoint override.
 * @param <C> the container type
 */
@Internal
public abstract class AbstractAwsEndpointService<C> implements AwsEmulatorService<C> {

    private final String serviceKind;
    private final String endpointProperty;
    private final Function<C, String> endpointResolver;

    protected AbstractAwsEndpointService(String serviceKind, String endpointProperty, Function<C, String> endpointResolver) {
        this.serviceKind = serviceKind;
        this.endpointProperty = endpointProperty;
        this.endpointResolver = endpointResolver;
    }

    @Override
    public Optional<String> resolveProperty(String propertyName, C container) {
        if (endpointProperty.equals(propertyName)) {
            return Optional.of(endpointResolver.apply(container));
        }
        return Optional.empty();
    }

    @Override
    public String getServiceKind() {
        return serviceKind;
    }

    @Override
    public List<String> getResolvableProperties() {
        return Collections.singletonList(endpointProperty);
    }
}
