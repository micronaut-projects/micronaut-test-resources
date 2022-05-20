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
    public static final String IMAGE_NAME_PROPERTY = "micronaut.testresources.kafka.image-name";
    public static final String DEFAULT_IMAGE = "confluentinc/cp-kafka:6.2.1";

    @Override
    public List<String> getResolvableProperties() {
        return Collections.singletonList(KAFKA_BOOTSTRAP_SERVERS);
    }

    @Override
    public List<String> getRequiredProperties() {
        return Collections.singletonList(IMAGE_NAME_PROPERTY);
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties) {
        if (KAFKA_BOOTSTRAP_SERVERS.equals(propertyName)) {
            System.out.println("Starting a Kafka test container");
            DockerImageName imageName = DockerImageName.parse(DEFAULT_IMAGE) ;
            if (properties.containsKey(IMAGE_NAME_PROPERTY)) {
                imageName = DockerImageName.parse(String.valueOf(properties.get(IMAGE_NAME_PROPERTY))).asCompatibleSubstituteFor(DEFAULT_IMAGE);
            }
            KafkaContainer kafka = new KafkaContainer(imageName);
            kafka.start();
            return Optional.of(kafka.getBootstrapServers());
        }
        return Optional.empty();
    }
}
