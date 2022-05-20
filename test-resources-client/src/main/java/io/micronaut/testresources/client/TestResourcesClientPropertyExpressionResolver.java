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
package io.micronaut.testresources.client;

import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertyExpressionResolver;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.value.PropertyResolver;
import io.micronaut.testresources.core.LazyTestResourcesExpressionResolver;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.micronaut.testresources.core.PropertyResolverSupport.resolveRequiredProperties;

/**
 * A property expression resolver which connects via client to a proxy in order to resolve
 * properties.
 */
public class TestResourcesClientPropertyExpressionResolver extends LazyTestResourcesExpressionResolver {

    public TestResourcesClientPropertyExpressionResolver() {
        super(new PropertyExpressionResolver() {
            private final Map<Environment, TestResourcesClient> clients = new ConcurrentHashMap<>();

            @Override
            public <T> Optional<T> resolve(PropertyResolver propertyResolver, ConversionService<?> conversionService, String expression, Class<T> requiredType) {
                if (propertyResolver instanceof Environment) {
                    TestResourcesClient client = clients.computeIfAbsent((Environment) propertyResolver, TestResourcesClientPropertyExpressionResolver::createClient);
                    Map<String, Object> props = resolveRequiredProperties(propertyResolver, client);
                    Optional<String> resolved = client.resolve(expression, props);
                    if (resolved.isPresent()) {
                        return conversionService.convert(resolved.get(), requiredType);
                    }
                }
                return Optional.empty();
            }
        });
    }

    private static TestResourcesClient createClient(Environment env) {
        Optional<URL> config = env.getResource("/test-resources.properties");
        return config.map(TestResourcesClientFactory::configuredAt)
            .orElse(NoOpClient.INSTANCE);
    }

    private static class NoOpClient implements TestResourcesClient {
        static final TestResourcesClient INSTANCE = new NoOpClient();

        @Override
        public List<String> getResolvableProperties() {
            return Collections.emptyList();
        }

        @Override
        public Optional<String> resolve(String name, Map<String, Object> properties) {
            return Optional.empty();
        }

        @Override
        public List<String> getRequiredProperties() {
            return Collections.emptyList();
        }

        @Override
        public void closeAll() {
        }
    }

}
