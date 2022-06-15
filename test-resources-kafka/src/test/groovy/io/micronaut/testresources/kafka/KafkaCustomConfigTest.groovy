package io.micronaut.testresources.kafka

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(environments = "custom")
class KafkaCustomConfigTest extends AbstractKafkaSpec {

    @Inject
    ApplicationContext applicationContext

    def "automatically starts a Kafka container"() {
        when:
        def client = applicationContext.getBean(AnalyticsClient)
        def result = client.updateAnalytics("oh yeah!")

        then:
        result.block() == "oh yeah!"
    }

}
