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
 * Shared default Docker images used by multiple test resource providers.
 */
@Internal
public final class DefaultTestResourceImages {
    public static final String DEFAULT_MARIADB_IMAGE = "mariadb:11.8.6";
    public static final String DEFAULT_MYSQL_IMAGE = "mysql:8.4.8";
    public static final String DEFAULT_POSTGRES_IMAGE = "postgres:18.3";

    private DefaultTestResourceImages() {
    }
}
