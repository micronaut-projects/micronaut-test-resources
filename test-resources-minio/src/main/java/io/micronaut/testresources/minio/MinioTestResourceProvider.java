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
package io.micronaut.testresources.minio;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * A test resource provider which will spawn a MinIO test container.
 */
public class MinioTestResourceProvider extends AbstractTestContainersProvider<MinIOContainer> {

    public static final String MINIO_URL = "minio.url";
    public static final String MINIO_ACCESS_KEY = "minio.access-key";
    public static final String MINIO_SECRET_KEY = "minio.secret-key";

    public static final List<String> RESOLVABLE_PROPERTIES = List.of(
        MINIO_URL,
        MINIO_ACCESS_KEY,
        MINIO_SECRET_KEY
    );

    public static final String DEFAULT_IMAGE = "minio/minio";
    public static final String SIMPLE_NAME = "minio";
    public static final String DISPLAY_NAME = "MinIO";


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
        return DEFAULT_IMAGE;
    }

    @Override
    protected MinIOContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new MinIOContainer(imageName);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, MinIOContainer container) {
        if (MINIO_URL.equals(propertyName)) {
            return Optional.of(container.getS3URL());
        }
        if (MINIO_ACCESS_KEY.equals(propertyName)) {
            return Optional.of(container.getUserName());
        }
        if (MINIO_SECRET_KEY.equals(propertyName)) {
            return Optional.of(container.getPassword());
        }
        return Optional.empty();
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES;
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES.contains(propertyName);
    }

}
