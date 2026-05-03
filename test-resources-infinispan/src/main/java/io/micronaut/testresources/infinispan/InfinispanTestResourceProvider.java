/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.testresources.infinispan;

import io.micronaut.testresources.core.DefaultTestResourceImages;
import io.micronaut.testresources.core.TestResourcesResolver;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.infinispan.testcontainers.InfinispanContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;

public class InfinispanTestResourceProvider extends AbstractTestContainersProvider<InfinispanContainer> implements TestResourcesResolver {

    static final String HOST = "infinispan.client.hotrod.server.host";
    static final String PORT = "infinispan.client.hotrod.server.port";
    static final String USERNAME = "infinispan.client.hotrod.security.authentication.username";
    static final String PASSWORD = "infinispan.client.hotrod.security.authentication.password";

    private static final List<String> SUPPORTED_PROPERTIES = List.of(HOST, PORT, USERNAME, PASSWORD);

    @Override
    public String getDisplayName() {
        return "Infinispan";
    }

    @Override
    protected String getSimpleName() {
        return "infinispan";
    }

    @Override
    protected String getDefaultImageName() {
        return DefaultTestResourceImages.DEFAULT_INFINISPAN_IMAGE;
    }

    @Override
    protected InfinispanContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new InfinispanContainer(imageName.asCanonicalNameString());
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, InfinispanContainer container) {
        if (HOST.equals(propertyName)) {
            return Optional.of(container.getHost());
        }
        if (PORT.equals(propertyName)) {
            return Optional.of(String.valueOf(container.getMappedPort(InfinispanContainer.DEFAULT_HOTROD_PORT)));
        }
        if (USERNAME.equals(propertyName)) {
            return Optional.of(container.getEnvMap().getOrDefault(InfinispanContainer.USER, InfinispanContainer.DEFAULT_USERNAME));
        }
        if (PASSWORD.equals(propertyName)) {
            return Optional.of(container.getEnvMap().getOrDefault(InfinispanContainer.PASS, InfinispanContainer.DEFAULT_PASSWORD));
        }

        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES.contains(propertyName);
    }
}
