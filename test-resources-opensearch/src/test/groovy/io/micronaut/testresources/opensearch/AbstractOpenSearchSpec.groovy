package io.micronaut.testresources.opensearch

import io.micronaut.context.annotation.Value
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Singleton

abstract class AbstractOpenSearchSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'opensearch'
    }

    @Override
    String getImageName() {
        'opensearch'
    }

    // Until we have a release of opensearch, we need to mock the client beans
    @Singleton
    static class OpenSearchHttp5Client {

        @Value('${micronaut.opensearch.httpclient5.http-hosts[0]}')
        String host
    }

    @Singleton
    static class OpenSearchRestClient {

        @Value('${micronaut.opensearch.rest-client.http-hosts[0]}')
        String host
    }
}
