package io.micronaut.testresources.rabbitmq

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class RabbitMQStartedTest extends AbstractRabbitMQSpec {

    @Inject
    ApplicationContext applicationContext

    def "automatically starts a RabbitMQ container"() {
        given:
        def initializer = applicationContext.getBean(ChannelPoolListener)
        def publisher = applicationContext.getBean(Publisher)
        def client = applicationContext.getBean(Consumer)

        when:
        publisher.updateAnalytics(new Book(title: "Micronaut for Spring developers"))

        then:
        listContainers().size() == 1
        client.book == new Book(title: "Micronaut for Spring developers")
    }

}
