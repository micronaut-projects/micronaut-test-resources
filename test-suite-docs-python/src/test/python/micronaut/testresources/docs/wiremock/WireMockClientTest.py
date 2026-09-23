# tag::imports[]
from typing import Annotated

from com.github.tomakehurst.wiremock.client import WireMock
from java.net import URI
from micronaut.context.annotation import Value
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test
# end::imports[]
from micronaut.context.annotation import Property


@Property(name="wiremock.url", value="http://localhost:8080")
# tag::clazz[]
@MicronautTest
class WireMockClientTest:

    wire_mock_url: Annotated[str, Value("${wiremock.url}")]  # <1>

    @Test
    def the_wiremock_client_is_configured_with_the_container_url(self) -> None:
        uri = URI.create(self.wire_mock_url)
        WireMock.configureFor(uri.getHost(), uri.getPort())  # <2>
        # end::clazz[]
        assert uri.getHost() == "localhost"
        assert uri.getPort() == 8080
        # tag::clazz[]
# end::clazz[]
