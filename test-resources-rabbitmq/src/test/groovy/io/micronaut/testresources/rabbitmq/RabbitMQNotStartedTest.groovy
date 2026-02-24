package io.micronaut.testresources.rabbitmq

import groovy.test.NotYetImplemented
import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class RabbitMQNotStartedTest extends AbstractRabbitMQSpec {

    @Inject
    ApplicationContext applicationContext

    @NotYetImplemented
    def "doesn't start a RabbitMQ container if bean is not requested"() {

        when:
        def bean = applicationContext.getBean(SomeBean)

        then:
        listContainers().empty
    }

}
