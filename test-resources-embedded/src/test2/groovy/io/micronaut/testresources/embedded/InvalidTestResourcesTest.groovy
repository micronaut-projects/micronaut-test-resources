package io.micronaut.testresources.embedded

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class InvalidTestResourcesTest extends Specification {

    def "fails if a resolver uses camel case in properties"() {
        when:
        def context = ApplicationContext.builder().build()
        context.start()

        then:
        IllegalArgumentException ex = thrown()
        ex.message == 'Test resources resolver [io.micronaut.testresources.embedded.support.FailingResolver] : Property key [myProperty] is not valid. Property keys must be in kebab case.'
    }

}
