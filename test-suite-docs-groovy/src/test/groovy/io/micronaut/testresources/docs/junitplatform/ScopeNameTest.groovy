package io.micronaut.testresources.docs.junitplatform

// tag::imports[]
import io.micronaut.test.extensions.junit5.ScopeHolder
import io.micronaut.test.extensions.junit5.annotation.TestResourcesScope
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification
// end::imports[]

// tag::clazz[]
@MicronautTest
@TestResourcesScope("my scope") // <1>
class ScopeNameTest extends Specification {

    void "test something"() {
        expect:
        ScopeHolder.get().orElse(null) == "my scope" // <2>
    }
}
// end::clazz[]
