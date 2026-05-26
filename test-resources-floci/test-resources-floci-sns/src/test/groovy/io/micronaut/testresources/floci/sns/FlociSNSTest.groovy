package io.micronaut.testresources.floci.sns

import spock.lang.Specification

class FlociSNSTest extends Specification {

    def "exposes SNS endpoint override property"() {
        expect:
        new FlociSNSService().resolvableProperties == ["aws.services.sns.endpoint-override"]
    }
}
