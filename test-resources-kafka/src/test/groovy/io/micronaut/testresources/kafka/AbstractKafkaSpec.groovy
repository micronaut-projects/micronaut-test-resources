package io.micronaut.testresources.kafka


import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.annotation.Prototype
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import reactor.core.publisher.Mono

abstract class AbstractKafkaSpec  extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'kafka'
    }

    @Override
    String getImageName() {
        'cp-kafka'
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
