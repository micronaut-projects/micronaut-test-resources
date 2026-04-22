package io.micronaut.testresources.jdbc.mysql

import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.postgres.PostgreSQLTestResourceProvider
import io.micronaut.testresources.testcontainers.TestContainers
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

class SharedResourceNamePostgreSQLTest extends AbstractTestContainersSpec {
    void "shared resource name reuses one PostgreSQL container across distinct JDBC databases"() {
        given:
        def provider = new PostgreSQLTestResourceProvider()
        def readProperties = [
                (Scope.PROPERTY_KEY): scopeName,
                "datasources.read.dialect": "POSTGRES",
                "datasources.read.db-name": "read_db",
                "datasources.read.test-resources.resource-name": "shared-postgres"
        ]
        def writeProperties = [
                (Scope.PROPERTY_KEY): scopeName,
                "datasources.write.dialect": "POSTGRES",
                "datasources.write.db-name": "write_db",
                "datasources.write.test-resources.resource-name": "shared-postgres"
        ]

        when:
        def readUrl = provider.resolve("datasources.read.url", readProperties, [:])
        def writeUrl = provider.resolve("datasources.write.url", writeProperties, [:])
        def scope = Scope.of(scopeName)
        def readContainers = TestContainers.findByRequestedProperty(scope, "datasources.read.url")
        def writeContainers = TestContainers.findByRequestedProperty(scope, "datasources.write.url")
        def databases = readContainers.first().execInContainer(
                "psql",
                "-U",
                readContainers.first().username,
                "-d",
                readContainers.first().databaseName,
                "-tAc",
                "SELECT datname FROM pg_database WHERE datname IN ('read_db', 'write_db') ORDER BY datname"
        ).stdout
                .readLines()
                .findAll { !it.isBlank() }

        then:
        readUrl.present
        writeUrl.present
        readContainers.size() == 1
        writeContainers.size() == 1
        readContainers.first().is(writeContainers.first())
        withoutQueryString(readUrl.get()).endsWith("/read_db")
        withoutQueryString(writeUrl.get()).endsWith("/write_db")
        databases == ["read_db", "write_db"]
    }

    @Override
    String getImageName() {
        "postgres"
    }

    private static String withoutQueryString(String url) {
        int queryIndex = url.indexOf('?')
        queryIndex >= 0 ? url.substring(0, queryIndex) : url
    }
}
