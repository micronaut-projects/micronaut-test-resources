# tag::imports[]
from typing import Annotated

from java.net import URI
from micronaut.context.annotation import Value
from micronaut.core.annotation import ReflectiveAccess
from micronaut.test.extensions.junit5.annotation import MicronautTest
from micronaut.test.extensions.testresources import TestResourcesPropertyProvider
from micronaut.test.extensions.testresources.annotation import TestResourcesProperties
from org.junit.jupiter.api import Disabled, Test
# end::imports[]


# tag::provider[]
@ReflectiveAccess
class RabbitMQProvider(TestResourcesPropertyProvider):

    def provide(self, test_properties: dict[str, object]) -> dict[str, str]:
        uri = test_properties.get("rabbitmq.uri")  # <1>
        return {
            "rabbitmq.servers.product-cluster.port": str(URI.create(uri).getPort())  # <2>
        }
# end::provider[]


# TODO(python): @TestResourcesProperties is read by reflection from the test class (TestResourcesPropertiesFactory)
# but the Python compiler does not copy it onto the generated class, so the "rabbitmq.uri" property is never
# requested from test resources ("Error resolving property value [${rabbitmq.servers.product-cluster.port}]").
# The provider would then be instantiated by reflection before the GraalPy runtime exists.
@Disabled("TODO(python): @TestResourcesProperties is not applied to a Python test class yet")
# tag::test[]
@MicronautTest
@TestResourcesProperties(
    value=["rabbitmq.uri"],  # <1>
    providers=[RabbitMQProvider]  # <2>
)
class ConnectionSpec:

    product_cluster_port: Annotated[int, Value("${rabbitmq.servers.product-cluster.port}")]

    @Test
    def the_product_cluster_port_is_derived_from_the_rabbitmq_uri(self) -> None:
        assert self.product_cluster_port == 5672
# end::test[]
