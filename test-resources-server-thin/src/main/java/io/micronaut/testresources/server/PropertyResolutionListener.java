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
package io.micronaut.testresources.server;

import io.micronaut.testresources.core.TestResourcesResolver;

import java.util.Map;

/**
 * A test resources property resolution listener will be notified
 * whenever a property is resolved by a {@link TestResourcesResolver}.
 */
public interface PropertyResolutionListener {
    /**
     * Records that a property was resolved by a particular resolver.
     * @param property the property which was resolved
     * @param resolvedValue the result of the resolution
     * @param resolver the resolver which resolved the property
     * @param properties the properties used for resolution
     * @param testResourcesConfig the test resources configuration
     */
    void resolved(String property,
                  String resolvedValue,
                  TestResourcesResolver resolver,
                  Map<String, Object> properties,
                  Map<String, Object> testResourcesConfig);

    /**
     * Records an error which happened during property resolution,
     * for example if a container fails to start.
     * @param property the property which we attempted to resolve
     * @param resolver the resolver which failed
     * @param error the error which happened
     */
    void errored(String property,
                 TestResourcesResolver resolver,
                 Throwable error);
}
