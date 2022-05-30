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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.micronaut.testresources.core.Scope.PROPERTY_KEY;

/**
 * An utility class used to manage the lifecycle of test containers.
 * We preserve the list of open containers in-memory in a static field,
 * because we want them to live as long as the VM is live.
 *
 * It is possible to explicitly shutdown all containers by calling
 * the {@link #closeAll()} method.
 */
public final class TestContainers {
    private static final Map<Key, GenericContainer<?>> CONTAINERS = new HashMap<>();
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Logger LOGGER = LoggerFactory.getLogger(TestContainers.class);

    private TestContainers() {

    }

    /**
     * Returns a test container and caches it, so that if the same owner
     * and properties are requested, we can return an existing container.
     *
     * @param <T> the container type
     * @param owner the class which requested the creation of a container
     * @param name an identifier for the container, used for logging purposes
     * @param query the parameters used to create the container. Different parameters mean
     * different container will be created.
     * @param creator if the container is not in cache, factory to create the container
     * @return the container
     */
    static <T extends GenericContainer<? extends T>> T getOrCreate(Class<?> owner,
                                                                   String name,
                                                                   Map<String, Object> query,
                                                                   Supplier<T> creator) {
        Key key = Key.of(owner, scopeOf(query), query);
        LOCK.lock();
        @SuppressWarnings("unchecked")
        T container = (T) CONTAINERS.get(key);
        if (container == null) {
            container = creator.get();
            LOGGER.info("Starting test container {}", name);
            container.start();
            CONTAINERS.put(key, container);
        }
        LOCK.unlock();
        return container;
    }

    private static Scope scopeOf(Map<String, Object> query) {
        Object scopeId = query.getOrDefault(PROPERTY_KEY, null);
        if (scopeId == null) {
            return Scope.ROOT;
        }
        return Scope.of(String.valueOf(scopeId));
    }

    /**
     * Lists all containers.
     * @return the containers
     */
    public static Map<Scope, List<GenericContainer<?>>> listAll() {
        return listByScope(null);
    }

    public static Map<Scope, List<GenericContainer<?>>> listByScope(String id) {
        Scope scope = Scope.of(id);
        LOCK.lock();
        try {
            return CONTAINERS.entrySet()
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

    public static void closeAll() {
        LOCK.lock();
        for (GenericContainer<?> container : CONTAINERS.values()) {
            container.close();
        }
        CONTAINERS.clear();
        LOCK.unlock();
    }

    public static void closeScope(String id) {
        Scope scope = Scope.of(id);
        LOCK.lock();
        Iterator<Map.Entry<Key, GenericContainer<?>>> iterator = CONTAINERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, GenericContainer<?>> entry = iterator.next();
            if (entry.getKey().scope.includes(scope)) {
                iterator.remove();
                entry.getValue().close();
            }
        }
        LOCK.unlock();
    }

    private static final class Key {
        private final Class<?> type;
        private final Scope scope;
        private final Map<String, String> properties;
        private final int hashCode;

        private Key(Class<?> type, Scope scope, Map<String, String> properties) {
            this.type = type;
            this.scope = scope;
            this.properties = properties;
            this.hashCode = 31 * (31 * type.hashCode() + properties.hashCode()) + scope.hashCode();
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

        static <T> Key of(Class<T> type, Scope scope, Map<String, Object> properties) {
            if (properties.isEmpty()) {
                return new Key(type, scope, Collections.emptyMap());
            }
            Map<String, String> converted = new HashMap<>(properties.size());
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                converted.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            return new Key(type, scope, Collections.unmodifiableMap(converted));
        }
    }
}
