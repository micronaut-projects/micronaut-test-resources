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
import io.micronaut.testresources.core.TestResourcesResolutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.math.BigDecimal;


/**
 * A test resource provider which will spawn a Kafka test container.
 */
public class KafkaTestResourceProvider extends AbstractTestContainersProvider<KafkaContainer> {

    public static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
    public static final String KAFKA_TOPICS = "containers.kafka.topics";
    public static final String KAFKA_PARTITIONS = "containers.kafka.partitions";
    public static final String DEFAULT_IMAGE = "apache/kafka:4.1.1";
    public static final int DEFAULT_PARTITIONS = 1;

    public static final String DISPLAY_NAME = "Kafka";
    public static final String SIMPLE_NAME = "kafka";
    private static final long ADMIN_TIMEOUT_SECONDS = 30;

    private final Object topicProvisioningMonitor = new Object();
    private final Map<KafkaContainer, TopicProvisioningConfiguration> topicProvisioningConfigurations =
        Collections.synchronizedMap(new WeakHashMap<>());

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
        KafkaContainer container = new KafkaContainer(imageName);
        topicProvisioningConfigurations.put(container, topicProvisioningConfiguration(testResourcesConfig));
        return container;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, KafkaContainer container) {
        ensureTopicsExist(container);
        return Optional.of(container.getBootstrapServers());
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return KAFKA_BOOTSTRAP_SERVERS.equals(propertyName);
    }

    private void ensureTopicsExist(KafkaContainer container) {
        TopicProvisioningConfiguration configuration = topicProvisioningConfigurations.get(container);
        if (configuration == null || configuration.provisioned() || configuration.topics().isEmpty()) {
            return;
        }
        synchronized (topicProvisioningMonitor) {
            TopicProvisioningConfiguration currentConfiguration = topicProvisioningConfigurations.get(container);
            if (currentConfiguration == null || currentConfiguration.provisioned() || currentConfiguration.topics().isEmpty()) {
                return;
            }
            provisionTopics(container, currentConfiguration);
            topicProvisioningConfigurations.put(container, currentConfiguration.markProvisioned());
        }
    }

    private void provisionTopics(KafkaContainer container, TopicProvisioningConfiguration configuration) {
        Properties adminClientConfiguration = new Properties();
        adminClientConfiguration.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(adminClientConfiguration)) {
            Set<String> existingTopics = adminClient.listTopics().names().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<NewTopic> topicsToCreate = configuration.topics().stream()
                .filter(topic -> !existingTopics.contains(topic))
                .map(topic -> new NewTopic(topic, configuration.partitions(), (short) 1))
                .toList();
            if (topicsToCreate.isEmpty()) {
                return;
            }
            var createTopicsResult = adminClient.createTopics(topicsToCreate);
            for (NewTopic topic : topicsToCreate) {
                try {
                    createTopicsResult.values().get(topic.name()).get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof TopicExistsException)) {
                        throw e;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TestResourcesResolutionException("Interrupted while provisioning Kafka topics " + topicProvisioningDetails(configuration), e);
        } catch (ExecutionException e) {
            throw new TestResourcesResolutionException("Failed to provision Kafka topics " + topicProvisioningDetails(configuration), e);
        } catch (TimeoutException e) {
            throw new TestResourcesResolutionException(
                "Timed out after " + ADMIN_TIMEOUT_SECONDS + "s while provisioning Kafka topics " + topicProvisioningDetails(configuration),
                e
            );
        }
    }

    private static String topicProvisioningDetails(TopicProvisioningConfiguration configuration) {
        return "[topics=" + configuration.topics() + ", partitions=" + configuration.partitions() + "]";
    }

    private TopicProvisioningConfiguration topicProvisioningConfiguration(Map<String, Object> testResourcesConfig) {
        List<String> topics = configuredTopics(testResourcesConfig);
        if (topics.isEmpty()) {
            return new TopicProvisioningConfiguration(topics, DEFAULT_PARTITIONS, false);
        }
        return new TopicProvisioningConfiguration(topics, configuredPartitions(testResourcesConfig), false);
    }

    static List<String> configuredTopics(Map<String, Object> testResourcesConfig) {
        Object configuredTopics = testResourcesConfig.get(KAFKA_TOPICS);
        if (configuredTopics == null) {
            return Collections.emptyList();
        }
        List<String> rawTopics;
        if (configuredTopics instanceof List<?> list) {
            rawTopics = list.stream().map(String::valueOf).toList();
        } else {
            rawTopics = Collections.singletonList(String.valueOf(configuredTopics));
        }
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        for (String rawTopic : rawTopics) {
            String topic = rawTopic.trim();
            if (topic.isEmpty()) {
                throw new IllegalArgumentException("Kafka topic names must not be blank");
            }
            topics.add(topic);
        }
        return List.copyOf(topics);
    }

    static int configuredPartitions(Map<String, Object> testResourcesConfig) {
        Object configuredPartitions = testResourcesConfig.get(KAFKA_PARTITIONS);
        int partitions = configuredPartitions == null ? DEFAULT_PARTITIONS : asInteger(configuredPartitions, KAFKA_PARTITIONS);
        if (partitions < 1) {
            throw new IllegalArgumentException("Kafka topic partitions must be greater than or equal to 1");
        }
        return partitions;
    }

    private static int asInteger(Object value, String propertyName) {
        if (value instanceof Number number) {
            try {
                return new BigDecimal(String.valueOf(number)).intValueExact();
            } catch (ArithmeticException | NumberFormatException e) {
                throw new IllegalArgumentException("Kafka topic property '" + propertyName + "' must be an integer", e);
            }
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Kafka topic property '" + propertyName + "' must be an integer", e);
        }
    }

    private record TopicProvisioningConfiguration(List<String> topics, int partitions, boolean provisioned) {
        private TopicProvisioningConfiguration markProvisioned() {
            return new TopicProvisioningConfiguration(topics, partitions, true);
        }
    }
}
