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
package io.micronaut.testresources.kafka;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn a Schema Registry test container.
 */
public class KafkaSchemaRegistryTestResourceProvider extends AbstractTestContainersProvider<GenericContainer<?>> {
    public static final String KAFKA_SCHEMA_REGISTRY_URL = "kafka.schema.registry.url";
    public static final String DEFAULT_IMAGE = "confluentinc/cp-schema-registry:8.2.0";
    public static final String DISPLAY_NAME = "Kafka Schema Registry";
    public static final String SIMPLE_NAME = "kafka-schema-registry";
    public static final int PORT = 8081;
    private static final String PROPERTY_ENTRY = "kafka.schema.registry";

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return KafkaServices.resolvablePropertyForUrlRequest(propertyEntries, PROPERTY_ENTRY, KAFKA_SCHEMA_REGISTRY_URL);
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return Collections.singletonList(PROPERTY_ENTRY);
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
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
    protected GenericContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        KafkaContainer kafka = KafkaServices.startKafka(requestedProperties, testResourcesConfig);
        return new GenericContainer<>(imageName)
            .withNetwork(KafkaServices.network(requestedProperties))
            .withExposedPorts(PORT)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + PORT)
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", KafkaServices.KAFKA_INTERNAL_BOOTSTRAP_SERVERS_WITH_PROTOCOL)
            .dependsOn(kafka)
            .waitingFor(Wait.forHttp("/subjects").forPort(PORT));
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, GenericContainer<?> container) {
        return Optional.of(KafkaServices.httpUrl(container, PORT));
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return KAFKA_SCHEMA_REGISTRY_URL.equals(propertyName);
    }
}
