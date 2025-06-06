package io.micronaut.testresources.solr

import io.micronaut.context.annotation.Value
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Singleton

abstract class AbstractWiremockSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'wiremock'
    }

    @Override
    String getImageName() {
        'wiremock'
    }

    @Singleton
    static class WiremockClient {

        @Value('${wiremock.port}')
        int port

        @Value('${wiremock.host}')
        String host

        @Value('${wiremock.url}')
        String url
    }

}
