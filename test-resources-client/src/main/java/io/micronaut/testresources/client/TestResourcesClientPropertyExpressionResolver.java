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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.value.PropertyResolver;
import io.micronaut.testresources.codec.Result;
import io.micronaut.testresources.core.LazyTestResourcesExpressionResolver;
import io.micronaut.testresources.core.TestResourcesResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.micronaut.testresources.core.PropertyResolverSupport.resolveRequiredProperties;

/**
 * A property expression resolver which connects via client to a server in order to resolve
 * properties.
 */
public class TestResourcesClientPropertyExpressionResolver extends LazyTestResourcesExpressionResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestResourcesClientPropertyExpressionResolver.class);

    public TestResourcesClientPropertyExpressionResolver() {
        super(new DelegateResolver());
    }

    private static TestResourcesClient createClient(Environment env) {
        return TestResourcesClientFactory.findByConvention().orElse(NoOpClient.INSTANCE);
    }

    private static class NoOpClient implements TestResourcesClient {
        static final TestResourcesClient INSTANCE = new NoOpClient();

        @Override
        public Result<List<String>> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            return Result.emptyList();
        }

        @Override
        public Optional<Result<String>> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
            return Optional.empty();
        }

        @Override
        public Result<List<String>> getRequiredProperties(String expression) {
            return Result.emptyList();
        }

        @Override
        public Result<List<String>> getRequiredPropertyEntries() {
            return Result.emptyList();
        }

        @Override
        public Result<Boolean> closeAll() {
            return Result.TRUE;
        }

        @Override
        public Result<Boolean> closeScope(@Nullable String id) {
            return Result.FALSE;
        }
    }

    private static class DelegateResolver implements PropertyExpressionResolver, AutoCloseable {
        private final Map<Environment, TestResourcesClient> clients = new ConcurrentHashMap<>();

        @Override
        public <T> Optional<T> resolve(PropertyResolver propertyResolver,
                                       ConversionService conversionService,
                                       String expression,
                                       Class<T> requiredType) {
            if (propertyResolver instanceof Environment) {
                TestResourcesClient client = clients.computeIfAbsent((Environment) propertyResolver, TestResourcesClientPropertyExpressionResolver::createClient);
                Map<String, Object> props = resolveRequiredProperties(expression, propertyResolver, new TestResourcesResolver() {
                    @Override
                    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
                        return client.getResolvableProperties(propertyEntries, testResourcesConfig).value();
                    }

                    @Override
                    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
                        return client.resolve(propertyName, properties, testResourcesConfig).map(Result::value);
                    }

                    @Override
                    public List<String> getRequiredProperties(String expression) {
                        return client.getRequiredProperties(expression).value();
                    }

                    @Override
                    public List<String> getRequiredPropertyEntries() {
                        return client.getRequiredPropertyEntries().value();
                    }
                });
                Map<String, Object> properties = propertyResolver.getProperties(TestResourcesResolver.TEST_RESOURCES_PROPERTY);
                Optional<String> resolved = callClient(expression, client, props, properties);
                if (resolved.isPresent()) {
                    String resolvedValue = resolved.get();
                    LOGGER.debug("Resolved expression '{}' to '{}'", expression, resolvedValue);
                    return conversionService.convert(resolvedValue, requiredType);
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Test resources cannot resolve expression '{}'", expression);
                }
            }
            return Optional.empty();
        }

        private static Optional<String> callClient(String expression, TestResourcesClient client, Map<String, Object> props, Map<String, Object> properties) {
            return client.resolve(expression, props, properties).map(Result::value);
        }

        @Override
        public void close() throws Exception {
            for (TestResourcesClient client : clients.values()) {
                client.closeAll();
            }
        }
    }
}
