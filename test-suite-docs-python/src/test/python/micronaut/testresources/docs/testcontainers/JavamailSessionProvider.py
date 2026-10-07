# tag::imports[]
from typing import Annotated

from jakarta.inject import Singleton
from jakarta.mail import Session
from java.util import Properties
from micronaut.context.annotation import Value
from micronaut.email.javamail.sender import SessionProvider
# end::imports[]


# tag::clazz[]
@Singleton
class JavamailSessionProvider(SessionProvider):
    smtp_host: Annotated[str, Value("${smtp.host}")]  # <1>

    smtp_port: Annotated[str, Value("${smtp.port}")]  # <2>

    def session(self) -> Session:
        props = Properties()
        props.put("mail.smtp.host", self.smtp_host)
        props.put("mail.smtp.port", self.smtp_port)
        return Session.getDefaultInstance(props)
# end::clazz[]
