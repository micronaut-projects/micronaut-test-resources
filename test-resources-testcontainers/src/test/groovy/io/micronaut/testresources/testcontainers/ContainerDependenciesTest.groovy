package io.micronaut.testresources.testcontainers

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import spock.lang.Specification

@MicronautTest
class ContainerDependenciesTest extends Specification {
    // we only inject the consumer, in order to make sure that the
    // dependency on the producer is triggered
    @Value("\${consumer.host}") String consumerHostname

    def "test containers on same network can talk to each other"() {
        when:
        // Please note that a user wouldn't be able to communicate
        // directly to the container, it's internal API of the process
        // which spawns the test containers, which happens to be the
        // test process here
        def networks = TestContainers.networks
        def stdout = TestContainers.listAll()
                .get(Scope.ROOT)
                .find {
                    it.networkAliases.contains('alice')
                }
                ?.execInContainer("wget", "-O", "-", "http://bob:8080")
                ?.stdout

        then:
        networks.size() == 1
        networks['custom'].close()
        "yay" == stdout
    }
}
