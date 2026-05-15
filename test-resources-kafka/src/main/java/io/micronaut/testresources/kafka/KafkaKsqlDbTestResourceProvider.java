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

import org.testcontainers.containers.GenericContainer;

/**
 * A test resource provider which will spawn a ksqlDB test container.
 */
public class KafkaKsqlDbTestResourceProvider extends AbstractKafkaServiceTestResourceProvider {
    public static final String KAFKA_KSQLDB_URL = "kafka.ksqldb.url";
    public static final String DEFAULT_IMAGE = "confluentinc/cp-ksqldb-server:8.2.0";
    public static final String DISPLAY_NAME = "Kafka ksqlDB";
    public static final String SIMPLE_NAME = "kafka-ksqldb";
    public static final int PORT = 8088;
    private static final String PROPERTY_ENTRY = "kafka.ksqldb";

    public KafkaKsqlDbTestResourceProvider() {
        super(DISPLAY_NAME, SIMPLE_NAME, DEFAULT_IMAGE, PROPERTY_ENTRY, KAFKA_KSQLDB_URL, PORT, "/info");
    }

    @Override
    protected void configureService(GenericContainer<?> container) {
        container
            .withEnv("KSQL_BOOTSTRAP_SERVERS", KafkaServices.KAFKA_INTERNAL_BOOTSTRAP_SERVERS)
            .withEnv("KSQL_HOST_NAME", "ksqldb")
            .withEnv("KSQL_LISTENERS", "http://0.0.0.0:" + PORT)
            .withEnv("KSQL_KSQL_SERVICE_ID", "test-resources-ksqldb");
    }
}
