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

import io.micronaut.context.ApplicationContext;
import org.jspecify.annotations.Nullable;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.testresources.codec.Result;
import io.micronaut.testresources.codec.TestResourcesMediaType;
import io.micronaut.testresources.core.ResolverLoader;
import io.micronaut.testresources.core.TestResourcesResolutionException;
import io.micronaut.testresources.core.TestResourcesResolver;
import io.micronaut.testresources.core.ToggableTestResourcesResolver;
import io.micronaut.testresources.testcontainers.TestContainers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The main test resources controller, which will answer requests performed by the
 * test resources client to resolve properties or close test resources.
 */
@Controller("/")
@Produces(TestResourcesMediaType.TEST_RESOURCES_BINARY)
@ExecuteOn(TaskExecutors.BLOCKING)
@Ping
public class TestResourcesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestResourcesController.class);
    private static final int MAX_STOP_TIMEOUT = 5000;
    private static final String TEST_RESOURCES_PREFIX = "test-resources.";

    private final ResolverLoader loader;

    private final List<PropertyResolutionListener> propertyResolutionListeners;
    private final EmbeddedServer embeddedServer;
    private final ApplicationContext applicationContext;
    private final TaskScheduler taskScheduler;
    private final Set<String> warnedDisabledResolvers = ConcurrentHashMap.newKeySet();

    public TestResourcesController(List<PropertyResolutionListener> propertyResolutionListeners,
                                   EmbeddedServer embeddedServer,
                                   ApplicationContext applicationContext,
                                   ResolverLoader loader,
                                   TaskScheduler taskScheduler) {
        this.propertyResolutionListeners = propertyResolutionListeners;
        this.embeddedServer = embeddedServer;
        this.applicationContext = applicationContext;
        this.loader = loader;
        this.taskScheduler = taskScheduler;
    }

    /**
     * Lists all resolvable properties. Prefer {@link #getResolvableProperties(Map, Map)} to list
     * all properties which can be resolved for a particular configuration.
     *
     * @return the list of resolvable properties which do not depend on the application configuration
     */
    @Get("/list")
    public Result<List<String>> getResolvableProperties() {
        return getResolvableProperties(Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Lists all resolvable properties for a particular configuration.
     *
     * @param propertyEntries the property entries
     * @param testResourcesConfig the test resources configuration
     * @return the list of resolvable properties for the supplied configuration
     */
    @Post("/list")
    @Consumes(TestResourcesMediaType.TEST_RESOURCES_BINARY)
    public Result<List<String>> getResolvableProperties(Map<String, Collection<String>> propertyEntries,
                                                        Map<String, Object> testResourcesConfig) {
        var sanitizedTestResourcesConfig = sanitizeTestResourcesConfig(testResourcesConfig);
        return Result.of(loader.getResolvers()
            .stream()
            .filter(testResourcesResolver -> isEnabled(testResourcesResolver, sanitizedTestResourcesConfig))
            .map(r -> r.getResolvableProperties(propertyEntries, sanitizedTestResourcesConfig))
            .flatMap(Collection::stream)
            .distinct()
            .peek(p -> LOGGER.debug(
                "For configuration {} and property entries {} , resolvable property: {}",
                sanitizedTestResourcesConfig, propertyEntries, p))
            .toList());
    }

    /**
     * Lists all properties required to resolve a particular expression.
     *
     * @param expression the expression which needs to be resolved.
     * @return the list of required properties
     */
    @Get("/requirements/expr/{expression}")
    public Result<List<String>> getRequiredProperties(String expression) {
        return Result.of(loader.getResolvers()
            .stream()
            .map(testResourcesResolver -> testResourcesResolver.getRequiredProperties(expression))
            .flatMap(Collection::stream)
            .distinct()
            .toList());
    }

    /**
     * Lists all properties required by all resolvers.
     *
     * @return the list of required properties
     */
    @Get("/requirements/entries")
    public Result<List<String>> getRequiredPropertyEntries() {
        return Result.of(loader.getResolvers()
            .stream()
            .map(TestResourcesResolver::getRequiredPropertyEntries)
            .flatMap(Collection::stream)
            .distinct()
            .toList());
    }

    /**
     * Resolves a property.
     *
     * @param name the property to resolve
     * @param properties the resolved required properties
     * @param testResourcesConfig the test resources configuration
     * @return the resolved property, if any
     */
    @Post("/resolve")
    @Consumes(TestResourcesMediaType.TEST_RESOURCES_BINARY)
    public Optional<Result<String>> resolve(String name,
                                            Map<String, Object> properties,
                                            Map<String, Object> testResourcesConfig) {
        var sanitizedTestResourcesConfig = sanitizeTestResourcesConfig(testResourcesConfig);
        Optional<String> result = Optional.empty();
        for (TestResourcesResolver resolver : loader.getResolvers()) {
            if (!isEnabled(resolver, sanitizedTestResourcesConfig)) {
                continue;
            }
            try {
                result = resolver.resolve(name, properties, sanitizedTestResourcesConfig);
                LOGGER.debug(
                    "Attempt to resolve {} with resolver {}, properties {} and test resources configuration {} : {}",
                    name, resolver.getClass(), properties, sanitizedTestResourcesConfig,
                    result.orElse("\uD83D\uDEAB"));
            } catch (Exception ex) {
                for (PropertyResolutionListener listener : propertyResolutionListeners) {
                    listener.errored(name, resolver, ex);
                }
                throw TestResourcesResolutionException.wrap(ex);
            }
            if (result.isPresent()) {
                for (PropertyResolutionListener listener : propertyResolutionListeners) {
                    listener.resolved(name, result.get(), resolver, properties,
                        sanitizedTestResourcesConfig);
                }
                return Result.asOptional(result.get());
            }
        }
        return Optional.empty();
    }

    /**
     * Closes all test resources.
     *
     * @return true if the operation was successful
     */
    @Get("/close/all")
    public Result<Boolean> closeAll() {
        LOGGER.debug("Closing all test resources");
        return Result.of(TestContainers.closeAll());
    }

    /**
     * Closes a test resource scope.
     *
     * @param id the scope id
     * @return true if the operation was successful
     */
    @Get("/close/{id}")
    public Result<Boolean> closeScope(@Nullable String id) {
        LOGGER.info("Closing test resources of scope {}", id);
        return Result.of(TestContainers.closeScope(id));
    }

    /**
     * Lists all test containers started by the server.
     *
     * @return the list of test containers
     */
    @Get("/testcontainers")
    public Result<List<Map<String, Object>>> listContainers() {
        return listContainersByScope(null);
    }

    /**
     * Lists all test containers started by the server for a particular scope.
     *
     * @param scope the scope id
     * @return the list of test containers
     */
    @Get("/testcontainers/{scope}")
    public Result<List<Map<String, Object>>> listContainersByScope(@Nullable String scope) {
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

    /**
     * Requests a test resources service shutdown.
     *
     * @return a boolean indicating if shutdown was successful.
     */
    @Post("/stop")
    @Consumes(TestResourcesMediaType.TEST_RESOURCES_BINARY)
    public Result<Boolean> stopService() {
        taskScheduler.schedule(Duration.ofMillis(200), () -> {
            try {
                try {
                    embeddedServer.stop();
                    applicationContext.close();
                    closeResolvers();
                    TestContainers.closeAll();
                } finally {
                    Thread.sleep(MAX_STOP_TIMEOUT);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.exit(0);
            }
        });
        return Result.TRUE;
    }

    private void closeResolvers() {
        loader.getResolvers()
            .stream()
            .parallel()
            .filter(Closeable.class::isInstance)
            .map(Closeable.class::cast)
            .forEach(closable -> {
                try {
                    closable.close();
                } catch (IOException e) {
                    // ignore, we're shutting down anyway
                }
            });
    }

    private boolean isEnabled(TestResourcesResolver resolver,
                              Map<String, Object> testResourcesConfig) {
        if (resolver instanceof ToggableTestResourcesResolver toggable) {
            if (toggable.isEnabled(testResourcesConfig)) {
                return true;
            }
            if (warnedDisabledResolvers.add(toggable.getName())) {
                LOGGER.warn("Test resources provider for {} is disabled", toggable.getDisplayName());
            }
            return false;
        }
        return true;
    }

    /**
     * This method is responsible of making sure that the test resources configuration
     * map doesn't contain any entry which starts with `test-resources.` since the prefix
     * is supposed to be removed. However, depending on how the client is used, or even
     * if the service is called directly, it may be possible to pass in such configuration.
     * This is therefore a "best effort" to cleanup the configuration in case this happens.
     *
     * @param testResourcesConfig the configuration map
     * @return a cleanup map
     */
    private static Map<String, Object> sanitizeTestResourcesConfig(Map<String, Object> testResourcesConfig) {
        if (testResourcesConfig.keySet().stream().noneMatch(k -> k.startsWith(TEST_RESOURCES_PREFIX))) {
            return testResourcesConfig;
        }
        return testResourcesConfig.entrySet()
            .stream()
            .collect(Collectors.toMap(
                e -> {
                    var key = e.getKey();
                    if (key.startsWith(TEST_RESOURCES_PREFIX)) {
                        LOGGER.warn("Test resources configuration key '{}' should be passed without the '{}' prefix", key, TEST_RESOURCES_PREFIX);
                        return key.substring(TEST_RESOURCES_PREFIX.length());
                    }
                    return key;
                },
                Map.Entry::getValue
            ));
    }
}
