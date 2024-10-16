package io.micronaut.testresources.kafka

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest
class KafkaStartedTest extends AbstractKafkaSpec {

    @Inject
    ApplicationContext applicationContext

    def "automatically starts a Kafka container"() {
        when:
        def client = applicationContext.getBean(AnalyticsClient)
        def result = client.updateAnalytics("oh yeah!")

        then:
        listContainers().size() == 1
        result.block() == "oh yeah!"
        with(TestContainers.listByScope("kafka").get(Scope.of("kafka"))) {
            size() == 1
            get(0).dockerImageName == KafkaTestResourceProvider.DEFAULT_IMAGE
        }
    }

}
