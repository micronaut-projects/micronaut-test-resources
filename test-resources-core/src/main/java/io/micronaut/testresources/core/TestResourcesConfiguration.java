/*
 * Copyright 2017-2023 original authors
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

/**
 * Shared Test Resources configuration keys.
 */
public final class TestResourcesConfiguration {
    /**
     * System property used by an application runtime to disable Test Resources.
     */
    public static final String ENABLED = "micronaut.test.resources.enabled";

    private TestResourcesConfiguration() {
    }

    /**
     * @return {@code true} when Test Resources have been disabled for the current JVM.
     */
    public static boolean isDisabled() {
        return "false".equalsIgnoreCase(System.getProperty(ENABLED));
    }

    /**
     * @param propertyResolver The property resolver to inspect
     * @return {@code true} when Test Resources have been disabled for the current application.
     */
    public static boolean isDisabled(PropertyResolver propertyResolver) {
        return propertyResolver != null && propertyResolver.getProperty(ENABLED, Boolean.class).map(enabled -> !enabled).orElse(false)
            || isDisabled();
    }
}
