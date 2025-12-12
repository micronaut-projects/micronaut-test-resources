package io.micronaut.testresources.rabbitmq

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.PendingFeature

@MicronautTest
class RabbitMQNotStartedTest extends AbstractRabbitMQSpec {

    @Inject
    ApplicationContext applicationContext

    @PendingFeature
    def "doesn't start a RabbitMQ container if bean is not requested"() {

        when:
        def bean = applicationContext.getBean(SomeBean)

        then:
        listContainers().empty
    }

}
