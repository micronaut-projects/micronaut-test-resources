/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.testresources.localstack.sns;

import io.micronaut.testresources.localstack.LocalStackService;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Adds support for Localstack SNS.
 */
public class LocalStackSNSService implements LocalStackService {

    private static final String AWS_SNS_ENDPOINT_OVERRIDE = "aws.services.sns.endpoint-override";

    @Override
    public LocalStackContainer.Service getServiceKind() {
        return LocalStackContainer.Service.SNS;
    }

    @Override
    public List<String> getResolvableProperties() {
        return Collections.singletonList(AWS_SNS_ENDPOINT_OVERRIDE);
    }

    @Override
    public Optional<String> resolveProperty(String propertyName, LocalStackContainer container) {
        if (AWS_SNS_ENDPOINT_OVERRIDE.equals(propertyName)) {
            return Optional.of(container.getEndpointOverride(LocalStackContainer.Service.SNS).toString());
        }
        return Optional.empty();
    }
}
