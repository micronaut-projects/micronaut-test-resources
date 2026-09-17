from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .JavamailSessionProvider import JavamailSessionProvider


@MicronautTest
@Property(name="smtp.host", value="localhost")
@Property(name="smtp.port", value="2525")
class JavamailSessionProviderTest:

    session_provider: Annotated[JavamailSessionProvider, Inject]

    @Test
    def the_session_uses_the_container_host_and_port(self) -> None:
        session = self.session_provider.session()
        assert session.getProperty("mail.smtp.host") == "localhost"
        assert session.getProperty("mail.smtp.port") == "2525"
