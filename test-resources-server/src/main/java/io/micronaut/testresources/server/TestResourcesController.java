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
package io.micronaut.testresources.server;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.testresources.codec.Result;
import io.micronaut.testresources.codec.TestResourcesMediaType;
import io.micronaut.testresources.core.TestResourcesResolver;
import io.micronaut.testresources.embedded.TestResourcesResolverLoader;
import io.micronaut.testresources.testcontainers.TestContainers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A client responsible for connecting to a test resources
 * server.
 */
@Controller("/")
@Produces(TestResourcesMediaType.TEST_RESOURCES_BINARY)
@Consumes(TestResourcesMediaType.TEST_RESOURCES_BINARY)
public final class TestResourcesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestResourcesController.class);

    private final TestResourcesResolverLoader loader = new TestResourcesResolverLoader();

    @Get("/list")
    public Result<List<String>> getResolvableProperties() {
        return getResolvableProperties(Collections.emptyMap(), Collections.emptyMap());
    }

    @Post("/list")
    public Result<List<String>> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return Result.of(loader.getResolvers()
            .stream()
            .map(r -> r.getResolvableProperties(propertyEntries, testResourcesConfig))
            .flatMap(Collection::stream)
            .distinct()
            .peek(p -> LOGGER.debug("For configuration {} and property entries {} , resolvable property: {}", testResourcesConfig, propertyEntries, p))
            .toList());
    }

    @Get("/requirements/expr/{expression}")
    public Result<List<String>> getRequiredProperties(String expression) {
        return Result.of(loader.getResolvers()
            .stream()
            .map(testResourcesResolver -> testResourcesResolver.getRequiredProperties(expression))
            .flatMap(Collection::stream)
            .distinct()
            .toList());
    }

    @Get("/requirements/entries")
    public Result<List<String>> getRequiredPropertyEntries() {
        return Result.of(loader.getResolvers()
            .stream()
            .map(TestResourcesResolver::getRequiredPropertyEntries)
            .flatMap(Collection::stream)
            .distinct()
            .toList());
    }

    @Post("/resolve")
    public Optional<Result<String>> resolve(String name,
                                            Map<String, Object> properties,
                                            Map<String, Object> testResourcesConfig) {
        for (TestResourcesResolver resolver : loader.getResolvers()) {
            Optional<String> result = resolver.resolve(name, properties, testResourcesConfig);
            LOGGER.debug("Attempt to resolve {} with resolver {}, properties {} and test resources configuration {} : {}", name, resolver.getClass(), properties, testResourcesConfig, result.isPresent() ? result.get() : "\uD83D\uDEAB");
            if (result.isPresent()) {
                return Result.asOptional(result.get());
            }
        }
        return Optional.empty();
    }

    @Get("/close/all")
    public Result<Boolean> closeAll() {
        LOGGER.debug("Closing all test resources");
        return Result.of(TestContainers.closeAll());
    }

    @Get("/close/{id}")
    public Result<Boolean> closeScope(@Nullable String id) {
        LOGGER.info("Closing test resources of scope {}", id);
        return Result.of(TestContainers.closeScope(id));
    }

    @Get("/testcontainers")
    public Result<List<Map<String, String>>> listContainers() {
        return listContainersByScope(null);
    }

    @Get("/testcontainers/{scope}")
    public Result<List<Map<String, String>>> listContainersByScope(@Nullable String scope) {
        return Result.of(TestContainers.listByScope(scope)
            .entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(c -> new TestContainer(
                    c.getContainerName(),
                    c.getDockerImageName(),
                    c.getContainerId(),
                    entry.getKey().toString()).asMap()
                ))
            .toList());
    }

}
