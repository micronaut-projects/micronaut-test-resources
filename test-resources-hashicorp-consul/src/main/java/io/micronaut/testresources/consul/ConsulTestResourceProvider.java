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
package io.micronaut.testresources.consul;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

/**
 * A test resource provider which will spawn a Consul test container.
 */
public class ConsulTestResourceProvider extends AbstractTestContainersProvider<ConsulContainer> {

    public static final String PREFIX = "consul.client";
    public static final String PROPERTY_CONSUL_CLIENT_HOST = "consul.client.host";
    public static final String PROPERTY_CONSUL_CLIENT_PORT = "consul.client.port";
    public static final String PROPERTY_CONSUL_CLIENT_DEFAULT_ZONE = "consul.client.default-zone";

    public static final List<String> RESOLVABLE_PROPERTIES_LIST =
        Collections.unmodifiableList(Arrays.asList(
            PROPERTY_CONSUL_CLIENT_HOST,
            PROPERTY_CONSUL_CLIENT_PORT,
            PROPERTY_CONSUL_CLIENT_DEFAULT_ZONE
        ));

    public static final String HASHICORP_CONSUL_KV_PROPERTIES_KEY =
        "containers.hashicorp-consul.kv-properties";
    public static final String DEFAULT_IMAGE = "hashicorp/consul";
    public static final int CONSUL_HTTP_PORT = 8500;
    public static final String SIMPLE_NAME = "hashicorp-consul";
    public static final String DISPLAY_NAME = "Consul";

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries,
                                                Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES_LIST;
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
    protected ConsulContainer createContainer(DockerImageName imageName,
                                              Map<String, Object> requestedProperties,
                                              Map<String, Object> testResourcesConfig) {
        ConsulContainer consulContainer = new ConsulContainer(imageName);

        // We intentionally do NOT set port bindings here, allowing Testcontainers to assign a random port

        // Set startup properties
        if (testResourcesConfig.containsKey(HASHICORP_CONSUL_KV_PROPERTIES_KEY)) {
            @SuppressWarnings("unchecked")
            List<String> properties =
                (List<String>) testResourcesConfig.get(HASHICORP_CONSUL_KV_PROPERTIES_KEY);
            if (null != properties && !properties.isEmpty()) {
                properties.forEach((property) -> consulContainer.withConsulCommand(
                    "kv put " + property.replace("=", " ")));
            }
        }
        return consulContainer;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, ConsulContainer container) {
        if (propertyName != null && propertyName.startsWith(PREFIX)) {
            // Resolve the property
            switch (propertyName) {
                case PROPERTY_CONSUL_CLIENT_HOST -> {
                    return Optional.of(container.getHost());
                }
                case PROPERTY_CONSUL_CLIENT_PORT -> {
                    return Optional.of(container.getMappedPort(CONSUL_HTTP_PORT).toString());
                }
                case PROPERTY_CONSUL_CLIENT_DEFAULT_ZONE -> {
                    return Optional.of(
                        container.getHost() + ":" + container.getMappedPort(CONSUL_HTTP_PORT));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> properties,
                                   Map<String, Object> testResourcesConfig) {
        return propertyName != null && propertyName.startsWith(PREFIX);
    }
}
