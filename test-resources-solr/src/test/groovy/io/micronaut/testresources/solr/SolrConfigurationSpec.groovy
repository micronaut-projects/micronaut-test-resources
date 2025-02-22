package io.micronaut.testresources.solr

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared

@MicronautTest
class SolrConfigurationSpec extends AbstractSolrSpec {

    @Inject
    ApplicationContext applicationContext

    @Shared
    String testResourcePath = 'src/test/resources'

    def 'verify Solr container configuration'() {
        when: 'we get the Solr client'
        def client = applicationContext.getBean(SolrConfigClient)

        then: 'container is running'
        listContainers().size() == 1

        and: 'Solr endpoint is accessible'
        client.solrHost.contains('/solr')

        and: 'Zookeeper is running'
        client.zkHost.contains(':')

        and: 'The container is using the correct image'
        with(TestContainers.listByScope('solr').get(Scope.of('solr'))) {
            size() == 1
            get(0).dockerImageName == 'solr:9.8.0'
        }
    }

    @Singleton
    static class SolrConfigClient {
        String solrHost
        String zkHost

        SolrConfigClient(
                @Value('${micronaut.solr.hosts}') String solrHost,
                @Value('${micronaut.solr.zk-hosts}') String zkHost
        ) {
            this.solrHost = solrHost
            this.zkHost = zkHost
        }
    }
}
