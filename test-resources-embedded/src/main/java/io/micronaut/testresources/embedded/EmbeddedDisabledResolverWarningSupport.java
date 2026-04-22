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
package io.micronaut.testresources.embedded;

import io.micronaut.testresources.core.TestResourcesResolver;
import io.micronaut.testresources.core.ToggableTestResourcesResolver;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class EmbeddedDisabledResolverWarningSupport {
    private static final Set<String> WARNED_DISABLED_RESOLVERS = ConcurrentHashMap.newKeySet();

    private EmbeddedDisabledResolverWarningSupport() {
    }

    static boolean isEnabled(TestResourcesResolver resolver,
                             Map<String, Object> testResourcesConfig,
                             Logger logger) {
        if (resolver instanceof ToggableTestResourcesResolver toggable) {
            if (toggable.isEnabled(testResourcesConfig)) {
                return true;
            }
            if (WARNED_DISABLED_RESOLVERS.add(toggable.getName())) {
                logger.warn("Test resources provider for {} is disabled", toggable.getDisplayName());
            }
            return false;
        }
        return true;
    }

    static void resetWarnings() {
        WARNED_DISABLED_RESOLVERS.clear();
    }
}
