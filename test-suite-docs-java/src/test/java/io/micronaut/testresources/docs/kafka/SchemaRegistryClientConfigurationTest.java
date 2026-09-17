package io.micronaut.testresources.docs.kafka;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
@Property(name = "kafka.schema.registry.url", value = "http://localhost:8081")
class SchemaRegistryClientConfigurationTest {

    @Inject
    SchemaRegistryClientConfiguration configuration;

    @Test
    void theSchemaRegistryUrlIsInjected() {
        assertEquals("http://localhost:8081", configuration.getSchemaRegistryUrl());
    }
}
