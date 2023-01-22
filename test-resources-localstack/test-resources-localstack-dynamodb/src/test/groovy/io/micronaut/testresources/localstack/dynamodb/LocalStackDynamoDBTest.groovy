package io.micronaut.testresources.localstack.dynamodb


import io.micronaut.context.annotation.ConfigurationBuilder
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.localstack.AbstractLocalStackSpec
import jakarta.inject.Inject
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

@MicronautTest
class LocalStackDynamoDBTest extends AbstractLocalStackSpec {

    @Inject
    DynamoDBConfig dynamoConfig

    def "automatically starts an DynamoDB container"() {
        given:
        def client = buildClient()
        client.createTable {
            it.tableName('test-table')
            it.keySchema(KeySchemaElement.builder()
                    .attributeName('id')
                    .keyType(KeyType.HASH)
                    .build()
            )
            it.attributeDefinitions(AttributeDefinition.builder()
                    .attributeName('name')
                    .attributeType(ScalarAttributeType.S)
                    .build()
            )
            it.attributeDefinitions(AttributeDefinition.builder()
                    .attributeName('id')
                    .attributeType(ScalarAttributeType.S)
                    .build()
            )
            it.provisionedThroughput {
                it.readCapacityUnits(1L)
                it.writeCapacityUnits(1L)
            }
        }

        when:
        client.putItem {
            it.tableName("test-table")
            it.item(['id': AttributeValue.builder().s("1").build(), 'name': AttributeValue.builder().s("Cédric").build()])
        }

        then:
        def read = client.getItem {
            it.tableName("test-table")
            it.key(['id': AttributeValue.builder().s("1").build()])
        }
        read.item().get('name').s() == 'Cédric'

        and:
        listContainers().size() == 1
    }

    private DynamoDbClient buildClient() {
        DynamoDbClient.builder()
                .endpointOverride(new URI(dynamoConfig.dynamodb.endpointOverride))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(dynamoConfig.accessKeyId, dynamoConfig.secretKey)
                        )
                )
                .region(Region.of(dynamoConfig.region))
                .build()
    }

    @ConfigurationProperties("aws")
    static class DynamoDBConfig {
        String accessKeyId
        String secretKey
        String region

        @ConfigurationBuilder(configurationPrefix = "services.dynamodb")
        final Dynamo dynamodb = new Dynamo()

        static class Dynamo {
            String endpointOverride
        }
    }
}
