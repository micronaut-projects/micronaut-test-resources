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

import io.micronaut.core.value.PropertyResolver;

import java.util.Map;
import java.util.Optional;

final class TestResourcesConfiguration {
    private static final String MICRONAUT_TEST_RESOURCES_PROPERTY = "micronaut.test.resources";
    private static final String ENABLED = "enabled";

    private TestResourcesConfiguration() {
    }

    static boolean isEnabled(PropertyResolver propertyResolver) {
        return propertyResolver.getProperty(MICRONAUT_TEST_RESOURCES_PROPERTY + "." + ENABLED, Boolean.class)
            .or(() -> propertyResolver.getProperty(TestResourcesResolver.TEST_RESOURCES_PROPERTY + "." + ENABLED, Boolean.class))
            .or(() -> systemPropertyEnabled())
            .orElse(true);
    }

    static boolean isEnabled(Map<String, Object> testResourcesConfig) {
        Object enabled = testResourcesConfig.get(ENABLED);
        if (enabled == null) {
            return systemPropertyEnabled().orElse(true);
        }
        if (enabled instanceof Boolean enabledValue) {
            return enabledValue;
        }
        return Boolean.parseBoolean(String.valueOf(enabled));
    }

    private static Optional<Boolean> systemPropertyEnabled() {
        String enabled = System.getProperty(MICRONAUT_TEST_RESOURCES_PROPERTY + "." + ENABLED);
        if (enabled == null) {
            return Optional.empty();
        }
        return Optional.of(Boolean.parseBoolean(enabled));
    }
}
