package io.micronaut.testresources.wiremock

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject
import jakarta.inject.Singleton

abstract class AbstractWireMockSpec extends AbstractTestContainersSpec {

    @Inject
    ApplicationContext applicationContext

    @Override
    String getScopeName() {
        'wiremock'
    }

    @Override
    String getImageName() {
        'wiremock'
    }

    @Singleton
    static class WireMockClient {

        @Value('${wiremock.host}')
        String host

        @Value('${wiremock.port}')
        int port

        @Value('${wiremock.url}')
        String url
    }

    WireMockClient wireMockClient() {
        applicationContext.getBean(WireMockClient)
    }

    List<?> wireMockContainers() {
        TestContainers.listByScope(scopeName).get(Scope.of(scopeName))
    }
}
