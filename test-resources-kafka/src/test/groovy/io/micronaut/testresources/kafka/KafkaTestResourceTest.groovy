package io.micronaut.testresources.kafka

import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Prototype
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import reactor.core.publisher.Mono
import spock.lang.Specification

@MicronautTest
class KafkaTestResourceTest extends Specification {

    @Inject
    ApplicationContext applicationContext

    def "doesn't start a Kafka container if bean is not requested"() {
        when:
        def bean = applicationContext.getBean(SomeBean)

        then:
        "TODO: test no kafka running"
    }

    def "automatically starts a Kafka container"() {
        when:
        def client = applicationContext.getBean(AnalyticsClient)
        def result = client.updateAnalytics("oh yeah!")

        then:
        result.block() == "oh yeah!"
    }

    @Prototype
    static class SomeBean {
        String message = 'hello'
    }

    @KafkaClient
    static interface AnalyticsClient {
        @Topic("analytics")
        Mono<String> updateAnalytics(String book);
    }
}
