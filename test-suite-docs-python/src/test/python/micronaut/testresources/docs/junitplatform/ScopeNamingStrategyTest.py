# tag::imports[]
from micronaut.test.extensions.junit5 import ScopeHolder
from micronaut.test.extensions.junit5.annotation import MicronautTest, ScopeNamingStrategy, TestResourcesScope
from org.junit.jupiter.api import Test
# end::imports[]


# tag::clazz[]
@MicronautTest
@TestResourcesScope(namingStrategy=ScopeNamingStrategy.TestClassName)  # <1>
class ScopeNamingStrategyTest:

    @Test
    def test_something(self) -> None:
        assert ScopeHolder.get().orElse(None) == "micronaut.testresources.docs.junitplatform.ScopeNamingStrategyTest"  # <2>
# end::clazz[]
