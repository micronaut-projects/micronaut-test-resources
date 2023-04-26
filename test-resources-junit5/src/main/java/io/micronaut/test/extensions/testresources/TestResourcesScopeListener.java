/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.test.extensions.testresources;

import io.micronaut.test.extensions.testresources.annotation.TestResourcesScope;
import io.micronaut.testresources.client.TestResourcesClient;
import io.micronaut.testresources.client.TestResourcesClientFactory;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A listener which is responsible for shutting down test resources for a
 * particular scope once the last test which uses this scope is finished.
 */
public class TestResourcesScopeListener implements TestExecutionListener {
    private final Map<String, Set<String>> testsUsingResources = new ConcurrentHashMap<>();
    private final TestResourcesClient testResourcesClient;

    public TestResourcesScopeListener() {
        testResourcesClient = TestResourcesClientFactory.fromSystemProperties()
            .orElse(TestResourcesClientHolder.lazy());
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if (testResourcesClient != null) {
            Set<TestIdentifier> roots = testPlan.getRoots();
            visitTestIdentifiers(roots, testPlan);
        }
    }


    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult testExecutionResult) {
        if (testResourcesClient != null) {
            visitTestIdentifier(id, EventKind.TEST_FINISHED);
        }
    }

    private void visitTestIdentifiers(Set<TestIdentifier> ids, TestPlan testPlan) {
        for (TestIdentifier id : ids) {
            visitTestIdentifier(id, EventKind.TEST_REGISTERED);
            visitTestIdentifiers(testPlan.getChildren(id), testPlan);
        }
    }

    private void visitTestIdentifier(TestIdentifier id, EventKind kind) {
        id.getSource().ifPresent(source -> {
            if (source instanceof ClassSource classSource) {
                TestResourcesScope testResourcesScope = classSource.getJavaClass().getAnnotation(TestResourcesScope.class);
                visitTestIdentifierWithAnnotation(id, kind, testResourcesScope);
            } else if (source instanceof MethodSource methodSource) {
                TestResourcesScope testResourcesScope = methodSource.getJavaMethod().getAnnotation(TestResourcesScope.class);
                visitTestIdentifierWithAnnotation(id, kind, testResourcesScope);
            }
        });
    }

    private void visitTestIdentifierWithAnnotation(TestIdentifier id, EventKind kind, TestResourcesScope testResourcesScope) {
        if (testResourcesScope != null) {
            String[] scopeNames = testResourcesScope.value();
            for (String scopeName :scopeNames) {
                visitRequiredScope(id, kind, scopeName);
            }
        }
    }

    private void visitRequiredScope(TestIdentifier id, EventKind kind, String scopeName) {
        Set<String> testIdentifiers = testsUsingResources.computeIfAbsent(scopeName, scope -> new ConcurrentSkipListSet<>());
        String testId = id.getUniqueId();
        switch (kind) {
            case TEST_REGISTERED -> testIdentifiers.add(testId);
            case TEST_FINISHED -> {
                if (testIdentifiers.remove(testId)) {
                    if (testIdentifiers.isEmpty()) {
                        testResourcesClient.closeScope(scopeName);
                    }
                }
            }
        }
    }

    private enum EventKind {
        TEST_REGISTERED,
        TEST_FINISHED
    }
}
