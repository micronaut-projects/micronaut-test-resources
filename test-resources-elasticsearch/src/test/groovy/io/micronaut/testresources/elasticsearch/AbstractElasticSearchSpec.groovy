package io.micronaut.testresources.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchClient
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Inject

abstract class AbstractElasticSearchSpec extends AbstractTestContainersSpec {

    @Inject
    protected ElasticsearchClient client

    @Override
    String getScopeName() {
        'elasticsearch'
    }

}
