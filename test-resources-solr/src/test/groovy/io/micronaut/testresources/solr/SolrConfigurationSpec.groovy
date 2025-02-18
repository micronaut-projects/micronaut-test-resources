package io.micronaut.testresources.solr

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared

@MicronautTest
@Property(name = 'solr.collection', value = 'testcollection')
@Property(name = 'solr.config.name', value = 'testconfig')
@Property(name = 'solr.config.url', value = 'http://localhost:8983/solr/testconfig')
@Property(name = 'solr.schema.url', value = 'http://localhost:8983/solr/schema.xml')
@Property(name = 'solr.zookeeper.enabled', value = 'true')
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
