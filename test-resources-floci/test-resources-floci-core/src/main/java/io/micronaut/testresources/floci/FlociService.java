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
package io.micronaut.testresources.floci;

import io.floci.testcontainers.FlociContainer;

import java.util.List;
import java.util.Optional;

/**
 * Interface for Floci service loading.
 */
public interface FlociService {
    /**
     * Returns the service kind.
     * @return the service kind.
     */
    String getServiceKind();

    /**
     * Returns the list of properties that this service
     * can configure.
     * @return the list of supported properties
     */
    List<String> getResolvableProperties();

    /**
     * Resolves a property.
     * @param propertyName the property to resolve
     * @param container the Floci container
     * @return the resolved property, if available
     */
    Optional<String> resolveProperty(String propertyName, FlociContainer container);
}
