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
package io.micronaut.testresources.proxy;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.testresources.core.TestResourcesResolver;
import io.micronaut.testresources.embedded.TestResourcesResolverLoader;
import io.micronaut.testresources.testcontainers.TestContainers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A client responsible for connecting to a test resources
 * proxy.
 */
@Controller("/proxy")
public final class TestResourcesController implements TestResourcesResolver {

    private final TestResourcesResolverLoader loader = new TestResourcesResolverLoader();

    @Get("/list")
    public List<String> getResolvableProperties() {
        return getResolvableProperties(Collections.emptyMap());
    }

    @Override
    @Post("/list")
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries) {
        return loader.getResolvers()
            .stream()
            .map(r -> r.getResolvableProperties(propertyEntries))
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    @Get("/requirements/expr/{expression}")
    public List<String> getRequiredProperties(String expression) {
        return loader.getResolvers()
            .stream()
            .map(testResourcesResolver -> testResourcesResolver.getRequiredProperties(expression))
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    @Get("/requirements/entries")
    public List<String> getRequiredPropertyEntries() {
        return loader.getResolvers()
            .stream()
            .map(TestResourcesResolver::getRequiredPropertyEntries)
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    @Post("/resolve")
    public Optional<String> resolve(String name, Map<String, Object> properties) {
        Optional<String> result = Optional.empty();
        for (TestResourcesResolver resolver : loader.getResolvers()) {
            result = resolver.resolve(name, properties);
            if (result.isPresent()) {
                return result;
            }
        }
        return result;
    }

    @Get("/close/all")
    public void closeAll() {
        TestContainers.closeAll();
    }

    @Get("/testcontainers")
    public List<TestContainer> listContainers() {
        return TestContainers.listAll()
            .stream()
            .map(c -> new TestContainer(
                c.getContainerName(),
                c.getDockerImageName(),
                c.getContainerId()))
            .collect(Collectors.toList());
    }
}
