package io.micronaut.testresources.opensearch

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest
class CustomOpenSearchImageSpec extends AbstractOpenSearchSpec {

    @Override
    Map<String, String> getProperties() {
        super.properties + [
                "test-resources.containers.opensearch.image-name": "opensearchproject/opensearch:2.12.0"
        ]
    }

    @Inject
    ApplicationContext applicationContext

    def "#name starts OpenSearch using a custom image"() {
        when:
        applicationContext.getBean(clientBean)

        then:
        listContainers().size() == 1
        with(TestContainers.listByScope("opensearch").get(Scope.of("opensearch"))) {
            size() == 1
            get(0).dockerImageName == "opensearchproject/opensearch:2.12.0"
        }

        where:
        clientBean << [OpenSearchHttp5Client, OpenSearchRestClient]
        name = clientBean.simpleName
    }
}
