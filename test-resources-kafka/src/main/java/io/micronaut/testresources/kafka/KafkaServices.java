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

import io.micronaut.testresources.core.Scope;
import io.micronaut.testresources.core.TestResourcesResolutionException;
import io.micronaut.testresources.testcontainers.TestContainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class KafkaServices {
    static final String KAFKA_NETWORK_ALIAS = "kafka";
    static final int KAFKA_INTERNAL_PORT = 19092;
    static final String KAFKA_INTERNAL_BOOTSTRAP_SERVERS = KAFKA_NETWORK_ALIAS + ":" + KAFKA_INTERNAL_PORT;
    static final String KAFKA_INTERNAL_BOOTSTRAP_SERVERS_WITH_PROTOCOL = "PLAINTEXT://" + KAFKA_INTERNAL_BOOTSTRAP_SERVERS;

    private KafkaServices() {
    }

    static KafkaContainer configureKafka(KafkaContainer container, Map<String, Object> requestedProperties) {
        return container
            .withNetwork(network(requestedProperties))
            .withNetworkAliases(KAFKA_NETWORK_ALIAS)
            .withListener(KAFKA_INTERNAL_BOOTSTRAP_SERVERS);
    }

    static KafkaContainer startKafka(Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        new KafkaTestResourceProvider().resolve(
            KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS,
            requestedProperties,
            testResourcesConfig
        );
        Scope scope = Scope.from(requestedProperties);
        List<GenericContainer<?>> containers = TestContainers.findByRequestedProperty(
            scope,
            KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS
        );
        return containers.stream()
            .filter(KafkaContainer.class::isInstance)
            .map(KafkaContainer.class::cast)
            .findFirst()
            .orElseThrow(() -> new TestResourcesResolutionException("Kafka container could not be started"));
    }

    static Network network(Map<String, Object> requestedProperties) {
        String scope = Scope.from(requestedProperties).toString();
        return TestContainers.network(scope.isEmpty() ? KAFKA_NETWORK_ALIAS : KAFKA_NETWORK_ALIAS + "-" + scope);
    }

    static String httpUrl(GenericContainer<?> container, int port) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(port);
    }

    static List<String> resolvablePropertyForUrlRequest(Map<String, Collection<String>> propertyEntries,
                                                        String propertyEntry,
                                                        String propertyName) {
        if (propertyEntries.getOrDefault(propertyEntry, Collections.emptySet()).contains("url")) {
            return Collections.singletonList(propertyName);
        }
        return Collections.emptyList();
    }
}
