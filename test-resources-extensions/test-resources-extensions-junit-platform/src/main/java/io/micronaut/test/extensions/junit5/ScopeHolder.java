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
package io.micronaut.test.extensions.junit5;

import io.micronaut.core.annotation.Internal;

import java.util.Optional;

/**
 * Our JUnit listener needs to communicate with the test resources
 * provider factory. Because they are both instantiated with
 * service loading, we are using a thread local to share information.
 */
@Internal
public final class ScopeHolder {
    private static final ThreadLocal<String> CURRENT_SCOPE = ThreadLocal.withInitial(() -> null);

    private ScopeHolder() {
    }

    public static Optional<String> get() {
        return Optional.ofNullable(CURRENT_SCOPE.get());
    }

    public static void set(String scope) {
        CURRENT_SCOPE.set(scope);
    }

    public static void remove() {
        CURRENT_SCOPE.remove();
    }
}
