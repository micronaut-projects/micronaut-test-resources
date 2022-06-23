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
package io.micronaut.testresources.neo4j;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which will spawn a MongoDB test container.
 */
public class Neo4jTestResourceProvider extends AbstractTestContainersProvider<Neo4jContainer<?>> {

    public static final String NEO4J_SERVER_URI = "neo4j.uri";
    public static final String DEFAULT_IMAGE = "neo4j";

    private static final Set<String> SUPPORTED_PROPERTIES;

    static {
        Set<String> supported = new HashSet<>();
        supported.add(NEO4J_SERVER_URI);
        SUPPORTED_PROPERTIES = Collections.unmodifiableSet(supported);
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return Collections.singletonList(NEO4J_SERVER_URI);
    }

    @Override
    protected String getSimpleName() {
        return "neo4j";
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    protected Neo4jContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        Neo4jContainer<?> container = new Neo4jContainer<>(imageName);
        container.withoutAuthentication();
        return container;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, Neo4jContainer<?> container) {
        if (NEO4J_SERVER_URI.equals(propertyName)) {
            return Optional.of(container.getBoltUrl());
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        return SUPPORTED_PROPERTIES.contains(propertyName);
    }
}
