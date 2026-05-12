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
package io.micronaut.testresources.opentelemetry

import io.micronaut.testresources.core.DefaultTestResourceImages
import io.micronaut.testresources.core.TestResourcesResolutionException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.containers.wait.strategy.WaitAllStrategy
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

class OpenTelemetryTestResourceProviderTest extends Specification {

    private final provider = new TestableOpenTelemetryTestResourceProvider()

    def "provides OpenTelemetry endpoint property metadata"() {
        expect:
        provider.displayName == "OpenTelemetry"
        provider.name == "containers.opentelemetry"
        provider.getResolvableProperties([:], [:]) == ["otel.exporter.otlp.endpoint"]
        provider.getRequiredProperties("otel.exporter.otlp.endpoint") == ["OTEL_EXPORTER_OTLP_ENDPOINT"]
        provider.getRequiredProperties("other.property").empty
    }

    def "does not answer when endpoint is already configured"() {
        expect:
        !provider.shouldAnswer("otel.exporter.otlp.endpoint", [
                "otel.exporter.otlp.endpoint": "http://collector:4317"
        ], [:])
        !provider.shouldAnswer("otel.exporter.otlp.endpoint", [
                "OTEL_EXPORTER_OTLP_ENDPOINT": "http://collector:4317"
        ], [:])
    }

    def "answers when required properties contain unresolved endpoint placeholder"() {
        expect:
        provider.shouldAnswer("otel.exporter.otlp.endpoint", [
                "otel.exporter.otlp.endpoint": '${auto.test.resources.otel.exporter.otlp.endpoint}'
        ], [:])
    }

    def "creates LGTM container with default gRPC support"() {
        when:
        GenericContainer<?> container = provider.create(DockerImageName.parse(DefaultTestResourceImages.DEFAULT_OPENTELEMETRY_IMAGE), [:], [:])
        def waitStrategies = waitStrategies(container)

        then:
        container.exposedPorts == [4317, 3000]
        waitStrategies.any { it instanceof HostPortWaitStrategy && field(it, "ports").toList() == [4317] }
        waitStrategies.any { it instanceof HttpWaitStrategy && field(it, "path") == "/api/health" && field(it, "livenessPort").get() == 3000 }
    }

    def "rejects unsupported backend"() {
        when:
        provider.create(DockerImageName.parse(DefaultTestResourceImages.DEFAULT_OPENTELEMETRY_IMAGE), [:], [
                "containers.opentelemetry.backend": "collector"
        ])

        then:
        TestResourcesResolutionException e = thrown()
        e.message.contains("Unsupported OpenTelemetry test resource backend 'collector'")
    }

    def "rejects unsupported protocol"() {
        when:
        provider.create(DockerImageName.parse(DefaultTestResourceImages.DEFAULT_OPENTELEMETRY_IMAGE), [:], [
                "containers.opentelemetry.protocol": "http"
        ])

        then:
        TestResourcesResolutionException e = thrown()
        e.message.contains("Unsupported OpenTelemetry test resource protocol 'http'")
    }

    def "resolves mapped gRPC endpoint"() {
        given:
        def container = Stub(GenericContainer) {
            getHost() >> "localhost"
            getMappedPort(4317) >> 15417
            getMappedPort(3000) >> 13000
        }

        expect:
        provider.resolve("otel.exporter.otlp.endpoint", container).get() == "http://localhost:15417"
    }

    static class TestableOpenTelemetryTestResourceProvider extends OpenTelemetryTestResourceProvider {
        GenericContainer<?> create(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
            createContainer(imageName, requestedProperties, testResourcesConfig)
        }

        Optional<String> resolve(String propertyName, GenericContainer<?> container) {
            resolveProperty(propertyName, container)
        }
    }

    private static List<?> waitStrategies(GenericContainer<?> container) {
        def waitStrategy = field(container, "waitStrategy")
        assert waitStrategy instanceof WaitAllStrategy
        field(waitStrategy, "strategies")
    }

    private static Object field(Object target, String name) {
        Class<?> type = target.class
        while (type != null) {
            try {
                def field = type.getDeclaredField(name)
                field.accessible = true
                return field.get(target)
            } catch (NoSuchFieldException ignored) {
                type = type.superclass
            }
        }
        throw new NoSuchFieldException(name)
    }
}
