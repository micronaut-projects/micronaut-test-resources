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

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Configuration properties for test resources.
 */
@SuppressWarnings("unused")
@ConfigurationProperties(TestResourcesResolver.TEST_RESOURCES_PROPERTY)
final class TestResourcesConfiguration {
    private boolean enabled = true;

    /**
     * Whether application test resources resolution is enabled.
     * @return true if application test resources resolution is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether application test resources resolution is enabled.
     * @param enabled true if application test resources resolution is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
