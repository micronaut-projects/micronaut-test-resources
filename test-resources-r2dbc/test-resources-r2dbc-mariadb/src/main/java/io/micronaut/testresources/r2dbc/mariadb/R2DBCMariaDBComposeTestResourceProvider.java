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
package io.micronaut.testresources.r2dbc.mariadb;

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeDatabaseDescriptors;
import io.micronaut.testresources.core.compose.ComposeDatabaseResolverSupport;

import java.util.Map;
import java.util.Optional;

/**
 * Resolves MariaDB R2DBC properties from Docker Compose services.
 */
public final class R2DBCMariaDBComposeTestResourceProvider extends R2DBCMariaDBTestResourceProvider implements ComposeAwareTestResourcesResolver {
    @Override
    public String getDisplayName() {
        return "Docker Compose MariaDB R2DBC";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        Optional<String> compose = ComposeDatabaseResolverSupport.resolveR2dbc(propertyName, properties, testResourcesConfig, ComposeDatabaseDescriptors.MARIADB);
        return compose.isPresent() ? compose : super.resolveWithoutContainer(propertyName, properties, testResourcesConfig);
    }
}
