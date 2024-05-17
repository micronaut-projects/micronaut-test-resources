/*
 * Copyright 2017-2024 original authors
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

import java.util.Map;

/**
 * Internal class to deal with the test resources
 * kafka configuration block.
 */
abstract class KafkaConfigurationSupport {
    public static final String CONFIG_KAFKA_KRAFT_MODE = "containers.kafka.kraft";

    private KafkaConfigurationSupport() {

    }

    /**
     * Start Kafka container in Kraft mode with the confluent-local image.
     * See: <a href="https://docs.confluent.io/platform/current/installation/docker/image-reference.html#ak-images">Confluent Kafka Images</a>
     * See: <a href="https://java.testcontainers.org/modules/kafka/#using-kraft-mode">TestContainers Kafka Kraft Mode</a>
     */
    static boolean isKraftMode(Map<String, Object> testResourcesConfig) {
        Boolean clusterMode = (Boolean) testResourcesConfig.getOrDefault(CONFIG_KAFKA_KRAFT_MODE, false);
        return Boolean.TRUE.equals(clusterMode);
    }
}
