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
package io.micronaut.testresources.testcontainers;

import io.micronaut.testresources.core.TestResourcesResolver;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The base class for test resources providers which spawn test containers.
 *
 * @param <T> the container type
 */
public abstract class AbstractTestContainersProvider<T extends GenericContainer<? extends T>> implements TestResourcesResolver {

    public static final String IMAGE_NAME_PROPERTY = "image-name";

    /**
     * Returns the name of the resource resolver, for example "kafka" or "mysql".
     *
     * @return the name of the resolver
     */
    protected abstract String getSimpleName();

    protected final String getNamespace() {
        return "micronaut.testresources." + getSimpleName();
    }

    protected final String getImageNameProperty() {
        return getNamespace() + "." + IMAGE_NAME_PROPERTY;
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        return Collections.singletonList(getImageNameProperty());
    }

    /**
     * Returns the default image name.
     *
     * @return the default image name.
     */
    protected abstract String getDefaultImageName();

    /**
     * Creates the test container.
     *
     * @param imageName the docker image name
     * @param properties the resolved properties
     * @return a container
     */
    protected abstract T createContainer(DockerImageName imageName, Map<String, Object> properties);

    /**
     * Determines if this resolver can resolve the requested property.
     * It is used in order to make sure that a "Postgres" resolver wouldn't
     * provide a value if the requested container type is for MySQL, for
     * example.
     *
     * @param propertyName the property to resolve
     * @param properties the resolved properties
     * @return if this resolver should answer
     */
    protected boolean shouldAnswer(String propertyName, Map<String, Object> properties) {
        return true;
    }

    @Override
    public final Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
        if (shouldAnswer(propertyName, properties)) {
            return resolveProperty(propertyName,
                TestContainers.getOrCreate(this.getClass(), getSimpleName(), properties, () -> {
                    String defaultImageName = getDefaultImageName();
                    DockerImageName imageName = DockerImageName.parse(defaultImageName);
                    String imageNameProperty = getImageNameProperty();
                    if (properties.containsKey(imageNameProperty)) {
                        imageName = DockerImageName.parse(String.valueOf(properties.get(imageNameProperty))).asCompatibleSubstituteFor(defaultImageName);
                    }
                    return createContainer(imageName, properties);
                }));
        }
        return Optional.empty();
    }

    protected abstract Optional<String> resolveProperty(String propertyName, T container);

}
