package io.micronaut.testresources.r2dbc.postgres

import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.postgres.PostgreSQLTestResourceProvider
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import io.micronaut.testresources.testcontainers.TestContainers

class SharedResourceNamePostgreSQLTest extends AbstractTestContainersSpec {

    void "shared resource name reuses one PostgreSQL container across JDBC and R2DBC databases"() {
        given:
        def jdbcProvider = new PostgreSQLTestResourceProvider()
        def r2dbcProvider = new R2DBCPostgreSQLTestResourceProvider()
        def jdbcProperties = [
                (Scope.PROPERTY_KEY): scopeName,
                "datasources.migrations.dialect": "POSTGRES",
                "datasources.migrations.db-name": "migrations_db",
                "datasources.migrations.test-resources.resource-name": "shared-postgres"
        ]
        def defaultR2dbcProperties = [
                (Scope.PROPERTY_KEY): scopeName,
                "r2dbc.datasources.default.dialect": "POSTGRES",
                "r2dbc.datasources.default.db-name": "default_db",
                "r2dbc.datasources.default.test-resources.resource-name": "shared-postgres"
        ]
        def reportingR2dbcProperties = [
                (Scope.PROPERTY_KEY): scopeName,
                "r2dbc.datasources.reporting.dialect": "POSTGRES",
                "r2dbc.datasources.reporting.db-name": "reporting_db",
                "r2dbc.datasources.reporting.test-resources.resource-name": "shared-postgres"
        ]

        when:
        def jdbcUrl = jdbcProvider.resolve("datasources.migrations.url", jdbcProperties, [:])
        def defaultR2dbcUrl = r2dbcProvider.resolve("r2dbc.datasources.default.url", defaultR2dbcProperties, [:])
        def reportingR2dbcUrl = r2dbcProvider.resolve("r2dbc.datasources.reporting.url", reportingR2dbcProperties, [:])
        def scope = Scope.of(scopeName)
        def jdbcContainers = TestContainers.findByRequestedProperty(scope, "datasources.migrations.url")
        def defaultContainers = TestContainers.findByRequestedProperty(scope, "r2dbc.datasources.default.url")
        def reportingContainers = TestContainers.findByRequestedProperty(scope, "r2dbc.datasources.reporting.url")
        def databases = jdbcContainers.first().execInContainer(
                "psql",
                "-U",
                jdbcContainers.first().username,
                "-d",
                jdbcContainers.first().databaseName,
                "-tAc",
                "SELECT datname FROM pg_database WHERE datname IN ('migrations_db', 'default_db', 'reporting_db') ORDER BY datname"
        ).stdout
                .readLines()
                .findAll { !it.isBlank() }

        then:
        jdbcUrl.present
        defaultR2dbcUrl.present
        reportingR2dbcUrl.present
        withoutQueryString(jdbcUrl.get()).endsWith("/migrations_db")
        withoutQueryString(defaultR2dbcUrl.get()).endsWith("/default_db")
        withoutQueryString(reportingR2dbcUrl.get()).endsWith("/reporting_db")
        jdbcContainers.size() == 1
        defaultContainers.size() == 1
        reportingContainers.size() == 1
        jdbcContainers.first().is(defaultContainers.first())
        defaultContainers.first().is(reportingContainers.first())
        databases == ["default_db", "migrations_db", "reporting_db"]
    }

    void "same-name JDBC reuse prepares the requested R2DBC database before returning the URL"() {
        given:
        def jdbcProvider = new PostgreSQLTestResourceProvider()
        def r2dbcProvider = new R2DBCPostgreSQLTestResourceProvider()
        def jdbcProperties = [
                (Scope.PROPERTY_KEY): scopeName,
                "datasources.default.dialect": "POSTGRES",
                "datasources.default.db-name": "migrations_db"
        ]

        when:
        def jdbcUrl = jdbcProvider.resolve("datasources.default.url", jdbcProperties, [:])
        def r2dbcProperties = [
                (Scope.PROPERTY_KEY)          : scopeName,
                "datasources.default.url"     : jdbcUrl.get(),
                "datasources.default.dialect" : "POSTGRES",
                "datasources.default.db-name" : "migrations_db",
                "r2dbc.datasources.default.dialect": "POSTGRES",
                "r2dbc.datasources.default.db-name": "reporting_db"
        ]
        def r2dbcUrl = r2dbcProvider.resolve("r2dbc.datasources.default.url", r2dbcProperties, [:])
        def scope = Scope.of(scopeName)
        def jdbcContainers = TestContainers.findByRequestedProperty(scope, "datasources.default.url")
        def databases = jdbcContainers.first().execInContainer(
                "psql",
                "-U",
                jdbcContainers.first().username,
                "-d",
                jdbcContainers.first().databaseName,
                "-tAc",
                "SELECT datname FROM pg_database WHERE datname IN ('migrations_db', 'reporting_db') ORDER BY datname"
        ).stdout
                .readLines()
                .findAll { !it.isBlank() }

        then:
        jdbcUrl.present
        r2dbcUrl.present
        jdbcContainers.size() == 1
        withoutQueryString(jdbcUrl.get()).endsWith("/migrations_db")
        withoutQueryString(r2dbcUrl.get()).endsWith("/reporting_db")
        databases == ["migrations_db", "reporting_db"]
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
