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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
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
    private static final Map<Key, GenericContainer<?>> CONTAINERS_BY_KEY = new HashMap<>();
    private static final Map<String, Set<GenericContainer<?>>> CONTAINERS_BY_PROPERTY = new HashMap<>();
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Logger LOGGER = LoggerFactory.getLogger(TestContainers.class);
    private static final Map<String, Network> NETWORKS_BY_KEY = new ConcurrentHashMap<>();

    private TestContainers() {

    }

    /**
     * Returns a test container and caches it, so that if the same owner
     * and properties are requested, we can return an existing container.
     *
     * @param <T> the container type
     * @param requestedProperty the property that this container will resolve
     * @param owner the class which requested the creation of a container
     * @param name the identifier of the container
     * @param query the parameters used to create the container. Different parameters mean
     * different container will be created.
     * @param creator if the container is not in cache, factory to create the container
     * @return the container
     */
    static <T extends GenericContainer<? extends T>> T getOrCreate(String requestedProperty,
                                                                   Class<?> owner,
                                                                   String name,
                                                                   Map<String, Object> query,
                                                                   Supplier<T> creator) {
        Key key = Key.of(owner, name, Scope.from(query), query);
        LOCK.lock();
        @SuppressWarnings("unchecked")
        T container = (T) CONTAINERS_BY_KEY.get(key);
        if (container == null) {
            container = creator.get();
            LOGGER.info("Starting test container {}", name);
            container.start();
            CONTAINERS_BY_KEY.put(key, container);
        }
        CONTAINERS_BY_PROPERTY.computeIfAbsent(requestedProperty, e -> new LinkedHashSet<>())
            .add(container);
        LOCK.unlock();
        return container;
    }

    /**
     * Lists all containers.
     *
     * @return the containers
     */
    public static Map<Scope, List<GenericContainer<?>>> listAll() {
        return listByScope(Scope.ROOT);
    }

    public static Map<Scope, List<GenericContainer<?>>> listByScope(String id) {
        Scope scope = Scope.of(id);
        return listByScope(scope);
    }

    private static Map<Scope, List<GenericContainer<?>>> listByScope(Scope scope) {
        LOCK.lock();
        try {
            return CONTAINERS_BY_KEY.entrySet()
                .stream()
                .filter(entry -> scope.includes(entry.getKey().scope))
                .collect(Collectors.groupingBy(
                    entry -> entry.getKey().scope,
                    Collectors.mapping(
                        Map.Entry::getValue,
                        Collectors.toList()
                    )
                ));
        } finally {
            LOCK.unlock();
        }
    }

    private static List<GenericContainer<?>> filterByScope(Scope scope, Set<GenericContainer<?>> containers) {
        if (containers.isEmpty()) {
            return Collections.emptyList();
        }
        LOCK.lock();
        try {
            return CONTAINERS_BY_KEY.entrySet()
                .stream()
                .filter(entry -> containers.contains(entry.getValue()) && scope.includes(entry.getKey().scope))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        } finally {
            LOCK.unlock();
        }
    }

    public static Network network(String name) {
        return NETWORKS_BY_KEY.computeIfAbsent(name, k -> Network.newNetwork());
    }

    public static boolean closeAll() {
        LOCK.lock();
        boolean closed = false;
        for (GenericContainer<?> container : CONTAINERS_BY_KEY.values()) {
            container.close();
            closed = true;
        }
        CONTAINERS_BY_KEY.clear();
        CONTAINERS_BY_PROPERTY.clear();
        NETWORKS_BY_KEY.values().forEach(Network::close);
        NETWORKS_BY_KEY.clear();
        LOCK.unlock();
        return closed;
    }

    public static Map<String, Network> getNetworks() {
        return Collections.unmodifiableMap(NETWORKS_BY_KEY);
    }

    public static boolean closeScope(String id) {
        Scope scope = Scope.of(id);
        LOCK.lock();
        boolean closed = false;
        Iterator<Map.Entry<Key, GenericContainer<?>>> iterator = CONTAINERS_BY_KEY.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, GenericContainer<?>> entry = iterator.next();
            var existingScope = entry.getKey().scope;
            if (scope.includes(existingScope)) {
                iterator.remove();
                GenericContainer<?> container = entry.getValue();
                LOGGER.debug("Stopping container {}", container.getContainerId());
                container.close();
                closed = true;
                for (Set<GenericContainer<?>> value : CONTAINERS_BY_PROPERTY.values()) {
                    value.remove(container);
                }
            }
        }
        LOCK.unlock();
        return closed;
    }

    public static List<GenericContainer<?>> findByRequestedProperty(Scope scope, String property) {
        LOCK.lock();
        try {
            Set<GenericContainer<?>> byProperty = CONTAINERS_BY_PROPERTY.getOrDefault(property, Collections.emptySet());
            LOGGER.debug("Found {} containers for property {}. All properties: {}", byProperty.size(), property, CONTAINERS_BY_PROPERTY.keySet());
            return filterByScope(scope, byProperty);
        } finally {
            LOCK.unlock();
        }
    }

    private static final class Key {
        private final Class<?> type;
        private final String name;
        private final Scope scope;
        private final Map<String, String> properties;
        private final int hashCode;

        private Key(Class<?> type, String name, Scope scope, Map<String, String> properties) {
            this.type = type;
            this.name = name;
            this.scope = scope;
            this.properties = properties;
            this.hashCode = 31 * (31 * (31 * type.hashCode() + properties.hashCode()) + scope.hashCode()) + name.hashCode();
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

            if (!type.equals(key.type)) {
                return false;
            }
            return properties.equals(key.properties);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        static <T> Key of(Class<T> type, String name, Scope scope, Map<String, Object> properties) {
            if (properties.isEmpty()) {
                return new Key(type, name, scope, Collections.emptyMap());
            }
            Map<String, String> converted = new HashMap<>(properties.size());
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                converted.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            return new Key(type, name, scope, Collections.unmodifiableMap(converted));
        }
    }
}
