package io.micronaut.testresources.solr

import io.micronaut.context.annotation.Value
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Singleton

abstract class AbstractSolrSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'solr'
    }

    @Override
    String getImageName() {
        'solr'
    }
    @Singleton
    static class SolrClient {

        @Value('${micronaut.solr.hosts}')
        String host
    }

}
