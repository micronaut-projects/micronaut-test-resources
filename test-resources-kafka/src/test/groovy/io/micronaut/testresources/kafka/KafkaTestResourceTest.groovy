package io.micronaut.testresources.kafka

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Prototype
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.testcontainers.DockerClientFactory
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
        kafkaContainers().empty
    }

    def "automatically starts a Kafka container"() {
        when:
        def client = applicationContext.getBean(AnalyticsClient)
        def result = client.updateAnalytics("oh yeah!")

        then:
        kafkaContainers().size() == 1
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

    private DockerClient dockerClient() {
        DockerClientFactory.instance().client()
    }

    private List<Container> runningTestContainers() {
        dockerClient().listContainersCmd()
                .exec()
                .findAll {
                    it.labels['org.testcontainers'] == 'true'
                }
                .findAll {
                    println it
                    it.state == 'running'
                }
    }

    private List<Container> kafkaContainers() {
        runningTestContainers()
                .findAll { it.image.contains('cp-kafka') }
    }
}
