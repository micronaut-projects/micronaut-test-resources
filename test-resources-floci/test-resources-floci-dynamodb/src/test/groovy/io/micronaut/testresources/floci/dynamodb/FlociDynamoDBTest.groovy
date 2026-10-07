package io.micronaut.testresources.floci.dynamodb

import spock.lang.Specification

class FlociDynamoDBTest extends Specification {

    def "exposes DynamoDB endpoint override property"() {
        expect:
        new FlociDynamoDBService().resolvableProperties == ["aws.services.dynamodb.endpoint-override"]
    }
}
