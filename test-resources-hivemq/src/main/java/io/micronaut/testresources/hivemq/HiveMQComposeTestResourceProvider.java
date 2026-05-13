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
package io.micronaut.testresources.hivemq;

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves HiveMQ properties from Docker Compose services.
 */
public final class HiveMQComposeTestResourceProvider extends HiveMQTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor HIVEMQ =
        new ComposeResolverSupport.ServiceDescriptor("hivemq", List.of("mqtt"), 1883);

    @Override
    public String getDisplayName() {
        return "Docker Compose HiveMQ";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            HIVEMQ,
            context -> true,
            context -> switch (propertyName) {
                case MQTT_CLIENT_CLIENT_ID -> context.labelOrEnvironment("io.micronaut.test-resources.client-id", "MQTT_CLIENT_ID", "micronaut-test-resources-" + UUID.randomUUID());
                case MQTT_CLIENT_SERVER_URI -> "tcp://" + context.hostPort();
                default -> null;
            });
    }
}
