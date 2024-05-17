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
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

import static io.micronaut.testresources.kafka.KafkaConfigurationSupport.isKraftMode;

/**
 * A test resource provider which will spawn a Kafka test container.
 */
public class KafkaTestResourceProvider extends AbstractTestContainersProvider<KafkaContainer> {

    public static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
    public static final String DEFAULT_IMAGE = "confluentinc/cp-kafka:7.0.4";
    /**
     * Leverage confluent-local image as it is optimized for local development and the image enables
     * KRaft mode with no configuration setup.
     * See: <a href="https://docs.confluent.io/platform/current/installation/docker/image-reference.html#ak-images">Confluent Kafka Images</a>
     */
    public static final String DEFAULT_KRAFT_IMAGE = "confluentinc/confluent-local:7.6.0";

    public static final String DISPLAY_NAME = "Kafka";
    public static final String SIMPLE_NAME = "kafka";
    public static final List<String> SUPPORTED_PROPERTIES_LIST = List.of(KAFKA_BOOTSTRAP_SERVERS);

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return Collections.singletonList(KAFKA_BOOTSTRAP_SERVERS);
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
    protected KafkaContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        if (isKraftMode(testResourcesConfig)) {
            return new KafkaContainer(DockerImageName.parse(DEFAULT_KRAFT_IMAGE).asCompatibleSubstituteFor("confluentinc/cp-kafka")).withKraft();
        } else {
            return new KafkaContainer(imageName);
        }
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, KafkaContainer container) {
        return Optional.of(container.getBootstrapServers());
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return KAFKA_BOOTSTRAP_SERVERS.equals(propertyName);
    }
}
