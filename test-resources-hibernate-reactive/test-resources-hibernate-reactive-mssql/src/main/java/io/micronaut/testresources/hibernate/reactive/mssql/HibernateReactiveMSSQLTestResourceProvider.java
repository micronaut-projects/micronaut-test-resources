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
package io.micronaut.testresources.hibernate.reactive.mssql;

import io.micronaut.testresources.hibernate.reactive.core.AbstractHibernateReactiveTestResourceProvider;
import io.micronaut.testresources.mssql.MSSQLTestResourceProvider;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static io.micronaut.testresources.mssql.MSSQLTestResourceProvider.createMSSQLContainer;

/**
 * A test resource provider which will spawn a MSSQL test container.
 */
public class HibernateReactiveMSSQLTestResourceProvider extends AbstractHibernateReactiveTestResourceProvider<MSSQLServerContainer<?>> {
    public static final String DISPLAY_NAME = "MSSQL (Hibernate reactive)";

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return "mssql";
    }

    @Override
    protected String getDefaultImageName() {
        return MSSQLTestResourceProvider.DEFAULT_IMAGE_NAME;
    }

    @Override
    protected MSSQLServerContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return createMSSQLContainer(imageName, getSimpleName(), testResourcesConfig);
    }

}
