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
package io.micronaut.testresources.localstack.dynamodb;

import io.micronaut.testresources.aws.AbstractAwsEndpointService;
import io.micronaut.testresources.localstack.LocalStackService;
import org.testcontainers.localstack.LocalStackContainer;

/**
 * Adds support for Localstack DynamoDB.
 */
public class LocalStackDynamoDBService extends AbstractAwsEndpointService<LocalStackContainer> implements LocalStackService {

    private static final String AWS_DYNAMODB_ENDPOINT_OVERRIDE = "aws.services.dynamodb.endpoint-override";
    private static final String SERVICE = "dynamodb";

    public LocalStackDynamoDBService() {
        super(SERVICE, AWS_DYNAMODB_ENDPOINT_OVERRIDE, container -> container.getEndpoint().toString());
    }
}
