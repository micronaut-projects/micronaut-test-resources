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

import java.util.Map;

/**
 * Interface for test resource resolvers which may be enabled or disabled
 * on demand.
 */
public interface ToggableTestResourcesResolver extends TestResourcesResolver {
    String getName();

    default String getDisplayName() {
        return getName();
    }

    default boolean isEnabled(Map<String, Object> testResourcesConfig) {
        var o = testResourcesConfig.get(getName() + ".enabled");
        if (o == null) {
            return true;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(o));
    }
}
