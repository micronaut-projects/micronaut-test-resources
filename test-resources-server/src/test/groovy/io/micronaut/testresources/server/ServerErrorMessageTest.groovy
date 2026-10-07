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

/**
 * The HTTP server does not put exception messages in its default error
 * response since Micronaut Core 5.2, so the server has to send them itself.
 */
@MicronautTest
@Property(name = "spec.name", value = "ServerErrorMessageTest")
@Property(name = "micronaut.testresources.server.url", value = "")
class ServerErrorMessageTest extends Specification {

    @Inject
    EmbeddedServer server

    def "client receives the message of an exception thrown by a resolver"() {
        given:
        def client = new DefaultTestResourcesClient(server.URI.toString(), null, 60)

        when:
        client.resolve("failing.value", [:], [:])

        then:
        TestResourcesException e = thrown()
        e.message == "Something bad happened"
    }

    @Singleton
    @Requires(property = "spec.name", value = "ServerErrorMessageTest")
    static class FailingResolver implements InjectableTestResourcesResolver {
        @Override
        List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            ["failing.value"]
        }

        @Override
        Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
            throw new IllegalStateException("Something bad happened")
        }
    }
}
