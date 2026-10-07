package io.micronaut.testresources.docsupport

import io.micronaut.test.extensions.testresources.TestResourcesClientHolder
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

/**
 * Installs the [DocsTestResourcesClient] for the duration of the test plan.
 */
class DocsTestResourcesClientInjector : TestExecutionListener {
    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        TestResourcesClientHolder.set(DocsTestResourcesClient())
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        TestResourcesClientHolder.set(null)
    }
}
