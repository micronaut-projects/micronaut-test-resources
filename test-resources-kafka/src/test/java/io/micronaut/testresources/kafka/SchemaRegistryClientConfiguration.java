package io.micronaut.testresources.kafka;

// tag::clazz[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

@Singleton
@Requires(property = "kafka.schema.registry.url")
class SchemaRegistryClientConfiguration {

    SchemaRegistryClientConfiguration(
            @Value("${kafka.schema.registry.url}") String schemaRegistryUrl) {
        // Configure a Schema Registry client for tests.
    }
}
// end::clazz[]
