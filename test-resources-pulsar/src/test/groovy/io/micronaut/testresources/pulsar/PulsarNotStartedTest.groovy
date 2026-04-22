package io.micronaut.testresources.pulsar

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class PulsarNotStartedTest extends AbstractPulsarSpec {

    @Inject
    ApplicationContext applicationContext

    def "doesn't start a Pulsar container if bean is not requested"() {
        when:
        def bean = applicationContext.getBean(SomeBean)

        then:
        bean.message == 'hello'
        listContainers().empty
    }
}
