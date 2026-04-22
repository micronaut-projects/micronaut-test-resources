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
package io.micronaut.testresources.pulsar;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn a Pulsar test container.
 */
public class PulsarTestResourceProvider extends AbstractTestContainersProvider<PulsarContainer> {

    public static final String PULSAR_SERVICE_URL = "pulsar.service-url";
    public static final String DEFAULT_IMAGE = "apachepulsar/pulsar:3.0.16";
    public static final String DISPLAY_NAME = "Pulsar";
    public static final String SIMPLE_NAME = "pulsar";

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return List.of(PULSAR_SERVICE_URL);
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
    protected PulsarContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new PulsarContainer(imageName);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, PulsarContainer container) {
        return Optional.of(container.getPulsarBrokerUrl());
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return PULSAR_SERVICE_URL.equals(propertyName);
    }
}
