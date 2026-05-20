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

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeLabels;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves RabbitMQ properties from Docker Compose services.
 */
public final class RabbitMQComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    public RabbitMQComposeTestResourcesProvider() {
        super("rabbitmq", Set.of("rabbitmq", "rabbit"), 5672, List.of("rabbitmq.uri", "rabbitmq.username", "rabbitmq.password"));
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return switch (context.propertyName()) {
            case "rabbitmq.uri" -> "amqp://" + context.hostPort();
            case "rabbitmq.username" -> context.labelOrEnvironment(ComposeLabels.USERNAME, "RABBITMQ_DEFAULT_USER", "guest");
            case "rabbitmq.password" -> context.labelOrEnvironment(ComposeLabels.PASSWORD, "RABBITMQ_DEFAULT_PASS", "guest");
            default -> null;
        };
    }
}
