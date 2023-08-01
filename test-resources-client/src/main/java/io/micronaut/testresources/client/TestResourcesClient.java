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
package io.micronaut.testresources.client;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.testresources.codec.Result;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A client responsible for connecting to a test resources
 * server.
 */
public interface TestResourcesClient {
    String SERVER_URI = "server.uri";
    String ACCESS_TOKEN = "server.access.token";
    String CLIENT_READ_TIMEOUT = "server.client.read.timeout";

    default Result<List<String>> getResolvableProperties() {
        return getResolvableProperties(Collections.emptyMap(), Collections.emptyMap());
    }

    Result<List<String>> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig);

    Optional<Result<String>> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig);

    Result<List<String>> getRequiredProperties(String expression);

    Result<List<String>> getRequiredPropertyEntries();

    /**
     * Closes all test resources.
     */
    Result<Boolean> closeAll();

    Result<Boolean> closeScope(@Nullable String id);
}
