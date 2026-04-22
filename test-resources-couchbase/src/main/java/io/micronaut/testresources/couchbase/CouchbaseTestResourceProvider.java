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
package io.micronaut.testresources.couchbase;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which will spawn a Couchbase test container.
 */
public class CouchbaseTestResourceProvider extends AbstractTestContainersProvider<CouchbaseContainer> {
    public static final String COUCHBASE_URI = "couchbase.uri";
    public static final String COUCHBASE_USERNAME = "couchbase.username";
    public static final String COUCHBASE_PASSWORD = "couchbase.password";
    public static final String DEFAULT_IMAGE = "couchbase/server";
    public static final String SIMPLE_NAME = "couchbase";
    public static final String DISPLAY_NAME = "Couchbase";

    private static final String CONTAINERS_PREFIX = "containers.";
    private static final List<String> SUPPORTED_PROPERTIES_LIST = List.of(
        COUCHBASE_URI,
        COUCHBASE_USERNAME,
        COUCHBASE_PASSWORD
    );
    private static final Set<String> SUPPORTED_PROPERTIES = Set.copyOf(SUPPORTED_PROPERTIES_LIST);

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        Set<String> explicitGenericMappings = explicitGenericMappings(testResourcesConfig);
        return SUPPORTED_PROPERTIES_LIST.stream()
            .filter(property -> !explicitGenericMappings.contains(property))
            .toList();
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
    protected CouchbaseContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new CouchbaseContainer(imageName);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, CouchbaseContainer container) {
        return switch (propertyName) {
            case COUCHBASE_URI -> Optional.of(container.getConnectionString());
            case COUCHBASE_USERNAME -> Optional.of(container.getUsername());
            case COUCHBASE_PASSWORD -> Optional.of(container.getPassword());
            default -> Optional.empty();
        };
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES.contains(propertyName) && !explicitGenericMappings(testResourcesConfig).contains(propertyName);
    }

    private static Set<String> explicitGenericMappings(Map<String, Object> testResourcesConfig) {
        Set<String> mappedProperties = new LinkedHashSet<>();
        for (String containerName : configuredContainerNames(testResourcesConfig)) {
            String prefix = CONTAINERS_PREFIX + containerName + ".";
            Object imageName = testResourcesConfig.get(prefix + "image-name");
            if (imageName == null || String.valueOf(imageName).isBlank()) {
                continue;
            }
            mappedProperties.addAll(extractHostNames(prefix, testResourcesConfig));
            mappedProperties.addAll(extractExposedPortProperties(prefix, testResourcesConfig));
        }
        return mappedProperties;
    }

    private static Set<String> configuredContainerNames(Map<String, Object> testResourcesConfig) {
        Set<String> containerNames = new LinkedHashSet<>();
        for (String key : testResourcesConfig.keySet()) {
            if (!key.startsWith(CONTAINERS_PREFIX)) {
                continue;
            }
            String remainder = key.substring(CONTAINERS_PREFIX.length());
            int dot = remainder.indexOf('.');
            if (dot > 0) {
                containerNames.add(remainder.substring(0, dot));
            }
        }
        return containerNames;
    }

    private static Set<String> extractHostNames(String prefix, Map<String, Object> testResourcesConfig) {
        Object hostNames = testResourcesConfig.get(prefix + "hostnames");
        if (hostNames instanceof List<?> list) {
            Set<String> values = new LinkedHashSet<>(list.size());
            for (Object value : list) {
                values.add(String.valueOf(value));
            }
            return values;
        }
        if (hostNames != null) {
            return Set.of(String.valueOf(hostNames));
        }
        return Set.of();
    }

    private static Set<String> extractExposedPortProperties(String prefix, Map<String, Object> testResourcesConfig) {
        Object exposedPorts = testResourcesConfig.get(prefix + "exposed-ports");
        if (!(exposedPorts instanceof List<?> list)) {
            return Set.of();
        }
        List<String> propertyNames = new ArrayList<>();
        for (Object definition : list) {
            if (definition instanceof Map<?, ?> mappings) {
                propertyNames.addAll(mappings.keySet().stream().map(String::valueOf).toList());
            }
        }
        return new LinkedHashSet<>(propertyNames);
    }
}
