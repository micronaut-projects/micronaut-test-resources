package io.micronaut.testresources.hivemq

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class HiveMQStartedTest extends AbstractHiveMQSpec {

    @Inject
    ApplicationContext applicationContext

    def "automatically starts a HiveMQ container"() {
        given:
        def publisher = applicationContext.getBean(Publisher)
        def client = applicationContext.getBean(Client)

        when:
        publisher.emit("hello".getBytes())


        then:
        hivemqContainers().size() == 1
        client.message == 'hello'
    }

}
