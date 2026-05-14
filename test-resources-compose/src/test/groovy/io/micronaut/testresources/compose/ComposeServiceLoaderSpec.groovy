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
package io.micronaut.testresources.compose

import io.micronaut.testresources.core.TestResourcesResolver
import io.micronaut.testresources.core.ToggableTestResourcesResolver
import io.micronaut.testresources.core.compose.ComposeResolverSupport
import spock.lang.Specification

class ComposeServiceLoaderSpec extends Specification {

    def cleanup() {
        ComposeResolverSupport.close()
    }

    def "compose module advertises provider-owned resolvers across supported modules"() {
        when:
        def providers = advertisedProviders()

        then:
        providers.containsAll([
                "io.micronaut.testresources.azure.AzuriteComposeTestResourceProvider",
                "io.micronaut.testresources.kafka.KafkaComposeTestResourceProvider",
                "io.micronaut.testresources.localstack.LocalStackComposeTestResourceProvider",
                "io.micronaut.testresources.mysql.MySQLComposeTestResourceProvider",
                "io.micronaut.testresources.r2dbc.mysql.R2DBCMySQLComposeTestResourceProvider",
                "io.micronaut.testresources.hibernate.reactive.mysql.HibernateReactiveMySQLComposeTestResourceProvider",
                "io.micronaut.testresources.opensearch.OpenSearchComposeTestResourceProvider",
                "io.micronaut.testresources.oauth2.keycloak.KeycloakComposeTestResourceProvider",
                "io.micronaut.testresources.wiremock.WireMockComposeTestResourceProvider"
        ])
        providers.every { it.endsWith("ComposeTestResourceProvider") }
    }

    def "advertised Compose providers expose resolver metadata without starting containers"() {
        when:
        def resolvers = ServiceLoader.load(TestResourcesResolver, getClass().classLoader)
                .findAll { it.class.name.endsWith("ComposeTestResourceProvider") }
                .sort { it.class.name }

        then:
        resolvers*.class.name == advertisedProviders().sort()

        and:
        resolvers.every { resolver ->
            resolver.displayName.startsWith("Docker Compose ")
            resolver.order == ComposeResolverSupport.ORDER
            resolver instanceof ToggableTestResourcesResolver
            resolver.name.startsWith("compose.")
            resolver.isEnabled(["compose.enabled": true])
            !resolver.isEnabled(["compose.enabled": false])
        }
    }

    def "advertised Compose providers ignore unsupported properties without starting containers"() {
        given:
        def resolvers = ServiceLoader.load(TestResourcesResolver, getClass().classLoader)
                .findAll { it.class.name.endsWith("ComposeTestResourceProvider") }

        expect:
        resolvers
        resolvers.every { resolver ->
            def method = resolver.class.getDeclaredMethod("resolveWithoutContainer", String, Map, Map)
            method.accessible = true
            method.invoke(resolver, "unsupported.compose.property", [:], [:]).empty
        }
    }

    private List<String> advertisedProviders() {
        getClass().classLoader
                .getResource("META-INF/services/io.micronaut.testresources.core.TestResourcesResolver")
                .readLines()
                .findAll { !it.blank && !it.startsWith("#") }
    }
}
