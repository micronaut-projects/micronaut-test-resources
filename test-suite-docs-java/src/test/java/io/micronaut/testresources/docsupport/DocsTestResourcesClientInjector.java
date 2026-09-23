package io.micronaut.testresources.docsupport;

import io.micronaut.test.extensions.testresources.TestResourcesClientHolder;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Installs the {@link DocsTestResourcesClient} for the duration of the test plan.
 */
public class DocsTestResourcesClientInjector implements TestExecutionListener {
    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        TestResourcesClientHolder.set(new DocsTestResourcesClient());
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        TestResourcesClientHolder.set(null);
    }
}
