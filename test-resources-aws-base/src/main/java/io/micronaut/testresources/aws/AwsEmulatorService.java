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

import java.util.List;
import java.util.Optional;

/**
 * Interface for AWS emulator service loading.
 * @param <C> the container type
 */
@Internal
public interface AwsEmulatorService<C> {
    /**
     * Returns the service kind.
     * @return the service kind.
     */
    String getServiceKind();

    /**
     * Returns the list of properties that this service can configure.
     * @return the list of supported properties
     */
    List<String> getResolvableProperties();

    /**
     * Resolves a property.
     * @param propertyName the property to resolve
     * @param container the AWS emulator container
     * @return the resolved property, if available
     */
    Optional<String> resolveProperty(String propertyName, C container);
}
