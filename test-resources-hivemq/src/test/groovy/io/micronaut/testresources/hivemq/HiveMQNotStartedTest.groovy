package io.micronaut.testresources.hivemq


import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.PendingFeature

@MicronautTest
class HiveMQNotStartedTest extends AbstractHiveMQSpec {

    @Inject
    ApplicationContext applicationContext

    @PendingFeature
    def "doesn't start a HiveMQ container if bean is not requested"() {

        when:
        def bean = applicationContext.getBean(SomeBean)

        then:
        listContainers().empty
    }

}
