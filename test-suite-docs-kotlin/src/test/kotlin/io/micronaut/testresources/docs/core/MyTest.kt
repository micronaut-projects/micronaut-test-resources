package io.micronaut.testresources.docs.core

// tag::imports[]
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
// end::imports[]

// tag::clazz[]
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyTest : TestPropertyProvider {

    @Value("\${myapp.someProperty}")
    lateinit var someProperty: String

    override fun getProperties(): Map<String, String> { // <1>
        return mapOf(
            "myapp.someProperty" to "value"
        )
    }

    @Test
    fun thePropertyIsAvailableToTheTest() {
        assertEquals("value", someProperty)
    }
}
// end::clazz[]
