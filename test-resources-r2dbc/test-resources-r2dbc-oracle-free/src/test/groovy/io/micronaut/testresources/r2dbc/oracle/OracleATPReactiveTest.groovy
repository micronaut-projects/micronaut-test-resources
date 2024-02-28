package io.micronaut.testresources.r2dbc.oracle

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import spock.lang.Issue

class OracleATPReactiveTest extends AbstractJDBCSpec {

    @Issue("https://github.com/micronaut-projects/micronaut-test-resources/issues/104")
    def "an Oracle container isn't started if ATP prod environment is detected"() {
        // because we test a production environment, ideally this should actually spawn a
        // mock for Oracle ATP, but for now we will just check that the properties are
        // not resolved by the test resources provider
        when:
        ApplicationContext.builder(environments as String[])
                .packages("io.micronaut.testresources.jdbc.free")
                .start()

        then:
        BeanInstantiationException ex = thrown()
        ex.message.contains("Test resources doesn't support resolving expression '$expression")

        where:
        environments                             | expression
        ["test", "jdbc", "prod"]                 | "datasources.default"
        ["test", "standalone", "standaloneprod"] | "r2dbc.datasources.default"

    }

    @Override
    String getImageName() {
        "oracle"
    }
}
