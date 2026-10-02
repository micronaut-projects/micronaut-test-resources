# tag::imports[]
from typing import Annotated

from jakarta.inject import Singleton
from micronaut.context.annotation import Requires, Value
# end::imports[]


# tag::clazz[]
@Singleton
@Requires(property="kafka.schema.registry.url")  # <1>
class SchemaRegistryClientConfiguration:

    def __init__(self, schema_registry_url: Annotated[str, Value("${kafka.schema.registry.url}")]):  # <2>
        # Configure a Schema Registry client for tests.
        self.schema_registry_url = schema_registry_url
# end::clazz[]
