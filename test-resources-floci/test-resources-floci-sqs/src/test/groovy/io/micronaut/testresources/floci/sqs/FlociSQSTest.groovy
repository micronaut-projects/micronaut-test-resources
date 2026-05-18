package io.micronaut.testresources.floci.sqs

import spock.lang.Specification

class FlociSQSTest extends Specification {

    def "exposes SQS endpoint override property"() {
        expect:
        new FlociSQSService().resolvableProperties == ["aws.services.sqs.endpoint-override"]
    }
}
