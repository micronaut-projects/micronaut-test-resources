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

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves Neo4j properties from Docker Compose services.
 */
public final class Neo4jComposeTestResourceProvider extends Neo4jTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor NEO4J =
        new ComposeResolverSupport.ServiceDescriptor("neo4j", List.of(), 7687);

    @Override
    public String getDisplayName() {
        return "Docker Compose Neo4j";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return ComposeResolverSupport.resolve(propertyName, properties, testResourcesConfig, NEO4J, context -> true, context -> "bolt://" + context.hostPort());
    }
}
