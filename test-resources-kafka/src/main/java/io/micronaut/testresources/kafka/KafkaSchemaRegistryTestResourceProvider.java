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

import io.micronaut.testresources.core.DefaultTestResourceImages;
import org.testcontainers.containers.GenericContainer;

/**
 * A test resource provider which will spawn a Schema Registry test container.
 */
public class KafkaSchemaRegistryTestResourceProvider extends AbstractKafkaServiceTestResourceProvider {
    public static final String KAFKA_SCHEMA_REGISTRY_URL = "kafka.schema.registry.url";
    public static final String DEFAULT_IMAGE = DefaultTestResourceImages.DEFAULT_KAFKA_SCHEMA_REGISTRY_IMAGE;
    public static final String DISPLAY_NAME = "Kafka Schema Registry";
    public static final String SIMPLE_NAME = "kafka-schema-registry";
    public static final int PORT = 8081;
    private static final String PROPERTY_ENTRY = "kafka.schema.registry";

    public KafkaSchemaRegistryTestResourceProvider() {
        super(DISPLAY_NAME, SIMPLE_NAME, DEFAULT_IMAGE, PROPERTY_ENTRY, KAFKA_SCHEMA_REGISTRY_URL, PORT, "/subjects");
    }

    @Override
    protected void configureService(GenericContainer<?> container) {
        container
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + PORT)
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", KafkaServices.KAFKA_INTERNAL_BOOTSTRAP_SERVERS_WITH_PROTOCOL);
    }
}
