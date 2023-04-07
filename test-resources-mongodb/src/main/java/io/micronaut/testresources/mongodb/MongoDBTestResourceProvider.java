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

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn a MongoDB test container.
 */
public class MongoDBTestResourceProvider extends AbstractTestContainersProvider<MongoDBContainer> {

    public static final String MONGODB_SERVERS = "mongodb.servers";
    public static final String MONGODB_SERVER_URI = "mongodb.uri";
    public static final String DEFAULT_IMAGE = "mongo:5";
    public static final String SIMPLE_NAME = "mongodb";
    public static final String DB_NAME = "containers." + SIMPLE_NAME + ".db-name";

    private String dbName;

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        List<String> servers = List.copyOf(propertyEntries.getOrDefault(MONGODB_SERVERS, Collections.emptySet()));
        if (servers.isEmpty()) {
            return Collections.singletonList(MONGODB_SERVER_URI);
        } else {
            return servers.stream().map(s -> MONGODB_SERVERS + "." + s + ".uri").toList();
        }
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return List.of(MONGODB_SERVERS);
    }

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    protected MongoDBContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        Object configuredDbName = testResourcesConfiguration.get(DB_NAME);
        if (configuredDbName != null) {
            this.dbName = configuredDbName.toString();
        }
        return new MongoDBContainer(imageName);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, MongoDBContainer container) {
        Optional<String> database = extractMongoDbServerFrom(propertyName);
        if (database.isPresent()) {
            return Optional.of(container.getReplicaSetUrl(database.get()));
        }
        String url = dbName == null ? container.getReplicaSetUrl() : container.getReplicaSetUrl(dbName);
        return Optional.of(url);
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        if (extractMongoDbServerFrom(propertyName).isPresent()) {
            return true;
        }
        return MONGODB_SERVER_URI.equals(propertyName);
    }

    private Optional<String> extractMongoDbServerFrom(String propertyName) {
        if (propertyName.startsWith(MONGODB_SERVERS + ".")) {
            String suffix = propertyName.substring(MONGODB_SERVERS.length() + 1);
            int dot = suffix.indexOf(".");
            if (dot > 0) {
                int nextDot = suffix.indexOf(".", dot + 1);
                if (nextDot == -1) {
                    String database = suffix.substring(0, dot);
                    String property = suffix.substring(dot + 1);
                    if ("uri".equals(property)) {
                        // it's the only property that we support
                        return Optional.of(database);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
