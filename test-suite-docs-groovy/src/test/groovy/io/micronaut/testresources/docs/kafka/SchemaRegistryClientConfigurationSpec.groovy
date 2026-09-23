package io.micronaut.testresources.docs.kafka

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = "kafka.schema.registry.url", value = "http://localhost:8081")
class SchemaRegistryClientConfigurationSpec extends Specification {

    @Inject
    SchemaRegistryClientConfiguration configuration

    void "the schema registry url is injected"() {
        expect:
        configuration.schemaRegistryUrl == "http://localhost:8081"
    }
}
