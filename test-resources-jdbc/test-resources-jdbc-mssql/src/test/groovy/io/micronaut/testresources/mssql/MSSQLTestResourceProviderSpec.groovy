package io.micronaut.testresources.mssql

import io.micronaut.core.util.StringUtils;
import spock.lang.Specification;

class MSSQLTestResourceProviderSpec extends Specification {

    void "verify String is parsed into boolean for the value of accept-license"(Object value) {
        String licenseKey = "containers.mssql.accept-license"
        Map<String, Object> testResourcesConfiguration = [(licenseKey): value]
        when:
        boolean accept = MSSQLTestResourceProvider.shouldAcceptLicense(licenseKey, testResourcesConfiguration)

        then:
        noExceptionThrown()
        accept

        where:
        value << [StringUtils.TRUE, true, Boolean.TRUE]
    }

    void "shared resource name reuse stays enabled when db-name is set"() {
        given:
        def provider = new MSSQLTestResourceProvider()
        def properties = [
                "datasources.default.db-type": "mssql",
                "datasources.default.db-name": "app_db",
                "datasources.default.test-resources.resource-name": "shared-mssql"
        ]

        expect:
        provider.getContainerOwnerKey("datasources.default.url", properties, [:]) == "mssql"
        provider.getContainerQuery("datasources.default.url", properties, [:]) == [
                "test-resources.resource-name": "shared-mssql"
        ]
    }

}
