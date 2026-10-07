package io.micronaut.testresources.docs.junitplatform

// tag::imports[]
import io.micronaut.test.extensions.junit5.ScopeHolder
import io.micronaut.test.extensions.junit5.annotation.ScopeNamingStrategy
import io.micronaut.test.extensions.junit5.annotation.TestResourcesScope
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification
// end::imports[]

// tag::clazz[]
@MicronautTest
@TestResourcesScope(namingStrategy = ScopeNamingStrategy.TestClassName) // <1>
class ScopeNamingStrategyTest extends Specification {

    void "test something"() {
        expect:
        ScopeHolder.get().orElse(null) == ScopeNamingStrategyTest.name // <2>
    }
}
// end::clazz[]
