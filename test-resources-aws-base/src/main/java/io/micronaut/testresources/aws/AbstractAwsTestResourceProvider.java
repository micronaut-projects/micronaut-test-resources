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
package io.micronaut.testresources.aws;

import io.micronaut.core.annotation.Internal;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
 * Base provider for AWS emulator test resources.
 * @param <C> the container type
 * @param <S> the service type
 */
@Internal
public abstract class AbstractAwsTestResourceProvider<C extends GenericContainer<? extends C>, S extends AwsEmulatorService<C>>
    extends AbstractTestContainersProvider<C> {

    private static final String AWS_ACCESS_KEY_ID = "aws.access-key-id";
    private static final String AWS_SECRET_KEY = "aws.secret-key";
    private static final String AWS_REGION = "aws.region";
    private static final List<String> COMMON_PROPERTIES = Collections.unmodifiableList(Arrays.asList(
        AWS_ACCESS_KEY_ID,
        AWS_SECRET_KEY,
        AWS_REGION
    ));

    private final List<S> services;
    private final Set<String> allSupportedKeys;
    private final Map<String, S> propertyToService;

    protected AbstractAwsTestResourceProvider(Class<S> serviceType) {
        services = StreamSupport.stream(ServiceLoader.load(serviceType).spliterator(), false)
            .toList();
        Map<String, S> propertyToService = new HashMap<>();
        for (S service : services) {
            for (String supportedProperty : service.getResolvableProperties()) {
                propertyToService.put(supportedProperty, service);
            }
        }
        this.propertyToService = Collections.unmodifiableMap(propertyToService);
        allSupportedKeys = Stream.concat(
            COMMON_PROPERTIES.stream(),
            services.stream().map(AwsEmulatorService::getResolvableProperties).flatMap(Collection::stream)
        ).collect(Collectors.toSet());
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return Stream.concat(
                services.stream().flatMap(service -> service.getResolvableProperties().stream()),
                COMMON_PROPERTIES.stream()
            ).distinct()
            .toList();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return allSupportedKeys.contains(propertyName);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, C container) {
        switch (propertyName) {
            case AWS_ACCESS_KEY_ID:
                return Optional.of(resolveAccessKey(container));
            case AWS_SECRET_KEY:
                return Optional.of(resolveSecretKey(container));
            case AWS_REGION:
                return Optional.of(resolveRegion(container));
            default:
                S service = propertyToService.get(propertyName);
                if (service != null) {
                    return service.resolveProperty(propertyName, container);
                }
        }
        return Optional.empty();
    }

    protected final List<S> getServices() {
        return services;
    }

    protected final Set<String> getServiceKinds() {
        return services.stream()
            .map(AwsEmulatorService::getServiceKind)
            .collect(Collectors.toSet());
    }

    protected abstract String resolveAccessKey(C container);

    protected abstract String resolveSecretKey(C container);

    protected abstract String resolveRegion(C container);
}
