# tag::imports[]
from micronaut.test.extensions.junit5 import ScopeHolder
from micronaut.test.extensions.junit5.annotation import MicronautTest, TestResourcesScope
from org.junit.jupiter.api import Test
# end::imports[]


# tag::clazz[]
@MicronautTest
@TestResourcesScope("my scope")  # <1>
class ScopeNameTest:

    @Test
    def test_something(self) -> None:
        assert ScopeHolder.get().orElse(None) == "my scope"  # <2>
# end::clazz[]
