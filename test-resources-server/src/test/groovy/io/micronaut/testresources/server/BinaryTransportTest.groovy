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
package io.micronaut.testresources.server

import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.codec.Result
import io.micronaut.testresources.codec.TestResourcesMediaType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

@MicronautTest
@Property(name = "spec.name", value = "BinaryTransportTest")
@Property(name = "micronaut.testresources.server.url", value = "")
@Property(name = "micronaut.http.client.read-timeout", value = "60s")
class BinaryTransportTest extends Specification {

    @Inject
    DiagnosticsClient client

    def "binary transport preserves core controller flows"() {
        expect:
        client.resolvableProperties.value().contains("test.binary.value")
        client.resolvableProperties.value().contains("micronaut.test.resources.server.uri")
        client.requiredProperties("binary.expression").value() == ["binary.dependency"]
        client.requiredPropertyEntries.value() == ["binary.dependency"]
        client.resolve("test.binary.value", [dependency: "ok"], [:]).get().value() == "resolved-ok"
        client.resolve("missing.binary.value", [:], [:]).empty
        !client.closeScope("binary-scope").value()
        !client.closeAll().value()
        client.listContainers().value().empty
    }

    @Client("/")
    @Produces(TestResourcesMediaType.TEST_RESOURCES_BINARY)
    @Consumes(TestResourcesMediaType.TEST_RESOURCES_BINARY)
    static interface DiagnosticsClient {
        @Get("/list")
        Result<List<String>> getResolvableProperties()

        @Post("/list")
        Result<List<String>> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig)

        @Get("/testcontainers")
        Result<List<Map<String, Object>>> listContainers()

        @Post("/resolve")
        Optional<Result<String>> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig)

        @Get("/requirements/expr/{expression}")
        Result<List<String>> requiredProperties(String expression)

        @Get("/requirements/entries")
        Result<List<String>> getRequiredPropertyEntries()

        @Get("/close/all")
        Result<Boolean> closeAll()

        @Get("/close/{id}")
        Result<Boolean> closeScope(String id)
    }

    @Singleton
    @Requires(property = "spec.name", value = "BinaryTransportTest")
    static class BinaryTestResolver implements InjectableTestResourcesResolver {
        @Override
        List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            ["test.binary.value", "missing.binary.value"]
        }

        @Override
        Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
            if (propertyName == "test.binary.value") {
                return Optional.of("resolved-${properties.get("dependency")}".toString())
            }
            return Optional.empty()
        }

        @Override
        List<String> getRequiredProperties(String expression) {
            expression == "binary.expression" ? ["binary.dependency"] : []
        }

        @Override
        List<String> getRequiredPropertyEntries() {
            ["binary.dependency"]
        }
    }
}
