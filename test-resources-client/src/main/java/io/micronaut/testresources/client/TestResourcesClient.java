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
 * proxy.
 */
public interface TestResourcesClient extends TestResourcesResolver {
    String PROXY_URI = "proxy.uri";
    String ACCESS_TOKEN = "proxy.access.token";
    String CLIENT_READ_TIMEOUT = "proxy.client.read.timeout";

    @Get("/list")
    default List<String> getResolvableProperties() {
        return getResolvableProperties(Collections.emptyMap());
    }

    @Override
    @Post("/list")
    List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries);

    @Override
    @Post("/resolve")
    Optional<String> resolve(String name, Map<String, Object> properties);

    @Override
    @Get("/requirements/expr/{expression}")
    List<String> getRequiredProperties(String expression);

    @Override
    @Get("/requirements/entries")
    List<String> getRequiredPropertyEntries();

    /**
     * Closes all test resources.
     */
    @Get("/close/all")
    void closeAll();

    @Get("/close/{id}")
    void closeScope(String id);
}
