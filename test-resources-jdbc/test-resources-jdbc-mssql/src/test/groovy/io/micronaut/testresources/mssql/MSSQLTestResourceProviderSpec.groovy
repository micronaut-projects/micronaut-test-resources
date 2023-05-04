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


}
