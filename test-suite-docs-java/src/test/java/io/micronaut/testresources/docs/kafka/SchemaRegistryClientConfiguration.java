package io.micronaut.testresources.docs.kafka;

// tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
// end::imports[]

// tag::clazz[]
@Singleton
@Requires(property = "kafka.schema.registry.url") // <1>
class SchemaRegistryClientConfiguration {

    private final String schemaRegistryUrl;

    SchemaRegistryClientConfiguration(
            @Value("${kafka.schema.registry.url}") String schemaRegistryUrl) { // <2>
        // Configure a Schema Registry client for tests.
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    String getSchemaRegistryUrl() {
        return schemaRegistryUrl;
    }
}
// end::clazz[]
