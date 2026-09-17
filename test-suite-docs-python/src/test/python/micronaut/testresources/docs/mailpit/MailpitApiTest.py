# tag::imports[]
from typing import Annotated

from java.net import URI
from micronaut.context.annotation import Value
from micronaut.http import HttpRequest
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test
# end::imports[]
from micronaut.context.annotation import Property


@Property(name="mailpit.api.url", value="http://localhost:8025")
# tag::clazz[]
@MicronautTest
class MailpitApiTest:

    mailpit_api_url: Annotated[URI, Value("${mailpit.api.url}")]  # <1>

    @Test
    def the_messages_are_listed_through_the_mailpit_api(self) -> None:
        request = HttpRequest.GET(self.mailpit_api_url.resolve("/api/v1/messages"))  # <2>
        # end::clazz[]
        assert request.getUri().toString() == "http://localhost:8025/api/v1/messages"
        # tag::clazz[]
# end::clazz[]
