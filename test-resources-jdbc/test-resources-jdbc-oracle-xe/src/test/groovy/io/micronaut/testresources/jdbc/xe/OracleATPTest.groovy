package io.micronaut.testresources.jdbc.xe

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import spock.lang.Issue

class OracleATPTest extends AbstractJDBCSpec {

    @Issue("https://github.com/micronaut-projects/micronaut-test-resources/issues/104")
    def "an Oracle container isn't started if ATP prod environment is detected"() {
        // because we test a production environment, ideally this should actually spawn a
        // mock for Oracle ATP, but for now we will just check that the properties are
        // not resolved by the test resources provider
        when:
        ApplicationContext.builder("test", "prod")
            .packages("io.micronaut.testresources.jdbc.xe")
            .start()

        then:
        BeanInstantiationException ex = thrown()
        ex.message.contains("Could not resolve placeholder \${auto.test.resources.datasources.default")
    }

    @Override
    String getImageName() {
        "oracle-xe"
    }
}
