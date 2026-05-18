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
 * A test resource provider which will spawn a Kafka Connect test container.
 */
public class KafkaConnectTestResourceProvider extends AbstractKafkaServiceTestResourceProvider {
    public static final String KAFKA_CONNECT_URL = "kafka.connect.url";
    public static final String DEFAULT_IMAGE = DefaultTestResourceImages.DEFAULT_KAFKA_CONNECT_IMAGE;
    public static final String DISPLAY_NAME = "Kafka Connect";
    public static final String SIMPLE_NAME = "kafka-connect";
    public static final int PORT = 8083;
    private static final String PROPERTY_ENTRY = "kafka.connect";

    public KafkaConnectTestResourceProvider() {
        super(DISPLAY_NAME, SIMPLE_NAME, DEFAULT_IMAGE, PROPERTY_ENTRY, KAFKA_CONNECT_URL, PORT, "/connectors");
    }

    @Override
    protected void configureService(GenericContainer<?> container) {
        container
            .withEnv("CONNECT_BOOTSTRAP_SERVERS", KafkaServices.KAFKA_INTERNAL_BOOTSTRAP_SERVERS)
            .withEnv("CONNECT_GROUP_ID", "test-resources-connect")
            .withEnv("CONNECT_CONFIG_STORAGE_TOPIC", "test-resources-connect-config")
            .withEnv("CONNECT_OFFSET_STORAGE_TOPIC", "test-resources-connect-offsets")
            .withEnv("CONNECT_STATUS_STORAGE_TOPIC", "test-resources-connect-status")
            .withEnv("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE", "false")
            .withEnv("CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE", "false")
            .withEnv("CONNECT_REST_ADVERTISED_HOST_NAME", "connect")
            .withEnv("CONNECT_REST_PORT", String.valueOf(PORT))
            .withEnv("CONNECT_PLUGIN_PATH", "/usr/share/java,/usr/share/confluent-hub-components");
    }
}
