package io.micronaut.testresources.kafka

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class KafkaNotStartedTest extends AbstractKafkaSpec {

    @Override
    Map<String, String> getProperties() {
        super.properties + [(KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS): 'localhost:65535']
    }

    @Inject
    ApplicationContext applicationContext

    def "doesn't start a Kafka container if bean is not requested"() {
        when:
        def bean = applicationContext.getBean(SomeBean)

        then:
        listContainers().empty
    }

}
