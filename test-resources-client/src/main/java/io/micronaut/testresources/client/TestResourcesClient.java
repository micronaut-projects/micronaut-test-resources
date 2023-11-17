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
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.testresources.core.TestResourcesResolver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A client responsible for connecting to a test resources
 * server.
 */
public interface TestResourcesClient extends TestResourcesResolver {
    String SERVER_URI = "server.uri";
    String ACCESS_TOKEN = "server.access.token";
    String CLIENT_READ_TIMEOUT = "server.client.read.timeout";

    @Get("/list")
    default List<String> getResolvableProperties() {
        return getResolvableProperties(Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    @Post("/list")
    List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig);

    @Override
    @Post("/resolve")
    Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig);

    @Override
    @Get("/requirements/expr/{expression}")
    List<String> getRequiredProperties(String expression);

    @Override
    @Get("/requirements/entries")
    List<String> getRequiredPropertyEntries();

    /**
     * Closes all test resources.
     * @return true if the operation was successful
     */
    @Get("/close/all")
    boolean closeAll();

    /**
     * Closes a test resource scope
     * @param id the scope id
     * @return true if the operation was successful
     */
    @Get("/close/{id}")
    boolean closeScope(@Nullable String id);
}
