package io.micronaut.testresources.docsupport

import io.micronaut.testresources.client.TestResourcesClient
import java.util.Optional

/**
 * A fake test resources client resolving the properties used by the examples of the guide, so that the
 * documentation test suite does not need a running test resources service.
 */
class DocsTestResourcesClient : TestResourcesClient {

    override fun getResolvableProperties(
        propertyEntries: Map<String, Collection<String>>,
        testResourcesConfig: Map<String, Any>
    ): List<String> = PROPERTIES.keys.toList()

    override fun resolve(name: String, properties: Map<String, Any>, testResourcesConfig: Map<String, Any>): Optional<String> =
        Optional.ofNullable(PROPERTIES[name])

    override fun getRequiredProperties(expression: String): List<String> = listOf()

    override fun getRequiredPropertyEntries(): List<String> = listOf()

    override fun closeAll(): Boolean = true

    override fun closeScope(id: String?): Boolean = true

    companion object {
        private val PROPERTIES = mapOf(
            "rabbitmq.uri" to "amqp://localhost:5672"
        )
    }
}
