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
package io.micronaut.testresources.mongodb;

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves MongoDB properties from Docker Compose services.
 */
public final class MongoDBComposeTestResourceProvider extends MongoDBTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor MONGODB =
        new ComposeResolverSupport.ServiceDescriptor("mongodb", List.of("mongo"), 27017);

    @Override
    public String getDisplayName() {
        return "Docker Compose MongoDB";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        Optional<String> database = extractMongoDbServerFrom(propertyName);
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            MONGODB,
            context -> database.map(context::matchesDatasource).orElse(true),
            context -> "mongodb://" + context.hostPort() + databasePath(context, database.orElse(null))
        );
    }

    private static String databasePath(ComposeResolverSupport.ResolutionContext context, String database) {
        String resolved = database == null ? context.labelOrEnvironment(ComposeResolverSupport.DATABASE_LABEL, "MONGO_INITDB_DATABASE", "") : database;
        return resolved.isBlank() ? "" : "/" + resolved;
    }
}
