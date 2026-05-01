package io.micronaut.testresources.wiremock

import io.micronaut.context.annotation.Value
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Singleton

abstract class AbstractWireMockSpec extends AbstractTestContainersSpec {

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
}
