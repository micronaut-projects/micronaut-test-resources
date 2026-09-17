# tag::imports[]
from typing import Annotated

from micronaut.context.annotation import Value
from micronaut.test.extensions.junit5.annotation import MicronautTest
from micronaut.test.support import TestPropertyProvider
from org.junit.jupiter.api import Disabled, Test, TestInstance
# end::imports[]


# TODO(python): TestPropertyProvider.getProperties() is called by Micronaut Test before the application
# context, and with it the GraalPy runtime, exists ("GraalPy context has not been initialized"), so a Python
# test class cannot supply test properties this way yet.
@Disabled("TODO(python): a Python test class cannot implement TestPropertyProvider yet")
# tag::clazz[]
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyTest(TestPropertyProvider):

    some_property: Annotated[str, Value("${myapp.someProperty}")]

    def getProperties(self) -> dict[str, str]:  # <1>
        return {
            "myapp.someProperty": "value"
        }

    @Test
    def the_property_is_available_to_the_test(self) -> None:
        assert self.some_property == "value"
# end::clazz[]
