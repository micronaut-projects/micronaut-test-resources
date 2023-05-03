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

    void create(String name, String scope, GenericContainer container) {
        TestContainers.getOrCreate("foo", TestContainersTest, name, [
                (Scope.PROPERTY_KEY): scope
        ]) {
            container
        }
    }
}
