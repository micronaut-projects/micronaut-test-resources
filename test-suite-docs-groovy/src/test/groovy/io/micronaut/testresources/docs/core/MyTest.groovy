package io.micronaut.testresources.docs.core

// tag::imports[]
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import spock.lang.Specification
// end::imports[]

// tag::clazz[]
@MicronautTest
class MyTest extends Specification implements TestPropertyProvider {

    @Value('${myapp.someProperty}')
    String someProperty

    @Override
    Map<String, String> getProperties() { // <1>
        return [
                "myapp.someProperty": "value"
        ]
    }

    void "the property is available to the test"() {
        expect:
        someProperty == "value"
    }
}
// end::clazz[]
