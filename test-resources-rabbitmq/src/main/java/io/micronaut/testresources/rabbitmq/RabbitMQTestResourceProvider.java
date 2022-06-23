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
package io.micronaut.testresources.rabbitmq;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which will spawn a RabbitMQ test container.
 */
public class RabbitMQTestResourceProvider extends AbstractTestContainersProvider<RabbitMQContainer> {

    public static final String RABBITMQ_URI = "rabbitmq.uri";
    public static final String RABBITMQ_USERNAME = "rabbitmq.username";
    public static final String RABBITMQ_PASSWORD = "rabbitmq.password";
    public static final String DEFAULT_IMAGE = "rabbitmq";

    public static final List<String> SUPPORTED_KEYS = Collections.unmodifiableList(Arrays.asList(
        RABBITMQ_URI,
        RABBITMQ_USERNAME,
        RABBITMQ_PASSWORD
    ));
    private static final Set<String> SUPPORTED_KEYSET = Collections.unmodifiableSet(
        new HashSet<>(SUPPORTED_KEYS)
    );

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_KEYS;
    }

    @Override
    protected String getSimpleName() {
        return "rabbitmq";
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    protected RabbitMQContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        return new RabbitMQContainer(imageName);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, RabbitMQContainer container) {
        switch (propertyName) {
            case RABBITMQ_URI:
                return Optional.of(container.getAmqpUrl());
            case RABBITMQ_USERNAME:
                return Optional.of(container.getAdminUsername());
            case RABBITMQ_PASSWORD:
                return Optional.of(container.getAdminPassword());
            default:
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        return SUPPORTED_KEYSET.contains(propertyName);
    }
}
