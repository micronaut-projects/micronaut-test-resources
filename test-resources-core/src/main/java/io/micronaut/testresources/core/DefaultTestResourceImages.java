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
package io.micronaut.testresources.core;

import io.micronaut.core.annotation.Internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default Docker images used by test resource providers.
 */
@Internal
public final class DefaultTestResourceImages {
    private static final String MANIFEST_RESOURCE = "/io/micronaut/testresources/core/default-images/Dockerfile";
    private static final Pattern FROM_PATTERN = Pattern.compile("^FROM\\s+(\\S+)\\s+AS\\s+([a-z0-9_]+)$");
    private static final Map<String, String> IMAGES = loadImages();

    public static final String DEFAULT_AZURITE_IMAGE = image("azurite");
    public static final String DEFAULT_CONSUL_IMAGE = image("consul");
    public static final String DEFAULT_COUCHBASE_IMAGE = image("couchbase");
    public static final String DEFAULT_ELASTICSEARCH_IMAGE = image("elasticsearch");
    public static final String DEFAULT_HAZELCAST_IMAGE = image("hazelcast");
    public static final String DEFAULT_HIVEMQ_IMAGE = image("hivemq");
    public static final String DEFAULT_INFINISPAN_IMAGE = image("infinispan");
    public static final String DEFAULT_KAFKA_IMAGE = image("kafka");
    public static final String DEFAULT_KEYCLOAK_IMAGE = image("keycloak");
    public static final String DEFAULT_LOCALSTACK_IMAGE = image("localstack");
    public static final String DEFAULT_MARIADB_IMAGE = image("mariadb");
    public static final String DEFAULT_MINIO_IMAGE = image("minio");
    public static final String DEFAULT_MONGODB_IMAGE = image("mongodb");
    public static final String DEFAULT_MSSQL_IMAGE = image("mssql");
    public static final String DEFAULT_MYSQL_IMAGE = image("mysql");
    public static final String DEFAULT_MYSQL_COMMUNITY_IMAGE = image("mysql_community");
    public static final String DEFAULT_NEO4J_IMAGE = image("neo4j");
    public static final String DEFAULT_OPENSEARCH_IMAGE = image("opensearch");
    public static final String DEFAULT_ORACLE_FREE_IMAGE = image("oracle_free");
    public static final String DEFAULT_ORACLE_XE_IMAGE = image("oracle_xe");
    public static final String DEFAULT_POSTGRES_IMAGE = image("postgres");
    public static final String DEFAULT_PULSAR_IMAGE = image("pulsar");
    public static final String DEFAULT_RABBITMQ_IMAGE = image("rabbitmq");
    public static final String DEFAULT_REDIS_IMAGE = image("redis");
    public static final String DEFAULT_REDIS_CLUSTER_IMAGE = image("redis_cluster");
    public static final String DEFAULT_SEAWEEDFS_IMAGE = image("seaweedfs");
    public static final String DEFAULT_SOLR_IMAGE = image("solr");
    public static final String DEFAULT_VAULT_IMAGE = image("vault");

    private DefaultTestResourceImages() {
    }

    /**
     * Returns the default image for the given manifest alias.
     *
     * @param alias The manifest alias
     * @return The Docker image name
     */
    public static String image(String alias) {
        String image = IMAGES.get(alias);
        if (image == null) {
            throw new IllegalArgumentException("No default Docker image is configured for '" + alias + "'");
        }
        return image;
    }

    /**
     * Returns all configured default images keyed by manifest alias.
     *
     * @return The default images
     */
    public static Map<String, String> images() {
        return IMAGES;
    }

    private static Map<String, String> loadImages() {
        InputStream resource = DefaultTestResourceImages.class.getResourceAsStream(MANIFEST_RESOURCE);
        if (resource == null) {
            throw new IllegalStateException("Default Docker image manifest not found: " + MANIFEST_RESOURCE);
        }
        Map<String, String> images = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = FROM_PATTERN.matcher(line);
                if (matcher.matches()) {
                    images.put(matcher.group(2), matcher.group(1));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read default Docker image manifest", e);
        }
        if (images.isEmpty()) {
            throw new IllegalStateException("Default Docker image manifest does not declare any images");
        }
        return Collections.unmodifiableMap(images);
    }
}
