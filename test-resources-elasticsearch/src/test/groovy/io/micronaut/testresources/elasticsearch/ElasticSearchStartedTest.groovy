package io.micronaut.testresources.elasticsearch

import io.micronaut.test.extensions.spock.annotation.MicronautTest

@MicronautTest
class ElasticSearchStartedTest extends AbstractElasticSearchSpec {


    def "automatically starts an ElasticSearch container"() {
        when:
        def info = client.info()

        then:
        info.clusterName() != null
    }

}
