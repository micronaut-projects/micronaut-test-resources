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

abstract class AbstractKafkaServiceTestResourceProvider extends AbstractTestContainersProvider<GenericContainer<?>> {
    private final String displayName;
    private final String simpleName;
    private final String defaultImageName;
    private final String propertyEntry;
    private final String propertyName;
    private final int port;
    private final String readinessPath;

    AbstractKafkaServiceTestResourceProvider(String displayName,
                                             String simpleName,
                                             String defaultImageName,
                                             String propertyEntry,
                                             String propertyName,
                                             int port,
                                             String readinessPath) {
        this.displayName = displayName;
        this.simpleName = simpleName;
        this.defaultImageName = defaultImageName;
        this.propertyEntry = propertyEntry;
        this.propertyName = propertyName;
        this.port = port;
        this.readinessPath = readinessPath;
    }

    @Override
    public final List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return KafkaServices.resolvablePropertyForUrlRequest(propertyEntries, propertyEntry, propertyName);
    }

    @Override
    public final List<String> getRequiredPropertyEntries() {
        return Collections.singletonList(propertyEntry);
    }

    @Override
    public final String getDisplayName() {
        return displayName;
    }

    @Override
    protected final String getSimpleName() {
        return simpleName;
    }

    @Override
    protected final String getDefaultImageName() {
        return defaultImageName;
    }

    @Override
    @SuppressWarnings("java:S2095") // AbstractTestContainersProvider owns and closes the returned container.
    protected final GenericContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        KafkaContainer kafka = KafkaServices.startKafka(requestedProperties, testResourcesConfig);
        GenericContainer<?> container = new GenericContainer<>(imageName)
            .withNetwork(KafkaServices.network(requestedProperties))
            .withExposedPorts(port)
            .dependsOn(kafka)
            .waitingFor(Wait.forHttp(readinessPath).forPort(port));
        configureService(container);
        return container;
    }

    @Override
    protected final Optional<String> resolveProperty(String propertyName, GenericContainer<?> container) {
        return Optional.of(KafkaServices.httpUrl(container, port));
    }

    @Override
    protected final boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return this.propertyName.equals(propertyName);
    }

    protected abstract void configureService(GenericContainer<?> container);
}
