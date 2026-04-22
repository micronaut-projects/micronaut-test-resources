package io.micronaut.testresources.testcontainers

import io.micronaut.testresources.core.Scope
import org.testcontainers.containers.GenericContainer
import spock.lang.Specification

class TestContainersTest extends Specification {

    def cleanup() {
        TestContainers.closeAll()
    }

    def "closing root scope closes all"() {
        def container1 = Stub(GenericContainer)
        def container2 = Stub(GenericContainer)
        def container3 = Stub(GenericContainer)
        create("c1", null, container1)
        create("c2", "child", container2)
        create("c3", "child.nested", container3)

        when:
        TestContainers.closeScope(null)

        then:
        TestContainers.listAll() == [:]
    }

    def "closing one scope closes nested scopes"() {
        def container1 = Stub(GenericContainer)
        def container2 = Stub(GenericContainer)
        def container3 = Stub(GenericContainer)
        create("c1", null, container1)
        create("c2", "child", container2)
        create("c3", "child.nested", container3)

        when:
        TestContainers.closeScope("child")

        then:
        TestContainers.listAll() == [
                (Scope.of(null)): [container1]
        ]
    }

    def "closing a leaf doesn't close parents"() {
        def container1 = Stub(GenericContainer)
        def container2 = Stub(GenericContainer)
        def container3 = Stub(GenericContainer)
        create("c1", null, container1)
        create("c2", "child", container2)
        create("c3", "child.nested", container3)

        when:
        TestContainers.closeScope("child.nested")


        then:
        TestContainers.listAll() == [
                (Scope.of(null)): [container1],
                (Scope.of("child")) : [container2]
        ]
    }

    def "normalized container identity can reuse a container across requested properties"() {
        def container = Stub(GenericContainer)
        int created = 0

        when:
        def jdbcContainer = TestContainers.getOrCreate("datasources.read.url", "postgres", "postgres", Scope.ROOT, [
                "test-resources.resource-name": "shared"
        ], () -> null) { imageName ->
            created++
            container
        }
        def r2dbcContainer = TestContainers.getOrCreate("r2dbc.datasources.write.url", "postgres", "postgres", Scope.ROOT, [
                "test-resources.resource-name": "shared"
        ], () -> null) { imageName ->
            created++
            Stub(GenericContainer)
        }

        then:
        jdbcContainer.is(r2dbcContainer)
        created == 1
        TestContainers.findByRequestedProperty(Scope.ROOT, "datasources.read.url") == [container]
        TestContainers.findByRequestedProperty(Scope.ROOT, "r2dbc.datasources.write.url") == [container]
    }

    def "different shared resource names still create different containers"() {
        def first = Stub(GenericContainer)
        def second = Stub(GenericContainer)

        when:
        def readContainer = TestContainers.getOrCreate("datasources.read.url", "postgres", "postgres", Scope.ROOT, [
                "test-resources.resource-name": "primary"
        ], () -> null) { imageName ->
            first
        }
        def writeContainer = TestContainers.getOrCreate("datasources.write.url", "postgres", "postgres", Scope.ROOT, [
                "test-resources.resource-name": "reporting"
        ], () -> null) { imageName ->
            second
        }

        then:
        !readContainer.is(writeContainer)
    }

    def "normalized identity still isolates containers by scope"() {
        def rootContainer = Stub(GenericContainer)
        def childContainer = Stub(GenericContainer)

        when:
        def root = TestContainers.getOrCreate("datasources.default.url", "postgres", "postgres", Scope.of(null), [
                "test-resources.resource-name": "shared"
        ], () -> null) { imageName ->
            rootContainer
        }
        def child = TestContainers.getOrCreate("datasources.default.url", "postgres", "postgres", Scope.of("child"), [
                "test-resources.resource-name": "shared"
        ], () -> null) { imageName ->
            childContainer
        }

        then:
        !root.is(child)
    }

    def "remembered databases are tracked per container"() {
        def first = Stub(GenericContainer)
        def second = Stub(GenericContainer)

        when:
        TestContainers.rememberDatabase(first, "read_db")
        TestContainers.rememberDatabase(first, "write_db")
        TestContainers.rememberDatabase(second, "reporting_db")

        then:
        TestContainers.hasDatabase(first, "read_db")
        TestContainers.hasDatabase(first, "write_db")
        !TestContainers.hasDatabase(first, "reporting_db")
        TestContainers.hasDatabase(second, "reporting_db")
    }

    void create(String name, String scope, GenericContainer container) {
        TestContainers.getOrCreate("foo", TestContainersTest.name, name, Scope.of(scope), [
                (Scope.PROPERTY_KEY): scope
        ], () -> null) { imageName ->
            container
        }
    }
}
