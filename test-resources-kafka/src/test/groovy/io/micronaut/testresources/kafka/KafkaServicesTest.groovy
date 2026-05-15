package io.micronaut.testresources.kafka

import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import io.micronaut.testresources.testcontainers.TestContainers
import spock.lang.Shared

class KafkaServicesTest extends AbstractTestContainersSpec {

    @Shared
    Set<String> scopes = []

    void cleanup() {
        scopes.each { TestContainers.closeScope(it) }
        scopes.clear()
    }

    def "resolves schema registry URL"() {
        given:
        def scope = "kafka-schema-registry"

        when:
        def url = resolve(new KafkaSchemaRegistryTestResourceProvider(), KafkaSchemaRegistryTestResourceProvider.KAFKA_SCHEMA_REGISTRY_URL, scope)

        then:
        url.toURI().scheme == "http"
        url.toURI().port == serviceContainer(scope, KafkaSchemaRegistryTestResourceProvider.DEFAULT_IMAGE).getMappedPort(KafkaSchemaRegistryTestResourceProvider.PORT)
        httpGet(url + "/subjects") == "[]"
        kafkaContainers(scope).size() == 1
    }

    def "resolves kafka connect URL"() {
        given:
        def scope = "kafka-connect"

        when:
        def url = resolve(new KafkaConnectTestResourceProvider(), KafkaConnectTestResourceProvider.KAFKA_CONNECT_URL, scope)

        then:
        url.toURI().scheme == "http"
        url.toURI().port == serviceContainer(scope, KafkaConnectTestResourceProvider.DEFAULT_IMAGE).getMappedPort(KafkaConnectTestResourceProvider.PORT)
        httpGet(url + "/connectors") == "[]"
        kafkaContainers(scope).size() == 1
    }

    def "resolves ksqldb URL"() {
        given:
        def scope = "kafka-ksqldb"

        when:
        def url = resolve(new KafkaKsqlDbTestResourceProvider(), KafkaKsqlDbTestResourceProvider.KAFKA_KSQLDB_URL, scope)

        then:
        url.toURI().scheme == "http"
        url.toURI().port == serviceContainer(scope, KafkaKsqlDbTestResourceProvider.DEFAULT_IMAGE).getMappedPort(KafkaKsqlDbTestResourceProvider.PORT)
        httpGet(url + "/info").contains("KsqlServerInfo")
        kafkaContainers(scope).size() == 1
    }

    def "reuses kafka container for broker and service URLs"() {
        given:
        def scope = "kafka-reuse"
        def requestedProperties = scopedProperties(scope)

        when:
        def bootstrapServers = new KafkaTestResourceProvider()
                .resolve(KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS, requestedProperties, [:])
                .get()
        def schemaRegistryUrl = new KafkaSchemaRegistryTestResourceProvider()
                .resolve(KafkaSchemaRegistryTestResourceProvider.KAFKA_SCHEMA_REGISTRY_URL, requestedProperties, [:])
                .get()
        def connectUrl = new KafkaConnectTestResourceProvider()
                .resolve(KafkaConnectTestResourceProvider.KAFKA_CONNECT_URL, requestedProperties, [:])
                .get()

        then:
        bootstrapServers
        schemaRegistryUrl
        connectUrl
        kafkaContainers(scope).size() == 1
        TestContainers.listByScope(scope).get(Scope.of(scope)).size() == 3
    }

    def "supports image overrides for new providers"() {
        given:
        def scope = "kafka-schema-registry-custom-image"

        when:
        def url = new KafkaSchemaRegistryTestResourceProvider()
                .resolve(
                        KafkaSchemaRegistryTestResourceProvider.KAFKA_SCHEMA_REGISTRY_URL,
                        scopedProperties(scope),
                        ["containers.kafka-schema-registry.image-name": "confluentinc/cp-schema-registry"]
                )
                .get()

        then:
        url
        serviceContainer(scope, "confluentinc/cp-schema-registry:latest").dockerImageName == "confluentinc/cp-schema-registry:latest"
    }

    def "declares default images"() {
        expect:
        KafkaSchemaRegistryTestResourceProvider.DEFAULT_IMAGE == "confluentinc/cp-schema-registry:8.2.0"
        KafkaConnectTestResourceProvider.DEFAULT_IMAGE == "confluentinc/cp-kafka-connect:8.2.0"
        KafkaKsqlDbTestResourceProvider.DEFAULT_IMAGE == "confluentinc/ksqldb-server:0.29.0"
    }

    def "only publishes service URL properties when requested"() {
        expect:
        new KafkaSchemaRegistryTestResourceProvider().getResolvableProperties([:], [:]).empty
        new KafkaConnectTestResourceProvider().getResolvableProperties([:], [:]).empty
        new KafkaKsqlDbTestResourceProvider().getResolvableProperties([:], [:]).empty

        new KafkaSchemaRegistryTestResourceProvider().getResolvableProperties(["kafka.schema.registry": ["url"]], [:]) ==
                [KafkaSchemaRegistryTestResourceProvider.KAFKA_SCHEMA_REGISTRY_URL]
        new KafkaConnectTestResourceProvider().getResolvableProperties(["kafka.connect": ["url"]], [:]) ==
                [KafkaConnectTestResourceProvider.KAFKA_CONNECT_URL]
        new KafkaKsqlDbTestResourceProvider().getResolvableProperties(["kafka.ksqldb": ["url"]], [:]) ==
                [KafkaKsqlDbTestResourceProvider.KAFKA_KSQLDB_URL]
    }

    private String resolve(provider, String property, String scope) {
        provider.resolve(property, scopedProperties(scope), [:]).get()
    }

    private Map<String, Object> scopedProperties(String scope) {
        scopes << scope
        [(Scope.PROPERTY_KEY): scope]
    }

    private static List kafkaContainers(String scope) {
        TestContainers.findByRequestedProperty(Scope.of(scope), KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS)
    }

    private static serviceContainer(String scope, String imageName) {
        TestContainers.listByScope(scope)
                .get(Scope.of(scope))
                .find { it.dockerImageName == imageName }
    }

    private static String httpGet(String url) {
        url.toURL().text
    }
}
