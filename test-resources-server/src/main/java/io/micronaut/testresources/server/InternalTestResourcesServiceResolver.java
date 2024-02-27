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
package io.micronaut.testresources.server;

import io.micronaut.core.annotation.Internal;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.testresources.core.TestResourcesResolver;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link TestResourcesResolver} that resolves properties regarding the Test Resource Service itself,
 * such as <code>micronaut.test.resources.server.uri</code>.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 2.4.0
 */
@Internal
@Singleton
public class InternalTestResourcesServiceResolver implements TestResourcesResolver {

    public static final String SERVER_URI = "micronaut.test.resources.server.uri";

    private final EmbeddedServer server;

    public InternalTestResourcesServiceResolver(EmbeddedServer server) {
        this.server = server;
    }


    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return List.of(SERVER_URI);
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        if (SERVER_URI.equals(propertyName)) {
            return Optional.of(server.getURI().toString());
        }
        return Optional.empty();
    }
}
