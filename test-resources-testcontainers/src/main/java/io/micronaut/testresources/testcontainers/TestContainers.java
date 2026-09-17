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
import io.micronaut.testresources.core.TestResourcesResolutionException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An utility class used to manage the lifecycle of test containers.
 * We preserve the list of open containers in-memory in a static field,
 * because we want them to live as long as the VM is live.
 *
 * It is possible to explicitly shutdown all containers by calling
 * the {@link #closeAll()} method.
 */
public final class TestContainers {
    private static final Map<Key, GenericContainer<?>> CONTAINERS_BY_KEY =
        new HashMap<>();
    private static final Map<String, Set<GenericContainer<?>>> CONTAINERS_BY_PROPERTY =
        new HashMap<>();
    private static final Map<DockerImageName, AtomicInteger> PULLING = new HashMap<>();
    private static final Map<DockerImageName, AtomicInteger> STARTING = new HashMap<>();
    private static final Map<Key, Lock> OPERATIONS_PER_KEY = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(TestContainers.class);
    private static final Map<String, Network> NETWORKS_BY_KEY = new ConcurrentHashMap<>();
    private static final Map<GenericContainer<?>, Set<String>> DATABASES_BY_CONTAINER = new IdentityHashMap<>();
    private static final Map<GenericContainer<?>, Lock> DATABASE_LOCKS_BY_CONTAINER = new IdentityHashMap<>();

    private static final Lock MAP_LOCK = new ReentrantLock();

    private TestContainers() {

    }

    private static <R> R withKey(Key key, Function<Key, R> action) {
        var lock = OPERATIONS_PER_KEY.computeIfAbsent(key, op -> new ReentrantLock());
        lock.lock();
        try {
            return action.apply(key);
        } finally {
            lock.unlock();
        }
    }

    private static <B> B withMapLock(String description, Supplier<B> supplier) {
        MAP_LOCK.lock();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Locked for {}", description);
        }
        try {
            return supplier.get();
        } finally {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Unlocked for {}", description);
            }
            MAP_LOCK.unlock();
        }
    }

    /**
     * Returns a test container and caches it, so that if the same owner
     * and properties are requested, we can return an existing container.
     *
     * @param <T> the container type
     * @param requestedProperty the property that this container will resolve
     * @param owner the identity which requested the creation of a container
     * @param name the identifier of the container
     * @param scope the test resources scope
     * @param query the parameters used to create the container. Different parameters mean
     * different container will be created.
     * @param imageNameSupplier the function which computes the image name
     * @param creator if the container is not in cache, factory to create the container
     * @return the container
     */
    static <T extends GenericContainer<? extends T>> T getOrCreate(String requestedProperty,
                                                                   String owner,
                                                                   String name,
                                                                   Scope scope,
                                                                   Map<String, Object> query,
                                                                   Supplier<DockerImageName> imageNameSupplier,
                                                                   Function<DockerImageName, T> creator) {
        return withKey(Key.of(owner, name, scope, query), key -> {
            try {
                T container = withMapLock("getOrCreate", () -> (T) CONTAINERS_BY_KEY.get(key));
                if (container != null && container.getContainerId() != null && !isRunning(container)) {
                    removeCachedContainer(key, container);
                    container = null;
                }
                var dockerImageName = imageNameSupplier.get();
                if (container == null) {
                    notifyStartOperation(PULLING, dockerImageName);
                    try {
                        container = creator.apply(dockerImageName);
                    } finally {
                        notifyEndOperation(PULLING, dockerImageName);
                    }
                    try {
                        notifyStartOperation(STARTING, dockerImageName);
                        if (DockerSupport.isDockerAvailable()) {
                            LOGGER.info("Starting test container {}", name);
                            container.start();
                        } else {
                            LOGGER.info("Trying to start container {} but it is likely to fail as Docker doesn't seem to be available", name);
                            container.start();
                        }
                    } finally {
                        notifyEndOperation(STARTING, dockerImageName);
                    }
                    T finalContainer = container;
                    withMapLock("getOrCreate", () -> CONTAINERS_BY_KEY.put(key, finalContainer));
                }
                T finalContainer = container;
                withMapLock("getOrCreate", () ->
                    CONTAINERS_BY_PROPERTY.computeIfAbsent(requestedProperty,
                            e -> new LinkedHashSet<>())
                        .add(finalContainer)
                );
                return container;
            }  catch (ContainerFetchException ex) {
                // unwrap message for clearer error on the client side
                Throwable cause = ex.getCause();
                String message = cause == null ? ex.getMessage() : cause.getMessage();
                if (message == null) {
                    message = "Unable to fetch container image";
                }
                throw new TestResourcesResolutionException(message);
            }
        });
    }

    private static boolean isRunning(GenericContainer<?> container) {
        try {
            return container.isRunning();
        } catch (RuntimeException e) {
            LOGGER.debug("Unable to inspect cached container {}; recreating it", container.getContainerId(), e);
            return false;
        }
    }

    private static void removeCachedContainer(Key key, GenericContainer<?> container) {
        withMapLock("removeCachedContainer", () -> {
            if (CONTAINERS_BY_KEY.get(key) == container) {
                CONTAINERS_BY_KEY.remove(key);
                CONTAINERS_BY_PROPERTY.values().forEach(containers -> containers.remove(container));
            }
            return null;
        });
        try {
            container.close();
        } catch (RuntimeException e) {
            LOGGER.debug("Unable to close stale cached container {}", container.getContainerId(), e);
        }
    }

    private static void notifyStartOperation(Map<DockerImageName, AtomicInteger> operation, DockerImageName dockerImageName) {
        withMapLock("notifyStartOperation", () -> {
            operation.computeIfAbsent(dockerImageName, unused -> new AtomicInteger(0))
                .incrementAndGet();
            return null;
        });
    }

    private static void notifyEndOperation(Map<DockerImageName, AtomicInteger> operation, DockerImageName dockerImageName) {
        withMapLock("notifyEndOperation", () -> {
            AtomicInteger counter = operation.get(dockerImageName);
            if (counter == null) {
                return null;
            }
            var remaining = counter.decrementAndGet();
            if (remaining == 0) {
                operation.remove(dockerImageName);
            }
            return null;
        });
    }

    /**
     * Returns the list of containers which are being started.
     *
     * @return the list of containers
     */
    public static List<String> startingContainers() {
        return withMapLock("containersInProgress", () ->
            STARTING.keySet().stream().map(DockerImageName::toString).sorted().toList()
        );
    }

    /**
     * Returns the list of containers which are being pulled.
     *
     * @return the list of containers
     */
    public static List<String> pullingContainers() {
        return withMapLock("containersInProgress", () ->
            PULLING.keySet().stream().map(DockerImageName::toString).sorted().toList()
        );
    }

    /**
     * Lists all containers.
     *
     * @return the containers
     */
    public static Map<Scope, List<GenericContainer<?>>> listAll() {
        return listByScope(Scope.ROOT);
    }

    public static Map<Scope, List<GenericContainer<?>>> listByScope(@Nullable String id) {
        Scope scope = Scope.of(id);
        return listByScope(scope);
    }

    private static Map<Scope, List<GenericContainer<?>>> listByScope(Scope scope) {
        return withMapLock("listByScope", () -> CONTAINERS_BY_KEY.entrySet()
            .stream()
            .filter(entry -> scope.includes(entry.getKey().scope))
            .collect(Collectors.groupingBy(
                entry -> entry.getKey().scope,
                Collectors.mapping(
                    Map.Entry::getValue,
                    Collectors.toList()
                )
            ))
        );
    }

    @SuppressWarnings("java:S6204") // toList() breaks the return type
    private static List<GenericContainer<?>> filterByScope(Scope scope,
                                                           Set<GenericContainer<?>> containers) {
        if (containers.isEmpty()) {
            return Collections.emptyList();
        }
        return withMapLock("filterByScope", () -> CONTAINERS_BY_KEY.entrySet()
            .stream()
            .filter(entry -> containers.contains(entry.getValue()) &&
                             scope.includes(entry.getKey().scope))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList())
        );
    }

    public static Network network(String name) {
        return withMapLock("network",
            () -> NETWORKS_BY_KEY.computeIfAbsent(name, k -> Network.newNetwork()));
    }

    public static boolean closeAll() {
        return withMapLock("closeAll", () -> {
            boolean closed = false;
            for (GenericContainer<?> container : CONTAINERS_BY_KEY.values()) {
                container.close();
                closed = true;
            }
            CONTAINERS_BY_KEY.clear();
            CONTAINERS_BY_PROPERTY.clear();
            DATABASES_BY_CONTAINER.clear();
            DATABASE_LOCKS_BY_CONTAINER.clear();
            NETWORKS_BY_KEY.values().forEach(Network::close);
            NETWORKS_BY_KEY.clear();
            return closed;
        });
    }

    public static Map<String, Network> getNetworks() {
        return Collections.unmodifiableMap(NETWORKS_BY_KEY);
    }

    public static boolean closeScope(@Nullable String id) {
        Scope scope = Scope.of(id);
        return withMapLock("closeScope", () -> {
            boolean closed = false;
            Iterator<Map.Entry<Key, GenericContainer<?>>> iterator =
                CONTAINERS_BY_KEY.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Key, GenericContainer<?>> entry = iterator.next();
                var existingScope = entry.getKey().scope;
                if (scope.includes(existingScope)) {
                    iterator.remove();
                    GenericContainer<?> container = entry.getValue();
                    LOGGER.debug("Stopping container {}", container.getContainerId());
                    container.close();
                    DATABASES_BY_CONTAINER.remove(container);
                    DATABASE_LOCKS_BY_CONTAINER.remove(container);
                    closed = true;
                    for (Set<GenericContainer<?>> value : CONTAINERS_BY_PROPERTY.values()) {
                        value.remove(container);
                    }
                }
            }
            return closed;
        });
    }

    public static List<GenericContainer<?>> findByRequestedProperty(Scope scope, String property) {
        return withMapLock("findByRequestedProperty", () -> {
            Set<GenericContainer<?>> byProperty =
                CONTAINERS_BY_PROPERTY.getOrDefault(property, Collections.emptySet());
            LOGGER.debug("Found {} containers for property {}. All properties: {}",
                byProperty.size(), property, CONTAINERS_BY_PROPERTY.keySet());
            return filterByScope(scope, byProperty);
        });
    }

    public static boolean hasDatabase(GenericContainer<?> container, String databaseName) {
        return withMapLock("hasDatabase", () ->
            DATABASES_BY_CONTAINER.getOrDefault(container, Collections.emptySet()).contains(databaseName)
        );
    }

    public static void rememberDatabase(GenericContainer<?> container, String databaseName) {
        withMapLock("rememberDatabase", () -> {
            DATABASES_BY_CONTAINER
                .computeIfAbsent(container, unused -> new LinkedHashSet<>())
                .add(databaseName);
            return null;
        });
    }

    public static void createDatabaseIfMissing(GenericContainer<?> container,
                                               String databaseName,
                                               Runnable databaseCreator) {
        Lock lock = withMapLock("databaseLock", () ->
            DATABASE_LOCKS_BY_CONTAINER.computeIfAbsent(container, unused -> new ReentrantLock())
        );
        lock.lock();
        try {
            if (!hasDatabase(container, databaseName)) {
                databaseCreator.run();
                rememberDatabase(container, databaseName);
            }
        } finally {
            lock.unlock();
        }
    }

    private static final class Key {
        private final String owner;
        private final String name;
        private final Map<String, String> properties;
        private final int hashCode;
        private final Scope scope;

        private Key(String owner, String name, Scope scope, Map<String, String> properties) {
            this.owner = owner;
            this.name = name;
            this.scope = scope;
            this.properties = properties;
            this.hashCode =
                31 * (31 * (31 * owner.hashCode() + properties.hashCode()) + scope.hashCode()) +
                name.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Key key = (Key) o;
            if (!name.equals(key.name)) {
                return false;
            }

            if (!scope.equals(key.scope)) {
                return false;
            }

            if (!owner.equals(key.owner)) {
                return false;
            }
            return properties.equals(key.properties);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        static Key of(String owner, String name, Scope scope, Map<String, Object> properties) {
            if (properties.isEmpty()) {
                return new Key(owner, name, scope, Collections.emptyMap());
            }
            Map<String, String> converted = new HashMap<>(properties.size());
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                converted.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            return new Key(owner, name, scope, Collections.unmodifiableMap(converted));
        }
    }
}
