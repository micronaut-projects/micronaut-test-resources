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

import io.micronaut.testresources.core.TestResourcesResolver;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn a Kafka test container.
 */
public class KafkaTestResourceProvider implements TestResourcesResolver {

    public static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";

    @Override
    public List<String> getResolvableProperties() {
        return Collections.singletonList(KAFKA_BOOTSTRAP_SERVERS);
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties) {
        if (KAFKA_BOOTSTRAP_SERVERS.equals(propertyName)) {
            System.out.println("Starting a Kafka test container");
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"));
            kafka.start();
            return Optional.of(kafka.getBootstrapServers());
        }
        return Optional.empty();
    }
}
