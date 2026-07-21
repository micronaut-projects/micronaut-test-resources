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

import io.micronaut.core.annotation.Internal;

/**
 * Internal lifecycle hook for resources that follow Test Resources scopes but
 * are not represented by a {@code GenericContainer}.
 */
@Internal
public interface ScopedTestResourcesLifecycle {

    /**
     * Closes all resources owned by this lifecycle.
     *
     * @return true if any resource was closed
     */
    boolean closeAll();

    /**
     * Closes resources within the supplied scope.
     *
     * @param id The scope id
     * @return true if any resource was closed
     */
    boolean closeScope(String id);
}
