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
package io.micronaut.testresources.localstack;

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeLabels;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves LocalStack properties from Docker Compose services.
 */
public final class LocalStackComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    public LocalStackComposeTestResourcesProvider() {
        super("localstack", Set.of("localstack"), 4566, List.of(
            "aws.access-key-id",
            "aws.secret-key",
            "aws.region",
            "aws.services.s3.endpoint-override",
            "aws.services.dynamodb.endpoint-override",
            "aws.services.sqs.endpoint-override",
            "aws.services.sns.endpoint-override"
        ));
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return switch (context.propertyName()) {
            case "aws.access-key-id" -> context.labelOrEnvironment(ComposeLabels.ACCESS_KEY, "AWS_ACCESS_KEY_ID", "test");
            case "aws.secret-key" -> context.labelOrEnvironment(ComposeLabels.SECRET_KEY, "AWS_SECRET_ACCESS_KEY", "test");
            case "aws.region" -> context.labelOrEnvironment("io.micronaut.test-resources.region", "AWS_DEFAULT_REGION", "us-east-1");
            default -> context.http();
        };
    }
}
