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

import io.micronaut.testresources.jdbc.AbstractJdbcTestResourceProvider;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A test resource provider which will spawn a MySQL test container.
 */
public class MySQLTestResourceProvider extends AbstractJdbcTestResourceProvider<MySQLContainer<?>> {
    public static final String DISPLAY_NAME = "MySQL";
    public static final String MYSQL_OFFICIAL_IMAGE = "container-registry.oracle.com/mysql/community-server";
    public static final String DOCKER_OFFICIAL_IMAGE = "mysql";
    public static final String SIMPLE_NAME = MySQLContainer.NAME;
    public static final String X_PROTOCOL_URL = "x-protocol-url";
    public static final int DEFAULT_X_PROTOCOL_PORT = 33060;

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return MYSQL_OFFICIAL_IMAGE;
    }

    @Override
    protected MySQLContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        // Testcontainers uses by default the Docker Hub official image, so the MySQL official image needs to be set as compatible substitute
        if (imageName.asCanonicalNameString().startsWith(MYSQL_OFFICIAL_IMAGE)) {
            imageName = imageName.asCompatibleSubstituteFor(DOCKER_OFFICIAL_IMAGE);
        }
        return new MySQLContainer<>(imageName);
    }

    @Override
    protected void configureContainer(MySQLContainer<?> container, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        container.withExposedPorts(MySQLContainer.MYSQL_PORT, DEFAULT_X_PROTOCOL_PORT);
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        List<String> resolvableProperties = new ArrayList<>(super.getResolvableProperties(propertyEntries, testResourcesConfig));
        Collection<String> datasources = propertyEntries.getOrDefault(PREFIX, Collections.emptyList());
        List<String> properties = datasources.stream()
            .map(ds -> PREFIX + "." + ds + "." + X_PROTOCOL_URL)
            .toList();
        resolvableProperties.addAll(properties);
        return resolvableProperties;
    }

    @Override
    protected String resolveDbSpecificProperty(String propertyName, JdbcDatabaseContainer<?> container) {
        if (X_PROTOCOL_URL.equals(propertyName.substring(propertyName.lastIndexOf(".") + 1))) {
            String username = container.getUsername();
            String password = container.getPassword();
            String host = container.getHost();
            String port = String.valueOf(container.getMappedPort(DEFAULT_X_PROTOCOL_PORT));
            String schema = container.getDatabaseName();

            return "mysqlx://%s:%s@%s:%s/%s".formatted(username, password, host, port, schema);
        } else {
            return super.resolveDbSpecificProperty(propertyName, container);
        }
    }
}
