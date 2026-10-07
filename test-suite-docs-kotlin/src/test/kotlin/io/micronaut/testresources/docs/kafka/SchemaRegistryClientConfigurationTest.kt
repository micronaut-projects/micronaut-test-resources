package io.micronaut.testresources.docs.kafka

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest
@Property(name = "kafka.schema.registry.url", value = "http://localhost:8081")
class SchemaRegistryClientConfigurationTest {

    @Inject
    lateinit var configuration: SchemaRegistryClientConfiguration

    @Test
    fun theSchemaRegistryUrlIsInjected() {
        assertEquals("http://localhost:8081", configuration.schemaRegistryUrl)
    }
}
