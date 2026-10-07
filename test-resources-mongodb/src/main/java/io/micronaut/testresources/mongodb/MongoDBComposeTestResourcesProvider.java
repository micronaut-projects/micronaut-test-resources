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

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeLabels;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves MongoDB properties from Docker Compose services.
 */
public final class MongoDBComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    private static final String SERVERS = "mongodb.servers";
    private static final String SERVERS_PREFIX = SERVERS + ".";

    public MongoDBComposeTestResourcesProvider() {
        super("mongodb", Set.of("mongodb", "mongo"), 27017, List.of("mongodb.uri"));
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries) {
        return java.util.stream.Stream.concat(
                super.getResolvableProperties(propertyEntries).stream(),
                propertyEntries.getOrDefault(SERVERS, List.of()).stream().map(server -> SERVERS_PREFIX + server + ".uri")
            )
            .toList();
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return List.of(SERVERS);
    }

    @Override
    public boolean supports(String propertyName, Map<String, Object> properties) {
        return super.supports(propertyName, properties) || propertyName.startsWith(SERVERS_PREFIX) && propertyName.endsWith(".uri");
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return "mongodb://" + context.hostPort() + "/" + context.labelOrEnvironment(ComposeLabels.DATABASE, "MONGO_INITDB_DATABASE", database(context.propertyName()));
    }

    private static String database(String propertyName) {
        if (propertyName.startsWith(SERVERS_PREFIX) && propertyName.endsWith(".uri")) {
            String suffix = propertyName.substring(SERVERS_PREFIX.length());
            return suffix.substring(0, suffix.length() - ".uri".length());
        }
        return "test";
    }
}
