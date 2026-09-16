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
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.client.DefaultTestResourcesClient
import io.micronaut.testresources.client.TestResourcesException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

@MicronautTest
@Property(name = "spec.name", value = "TestResourcesExceptionHandlerTest")
@Property(name = "micronaut.testresources.server.url", value = "")
class TestResourcesExceptionHandlerTest extends Specification {

    @Inject
    EmbeddedServer server

    def "client receives an error message containing colons intact (#property)"() {
        given:
        def client = new DefaultTestResourcesClient(server.URI.toString(), null, 60)

        when:
        client.resolve(property, [:], [:])

        then:
        TestResourcesException e = thrown()
        e.message == expectedMessage

        where:
        property          | expectedMessage
        "image.failure"   | "Container startup failed for image docker.io/postgres:16"
        "nested.failure"  | "Connection refused: localhost:5432"
    }

    @Singleton
    @Requires(property = "spec.name", value = "TestResourcesExceptionHandlerTest")
    static class FailingResolver implements InjectableTestResourcesResolver {
        @Override
        List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            ["image.failure", "nested.failure"]
        }

        @Override
        Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
            if (propertyName == "image.failure") {
                throw new IllegalStateException("Container startup failed for image docker.io/postgres:16")
            }
            if (propertyName == "nested.failure") {
                throw new IllegalStateException("Could not start container",
                    new ConnectException("Connection refused: localhost:5432"))
            }
            Optional.empty()
        }
    }
}
