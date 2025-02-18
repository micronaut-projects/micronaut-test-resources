package io.micronaut.testresources.solr

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest
class SolrStartedSpec extends AbstractSolrSpec {

    @Inject
    ApplicationContext applicationContext

    def "#name starts Solr"() {
        when:
        applicationContext.getBean(clientBean)

        then:
        listContainers().size() == 1
        with(TestContainers.listByScope("solr").get(Scope.of("solr"))) {
            size() == 1
            get(0).dockerImageName == "solr:9.8.0"
        }

        where:
        clientBean << [SolrClient]
        name = clientBean.simpleName
    }
}
