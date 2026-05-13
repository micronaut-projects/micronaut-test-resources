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

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves RabbitMQ properties from Docker Compose services.
 */
public final class RabbitMQComposeTestResourceProvider extends RabbitMQTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor RABBITMQ =
        new ComposeResolverSupport.ServiceDescriptor("rabbitmq", List.of(), 5672);

    @Override
    public String getDisplayName() {
        return "Docker Compose RabbitMQ";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            RABBITMQ,
            context -> true,
            context -> switch (propertyName) {
                case RABBITMQ_URI -> "amqp://" + username(context) + ":" + password(context) + "@" + context.hostPort();
                case RABBITMQ_USERNAME -> username(context);
                case RABBITMQ_PASSWORD -> password(context);
                default -> null;
            });
    }

    private static String username(ComposeResolverSupport.ResolutionContext context) {
        return context.labelOrEnvironment(ComposeResolverSupport.USERNAME_LABEL, "RABBITMQ_DEFAULT_USER", "guest");
    }

    private static String password(ComposeResolverSupport.ResolutionContext context) {
        return context.labelOrEnvironment(ComposeResolverSupport.PASSWORD_LABEL, "RABBITMQ_DEFAULT_PASS", "guest");
    }
}
