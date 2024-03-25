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
package io.micronaut.testresources.opensearch;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn an OpenSearch test container.
 */
public class OpenSearchTestResourceProvider extends AbstractTestContainersProvider<OpensearchContainer<?>> {

    public static final String SIMPLE_NAME = "opensearch";
    public static final String DEFAULT_IMAGE = "opensearchproject/opensearch";
    public static final String DISPLAY_NAME = "OpenSearch";
    public static final String MICRONAUT_OPEN_SEARCH_REST_CLIENT_HTTP_HOSTS = "micronaut.opensearch.rest-client.http-hosts";
    public static final String MICRONAUT_OPEN_SEARCH_HTTPCLIENT5_HTTP_HOSTS = "micronaut.opensearch.httpclient5.http-hosts";
    public static final List<String> RESOLVABLE_PROPERTIES = List.of(
        MICRONAUT_OPEN_SEARCH_REST_CLIENT_HTTP_HOSTS,
        MICRONAUT_OPEN_SEARCH_HTTPCLIENT5_HTTP_HOSTS
    );

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES;
    }

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
    @SuppressWarnings("resource") // The container is long-lived and closed elsewhere
    protected OpensearchContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new OpensearchContainer<>(imageName)
            .withAccessToHost(true); // Necessary for host address and startup checks.
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, OpensearchContainer<?> container) {
        if (RESOLVABLE_PROPERTIES.contains(propertyName)) {
            return Optional.of(container.getHttpHostAddress());
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES.contains(propertyName);
    }
}
