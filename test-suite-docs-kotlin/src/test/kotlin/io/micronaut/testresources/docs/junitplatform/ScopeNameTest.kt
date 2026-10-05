package io.micronaut.testresources.docs.junitplatform

// tag::imports[]
import io.micronaut.test.extensions.junit5.ScopeHolder
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.extensions.junit5.annotation.TestResourcesScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
// end::imports[]

// tag::clazz[]
@MicronautTest
@TestResourcesScope("my scope") // <1>
class ScopeNameTest {

    @Test
    fun testSomething() {
        assertEquals("my scope", ScopeHolder.get().orElse(null)) // <2>
    }
}
// end::clazz[]
