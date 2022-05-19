package io.micronaut.testresources.embedded

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class EmbeddedTestResourcesTest extends Specification {
    @Inject
    ApplicationContext applicationContext

    def "resolves properties"() {
        when:
        def kafka = applicationContext.getBean(FakeKafka)

        then: "entries not configured explicitly are resolved"
        kafka.config == ["http://localhost:12345"]

        and: "entries with explicit user configuration take precedence"
        kafka.topic == "preserved"
    }

}
