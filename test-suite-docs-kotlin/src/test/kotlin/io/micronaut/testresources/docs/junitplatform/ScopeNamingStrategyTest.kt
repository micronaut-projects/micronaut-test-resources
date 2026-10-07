package io.micronaut.testresources.docs.junitplatform

// tag::imports[]
import io.micronaut.test.extensions.junit5.ScopeHolder
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.extensions.junit5.annotation.ScopeNamingStrategy
import io.micronaut.test.extensions.junit5.annotation.TestResourcesScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
// end::imports[]

// tag::clazz[]
@MicronautTest
@TestResourcesScope(namingStrategy = ScopeNamingStrategy.TestClassName::class) // <1>
class ScopeNamingStrategyTest {

    @Test
    fun testSomething() {
        assertEquals(ScopeNamingStrategyTest::class.java.name, ScopeHolder.get().orElse(null)) // <2>
    }
}
// end::clazz[]
