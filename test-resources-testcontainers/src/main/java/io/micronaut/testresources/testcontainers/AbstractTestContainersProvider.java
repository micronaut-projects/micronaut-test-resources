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
package io.micronaut.testresources.testcontainers;

import io.micronaut.testresources.core.Scope;
import io.micronaut.testresources.core.ToggableTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.testresources.core.DefaultTestResourceImages.DEFAULT_MARIADB_IMAGE;
import static io.micronaut.testresources.core.DefaultTestResourceImages.DEFAULT_MYSQL_IMAGE;
import static io.micronaut.testresources.core.DefaultTestResourceImages.DEFAULT_POSTGRES_IMAGE;
import static io.micronaut.testresources.testcontainers.TestContainerMetadataSupport.SPECIFIC_ORDER;

/**
 * The base class for test resources providers which spawn test containers.
 *
 * @param <T> the container type
 */
public abstract class AbstractTestContainersProvider<T extends GenericContainer<? extends T>>
    implements ToggableTestResourcesResolver {
    private static final String TEST_RESOURCES_RESOLVER_SERVICE =
        "META-INF/services/io.micronaut.testresources.core.TestResourcesResolver";

    @Override
    public String getName() {
        if (this instanceof ComposeAwareTestResourcesResolver) {
            return "compose." + getSimpleName();
        }
        return "containers." + getSimpleName();
    }

    @Override
    public int getOrder() {
        if (this instanceof ComposeAwareTestResourcesResolver) {
            return ComposeResolverSupport.ORDER;
        }
        return SPECIFIC_ORDER;
    }

    @Override
    public boolean isEnabled(Map<String, Object> testResourcesConfig) {
        if (this instanceof ComposeAwareTestResourcesResolver) {
            return ComposeResolverSupport.isEnabled(testResourcesConfig);
        }
        if (ComposeResolverSupport.isEnabled(testResourcesConfig) && hasComposeAwareReplacement()) {
            return false;
        }
        return ToggableTestResourcesResolver.super.isEnabled(testResourcesConfig);
    }

    private boolean hasComposeAwareReplacement() {
        String className = getClass().getName();
        if (!className.endsWith("TestResourceProvider")) {
            return false;
        }
        String composeClassName = className.substring(0, className.length() - "TestResourceProvider".length())
            + "ComposeTestResourceProvider";
        if (!isComposeReplacementAdvertised(composeClassName)) {
            return false;
        }
        try {
            Class<?> composeClass = Class.forName(composeClassName, false, getClass().getClassLoader());
            return ComposeAwareTestResourcesResolver.class.isAssignableFrom(composeClass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isComposeReplacementAdvertised(String composeClassName) {
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            Enumeration<URL> resources = classLoader.getResources(TEST_RESOURCES_RESOLVER_SERVICE);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
                    if (reader.lines().map(String::trim).anyMatch(composeClassName::equals)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    /**
     * Returns the name of the resource resolver, for example "kafka" or "mysql".
     *
     * @return the name of the resolver
     */
    protected abstract String getSimpleName();

    /**
     * Returns the default image name.
     *
     * @return the default image name.
     */
    protected abstract String getDefaultImageName();

    /**
     * Creates the test container.
     *
     * @param imageName the docker image name
     * @param requestedProperties the resolved properties
     * @param testResourcesConfig the test resources configuration
     * @return a container
     */
    protected abstract T createContainer(DockerImageName imageName,
                                         Map<String, Object> requestedProperties,
                                         Map<String, Object> testResourcesConfig);

    /**
     * Determines if this resolver can resolve the requested property.
     * It is used in order to make sure that a "Postgres" resolver wouldn't
     * provide a value if the requested container type is for MySQL, for
     * example.
     *
     * @param propertyName the property to resolve
     * @param requestedProperties the resolved properties
     * @param testResourcesConfig the test resources configuration
     * @return if this resolver should answer
     */
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties,
                                   Map<String, Object> testResourcesConfig) {
        return true;
    }

    /**
     * Lets a resolver provide a value for the requested property without triggering the
     * creation of a test container. This can be used in case a resolver wants to check
     * existing containers first.
     *
     * @param propertyName the name of the property to resolve
     * @param properties the properties used to resolve
     * @param testResourcesConfig the test resources configuration
     * @return a resolved property
     */
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return Optional.empty();
    }

    @Override
    public final Optional<String> resolve(String propertyName, Map<String, Object> properties,
                                          Map<String, Object> testResourcesConfig) {
        if (shouldAnswer(propertyName, properties, testResourcesConfig)) {
            Optional<String> firstPass =
                resolveWithoutContainer(propertyName, properties, testResourcesConfig);
            if (firstPass.isPresent()) {
                return firstPass;
            }
            Scope scope = Scope.from(properties);
            String containerOwnerKey = getContainerOwnerKey(propertyName, properties, testResourcesConfig);
            Map<String, Object> containerQuery = getContainerQuery(propertyName, properties, testResourcesConfig);
            T container = TestContainers.getOrCreate(propertyName, containerOwnerKey, getSimpleName(),
                scope, containerQuery, () -> {
                    String defaultImageName = getEffectiveDefaultImageName();
                    DockerImageName imageName = DockerImageName.parse(defaultImageName);
                    Optional<TestContainerMetadata> metadata =
                        TestContainerMetadataSupport.containerMetadataFor(
                                Collections.singletonList(getSimpleName()), testResourcesConfig)
                            .findAny();
                    if (metadata.isPresent()) {
                        TestContainerMetadata md = metadata.get();
                        if (md.getImageName().isPresent()) {
                            imageName = DockerImageName.parse(md.getImageName().get())
                                .asCompatibleSubstituteFor(defaultImageName);
                        }
                        if (md.getImageTag().isPresent()) {
                            imageName = imageName.withTag(md.getImageTag().get());
                        }
                    }
                    return imageName;
                }, imageName -> {
                    Optional<TestContainerMetadata> metadata =
                        TestContainerMetadataSupport.containerMetadataFor(
                                Collections.singletonList(getSimpleName()), testResourcesConfig)
                            .findAny();
                    T createdContainer = createContainer(imageName, properties, testResourcesConfig);
                    configureContainer(createdContainer, properties, testResourcesConfig);
                    getDefaultStartupTimeout(properties, testResourcesConfig).ifPresent(createdContainer::withStartupTimeout);
                    metadata.ifPresent(
                        md -> TestContainerMetadataSupport.applyMetadata(md, createdContainer));
                    return createdContainer;
                });
            prepareContainer(propertyName, container, properties, testResourcesConfig);
            return resolveProperty(propertyName, container, properties, testResourcesConfig);
        }
        return Optional.empty();
    }

    /**
     * Returns the owner key used to scope cached containers for this resolver.
     * Subclasses may override to share a physical container across multiple logical
     * consumers, but should keep the returned key stable for equivalent requests.
     *
     * @param propertyName the property being resolved
     * @param properties the resolved properties for the request
     * @param testResourcesConfig the test resources configuration
     * @return the owner key used for container reuse
     */
    protected String getContainerOwnerKey(String propertyName,
                                          Map<String, Object> properties,
                                          Map<String, Object> testResourcesConfig) {
        return getClass().getName();
    }

    /**
     * Returns the query object used to look up or create a cached container.
     * Subclasses may override to normalize request-specific properties into a
     * stable physical-resource identity while preserving any keys needed for safe reuse.
     *
     * @param propertyName the property being resolved
     * @param properties the resolved properties for the request
     * @param testResourcesConfig the test resources configuration
     * @return the container query used for cache lookup
     */
    protected Map<String, Object> getContainerQuery(String propertyName,
                                                    Map<String, Object> properties,
                                                    Map<String, Object> testResourcesConfig) {
        return properties;
    }

    private String getEffectiveDefaultImageName() {
        return switch (getDefaultImageName()) {
            case "mariadb" -> DEFAULT_MARIADB_IMAGE;
            case "mysql:8.4.5" -> DEFAULT_MYSQL_IMAGE;
            case "postgres" -> DEFAULT_POSTGRES_IMAGE;
            default -> getDefaultImageName();
        };
    }

    protected void configureContainer(T container, Map<String, Object> properties,
                                      Map<String, Object> testResourcesConfig) {
    }

    /**
     * Returns the default startup timeout for this provider before user metadata is applied.
     *
     * @param properties the resolved properties for the request
     * @param testResourcesConfig the test resources configuration
     * @return the provider default startup timeout, if any
     */
    protected Optional<Duration> getDefaultStartupTimeout(Map<String, Object> properties,
                                                          Map<String, Object> testResourcesConfig) {
        return Optional.empty();
    }

    protected void prepareContainer(String propertyName,
                                    T container,
                                    Map<String, Object> properties,
                                    Map<String, Object> testResourcesConfig) {
    }

    /**
     * Resolves the requested property from the started container with access to the
     * full requested-property map and test-resources configuration. Subclasses may
     * override when the resolved value depends on request metadata in addition to the
     * container itself.
     *
     * @param propertyName the property being resolved
     * @param container the started container
     * @param properties the resolved properties for the request
     * @param testResourcesConfig the test resources configuration
     * @return the resolved value, if any
     */
    protected Optional<String> resolveProperty(String propertyName,
                                               T container,
                                               Map<String, Object> properties,
                                               Map<String, Object> testResourcesConfig) {
        return resolveProperty(propertyName, container);
    }

    protected abstract Optional<String> resolveProperty(String propertyName, T container);

    /**
     * Executes a command inside a running container and converts failures into a
     * consistent {@link IllegalStateException}. Interrupted executions preserve the
     * current thread interrupt status before the exception is raised.
     *
     * @param failureMessage the message to use when the command fails
     * @param command the command to execute
     */
    protected final void executeInContainer(String failureMessage, ContainerCommand command) {
        try {
            Container.ExecResult result = command.execute();
            if (result.getExitCode() != 0) {
                throw new IllegalStateException(failureMessage + ": " + result.getStderr());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failureMessage, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(failureMessage, e);
        }
    }

    protected final String stringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    @FunctionalInterface
    protected interface ContainerCommand {
        Container.ExecResult execute() throws Exception;
    }
}
