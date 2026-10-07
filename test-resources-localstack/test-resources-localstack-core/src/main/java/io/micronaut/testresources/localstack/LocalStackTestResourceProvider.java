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

import io.micronaut.testresources.core.DefaultTestResourceImages;
import io.micronaut.testresources.aws.AbstractAwsTestResourceProvider;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * A test resource provider which will spawn LocalStack test containers.
 */
public class LocalStackTestResourceProvider extends AbstractAwsTestResourceProvider<LocalStackContainer, LocalStackService> {

    public static final String DISPLAY_NAME = "LocalStack";

    private static final String DEFAULT_IMAGE = DefaultTestResourceImages.DEFAULT_LOCALSTACK_IMAGE;
    private static final String NAME = "localstack";

    public LocalStackTestResourceProvider() {
        super(LocalStackService.class);
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    protected LocalStackContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        LocalStackContainer localStackContainer = new LocalStackContainer(imageName);
        localStackContainer.withServices(getServices().stream().map(LocalStackService::getServiceKind).toArray(String[]::new));
        return localStackContainer;
    }

    @Override
    protected String resolveAccessKey(LocalStackContainer container) {
        return container.getAccessKey();
    }

    @Override
    protected String resolveSecretKey(LocalStackContainer container) {
        return container.getSecretKey();
    }

    @Override
    protected String resolveRegion(LocalStackContainer container) {
        return container.getRegion();
    }
}
