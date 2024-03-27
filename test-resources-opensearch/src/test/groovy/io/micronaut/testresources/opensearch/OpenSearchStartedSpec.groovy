package io.micronaut.testresources.opensearch

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest
class OpenSearchStartedSpec extends AbstractOpenSearchSpec {

    @Inject
    ApplicationContext applicationContext

    def "#name starts OpenSearch"() {
        when:
        applicationContext.getBean(clientBean)

        then:
        listContainers().size() == 1
        with(TestContainers.listByScope("opensearch").get(Scope.of("opensearch"))) {
            size() == 1
            get(0).dockerImageName == "opensearchproject/opensearch:latest"
        }

        where:
        clientBean << [OpenSearchHttp5Client, OpenSearchRestClient]
        name = clientBean.simpleName
    }
}
