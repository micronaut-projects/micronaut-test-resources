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
package io.micronaut.testresources.floci.s3;

import io.floci.testcontainers.FlociContainer;
import io.micronaut.testresources.floci.FlociService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Adds support for Floci S3.
 */
public class FlociS3Service implements FlociService {

    private static final String AWS_S3_ENDPOINT_OVERRIDE = "aws.services.s3.endpoint-override";
    private static final String SERVICE = "s3";

    @Override
    public Optional<String> resolveProperty(String propertyName, FlociContainer container) {
        if (AWS_S3_ENDPOINT_OVERRIDE.equals(propertyName)) {
            return Optional.of(container.getEndpoint());
        }
        return Optional.empty();
    }

    @Override
    public String getServiceKind() {
        return SERVICE;
    }

    @Override
    public List<String> getResolvableProperties() {
        return Collections.singletonList(AWS_S3_ENDPOINT_OVERRIDE);
    }
}
