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
package io.micronaut.testresources.mysql;

import io.micronaut.testresources.core.TestResourcesResolver;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which will spawn a MySQL test container.
 */
public class MySQLTestResourceProvider implements TestResourcesResolver {

    public static final String URL = "datasources.default.url";
    public static final String USERNAME = "datasources.default.username";
    public static final String PASSWORD = "datasources.default.password";
    public static final String IMAGE_NAME_PROPERTY = "micronaut.testresources.mysql.image-name";
    public static final String DEFAULT_IMAGE = "mysql";

    private static final List<String> SUPPORTED_LIST = Collections.unmodifiableList(
        Arrays.asList(URL, USERNAME, PASSWORD)
    );
    private static final Set<String> SUPPORTED_SET = Collections.unmodifiableSet(
        new HashSet<>(SUPPORTED_LIST)
    );

    private MySQLContainer<?> container;

    @Override
    public List<String> getResolvableProperties() {
        return SUPPORTED_LIST;
    }

    @Override
    public List<String> getRequiredProperties() {
        return Collections.singletonList(IMAGE_NAME_PROPERTY);
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties) {
        if (SUPPORTED_SET.contains(propertyName)) {
            MySQLContainer<?> container = maybeStartContainer(properties);
            String value = null;
            switch (propertyName) {
                case URL:
                    value = container.getJdbcUrl();
                    break;
                case USERNAME:
                    value = container.getUsername();
                    break;
                case PASSWORD:
                    value = container.getPassword();
                    break;
                default:
            }
            return Optional.ofNullable(value);
        }
        return Optional.empty();
    }

    @NotNull
    private MySQLContainer<?> maybeStartContainer(Map<String, Object> properties) {
        if (container == null) {
            System.out.println("Starting a MySQL test container");
            DockerImageName imageName = DockerImageName.parse(DEFAULT_IMAGE);
            if (properties.containsKey(IMAGE_NAME_PROPERTY)) {
                imageName = DockerImageName.parse(String.valueOf(properties.get(IMAGE_NAME_PROPERTY))).asCompatibleSubstituteFor(DEFAULT_IMAGE);
            }
            container = new MySQLContainer<>(imageName);
            container.start();
        }
        return container;
    }
}
