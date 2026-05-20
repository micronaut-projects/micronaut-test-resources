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
package io.micronaut.testresources.compose;

/**
 * Labels understood by Compose-aware providers.
 */
public final class ComposeLabels {
    public static final String SERVICE = "io.micronaut.test-resources.service";
    public static final String DATASOURCE = "io.micronaut.test-resources.datasource";
    public static final String IGNORE = "io.micronaut.test-resources.ignore";
    public static final String USERNAME = "io.micronaut.test-resources.username";
    public static final String PASSWORD = "io.micronaut.test-resources.password";
    public static final String DATABASE = "io.micronaut.test-resources.database";
    public static final String ACCESS_KEY = "io.micronaut.test-resources.access-key";
    public static final String SECRET_KEY = "io.micronaut.test-resources.secret-key";
    public static final String TOKEN = "io.micronaut.test-resources.token";
    public static final String REALM = "io.micronaut.test-resources.realm";
    public static final String CLIENT_ID = "io.micronaut.test-resources.client-id";
    public static final String CLIENT_SECRET = "io.micronaut.test-resources.client-secret";

    private ComposeLabels() {
    }
}
