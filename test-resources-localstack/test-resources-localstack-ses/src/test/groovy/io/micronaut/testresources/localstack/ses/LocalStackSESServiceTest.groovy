package io.micronaut.testresources.localstack.ses

class LocalStackSESServiceTest extends spock.lang.Specification {

    def "exposes SES endpoint override property"() {
        expect:
        new LocalStackSESService().resolvableProperties == ["aws.services.ses.endpoint-override"]
    }
}
