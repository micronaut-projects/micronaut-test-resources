package io.micronaut.testresources.hivemq

import groovy.test.NotYetImplemented
import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class HiveMQNotStartedTest extends AbstractHiveMQSpec {

    @Inject
    ApplicationContext applicationContext

    @NotYetImplemented
    def "doesn't start a HiveMQ container if bean is not requested"() {

        when:
        def bean = applicationContext.getBean(SomeBean)

        then:
        hivemqContainers().empty
    }

}
