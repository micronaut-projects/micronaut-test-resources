from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .SchemaRegistryClientConfiguration import SchemaRegistryClientConfiguration


@MicronautTest
@Property(name="kafka.schema.registry.url", value="http://localhost:8081")
class SchemaRegistryClientConfigurationTest:

    configuration: Annotated[SchemaRegistryClientConfiguration, Inject]

    @Test
    def the_schema_registry_url_is_injected(self) -> None:
        assert self.configuration.schema_registry_url == "http://localhost:8081"
