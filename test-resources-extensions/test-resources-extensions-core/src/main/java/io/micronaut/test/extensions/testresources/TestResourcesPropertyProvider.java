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
package io.micronaut.test.extensions.testresources;

import java.util.Map;

/**
 * A test resources property provider is a type which
 * must be explicitly declared in via the {@link io.micronaut.test.extensions.testresources.annotation.TestResourcesProperties}
 * annotation.
 * <p/>
 * It is responsible for supplying additional test properties,
 * given the set of properties which are available before the
 * application context is started.
 * <p/>
 * It can be used, in particular, to derive new properties
 * from other properties resolved by the test resources client.
 * <p/>
 * This works in a very similar way as {@link io.micronaut.test.support.TestPropertyProvider},
 * but has access to other properties in order to perform
 * computation based on the value of these properties.
 */
@FunctionalInterface
public interface TestResourcesPropertyProvider {
    /**
     * Returns a map of properties which need to be exposed
     * to the application context, given the map of properties
     * which are already available during setup.
     *
     * These properties typically include the properties
     * visible in the configuration files which do not require
     * access to test resources.
     *
     * @param testProperties the set of properties available
     * @return a map of properties to be added
     */
    Map<String, String> provide(Map<String, Object> testProperties);
}
