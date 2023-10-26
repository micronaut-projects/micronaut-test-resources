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

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A test resource provider which will spawn LocalStack test containers.
 */
public class LocalStackTestResourceProvider extends AbstractTestContainersProvider<LocalStackContainer> {

    private static final String DEFAULT_IMAGE = "localstack/localstack";
    private static final String NAME = "localstack";

    private static final String AWS_ACCESS_KEY_ID = "aws.access-key-id";
    private static final String AWS_SECRET_KEY = "aws.secret-key";
    private static final String AWS_REGION = "aws.region";

    private static final List<String> COMMON_PROPERTIES;

    private static final Map<LocalStackContainer.Service, List<String>> RESOLVABLE_PROPERTIES;
    private static final Set<String> ALL_SUPPORTED_KEYS;
    private static final List<LocalStackService> SERVICES;
    private static final Map<String, LocalStackService> PROPERTY_TO_SERVICE;
    public static final String DISPLAY_NAME = "LocalStack";

    static {
        SERVICES = StreamSupport.stream(ServiceLoader.load(LocalStackService.class).spliterator(), false)
                .collect(Collectors.toList());
        Map<LocalStackContainer.Service, List<String>> resolvableProperties = new EnumMap<>(LocalStackContainer.Service.class);
        Map<String, LocalStackService> propertyToService = new HashMap<>();
        COMMON_PROPERTIES = Collections.unmodifiableList(Arrays.asList(
            AWS_ACCESS_KEY_ID,
            AWS_SECRET_KEY,
            AWS_REGION
        ));
        for (LocalStackService localStackService : SERVICES) {
            List<String> supportedProperties = localStackService.getResolvableProperties();
            resolvableProperties.put(localStackService.getServiceKind(), supportedProperties);
            for (String supportedProperty : supportedProperties) {
                propertyToService.put(supportedProperty, localStackService);
            }
        }
        RESOLVABLE_PROPERTIES = Collections.unmodifiableMap(resolvableProperties);
        ALL_SUPPORTED_KEYS = Stream.concat(
            COMMON_PROPERTIES.stream(),
            RESOLVABLE_PROPERTIES.values().stream().flatMap(Collection::stream)
        ).collect(Collectors.toSet());
        PROPERTY_TO_SERVICE = Collections.unmodifiableMap(propertyToService);
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return Stream.concat(
                SERVICES.stream().flatMap(service -> service.getResolvableProperties().stream()),
                COMMON_PROPERTIES.stream()
            ).distinct()
            .collect(Collectors.toList());
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
        localStackContainer.withServices(SERVICES.stream().map(LocalStackService::getServiceKind).toArray(LocalStackContainer.Service[]::new));
        return localStackContainer;
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return ALL_SUPPORTED_KEYS.contains(propertyName);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, LocalStackContainer container) {
        switch (propertyName) {
            case AWS_ACCESS_KEY_ID:
                return Optional.of(container.getAccessKey());
            case AWS_SECRET_KEY:
                return Optional.of(container.getSecretKey());
            case AWS_REGION:
                return Optional.of(container.getRegion());
            default:
                LocalStackService service = PROPERTY_TO_SERVICE.get(propertyName);
                if (service != null) {
                    return service.resolveProperty(propertyName, container);
                }
        }
        return Optional.empty();
    }
}
