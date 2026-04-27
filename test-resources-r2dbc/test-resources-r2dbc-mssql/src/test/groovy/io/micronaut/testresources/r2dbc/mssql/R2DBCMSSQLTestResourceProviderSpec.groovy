package io.micronaut.testresources.r2dbc.mssql

import spock.lang.Specification

class R2DBCMSSQLTestResourceProviderSpec extends Specification {

    void "shared resource name reuse stays enabled when db-name is set"() {
        given:
        def provider = new R2DBCMSSQLTestResourceProvider()
        def properties = [
                "r2dbc.datasources.default.db-type": "mssql",
                "r2dbc.datasources.default.db-name": "app_db",
                "r2dbc.datasources.default.test-resources.resource-name": "shared-mssql"
        ]

        expect:
        provider.getContainerOwnerKey("r2dbc.datasources.default.url", properties, [:]) == "mssql"
        provider.getContainerQuery("r2dbc.datasources.default.url", properties, [:]) == [
                "test-resources.resource-name": "shared-mssql"
        ]
    }
}
