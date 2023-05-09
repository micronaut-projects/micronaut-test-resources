/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.test.extensions.junit5;

import io.micronaut.test.extensions.junit5.annotation.ScopeNamingStrategy;
import io.micronaut.test.extensions.junit5.annotation.TestResourcesScope;
import io.micronaut.test.extensions.testresources.TestResourcesClientHolder;
import io.micronaut.testresources.client.TestResourcesClient;
import io.micronaut.testresources.client.TestResourcesClientFactory;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A listener which is responsible for shutting down test resources for a
 * particular scope once the last test which uses this scope is finished.
 */
public class TestResourcesScopeListener implements TestExecutionListener {
    private final Map<String, Set<String>> testsUsingResources = new ConcurrentHashMap<>();
    private TestResourcesClient testResourcesClient;
    private final Deque<String> nestedScopes = new ArrayDeque<>();

    /**
     * Creation of the test resources client is deferred at execution time,
     * so that the test resources client holder is not initialized at build
     * time in native image.
     */
    private void assertTestResourcesClient() {
        testResourcesClient = TestResourcesClientFactory.fromSystemProperties()
            .orElse(TestResourcesClientHolder.lazy());
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        assertTestResourcesClient();
        Set<TestIdentifier> roots = testPlan.getRoots();
        visitTestIdentifiers(roots, testPlan);
    }

    @Override
    public void executionStarted(TestIdentifier id) {
        assertTestResourcesClient();
        visitTestIdentifier(id, EventKind.TEST_STARTED);
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult testExecutionResult) {
        assertTestResourcesClient();
        visitTestIdentifier(id, EventKind.TEST_FINISHED);
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
                findTestResourceScopeAnnotation(classSource.getJavaClass()).ifPresent(testResourcesScope -> {
                    visitTestIdentifierWithAnnotation(id, kind, testResourcesScope);
                });
            }
        });
    }

    @SuppressWarnings({"java:S3776", "java:S3655"})
    private void visitTestIdentifierWithAnnotation(TestIdentifier id, EventKind kind, TestResourcesScope testResourcesScope) {
        if (id.getSource().isPresent()) {
            var testSource = id.getSource().get();
            if (testSource instanceof ClassSource classSource) {
                Class<?> testClass = classSource.getJavaClass();
                if (testResourcesScope != null) {
                    Optional<String> scope = findScope(testResourcesScope, testClass);
                    scope.ifPresent(scopeName -> {
                        if (!scopeName.isEmpty()) {
                            visitRequiredScope(id, kind, scopeName);
                        }
                    });
                }
            }
        }
    }

    private static Optional<String> findScope(TestResourcesScope testResourcesScope, Class<?> testClass) {
        String scopeName = testResourcesScope.value();
        if (scopeName == null || scopeName.isEmpty()) {
            Class<? extends ScopeNamingStrategy> namingStrategy = testResourcesScope.namingStrategy();
            if (!namingStrategy.equals(ScopeNamingStrategy.class)) {
                var scopeNamingStrategy = instantitateStrategy(namingStrategy);
                scopeName = scopeNamingStrategy.scopeNameFor(testClass);
            }
        }
        return Optional.ofNullable(scopeName);
    }

    private void visitRequiredScope(TestIdentifier id, EventKind kind, String scopeName) {
        Set<String> testIdentifiers = testsUsingResources.computeIfAbsent(scopeName, scope -> new ConcurrentSkipListSet<>());
        String testId = id.getUniqueId();
        switch (kind) {
            case TEST_REGISTERED -> testIdentifiers.add(testId);
            case TEST_STARTED -> {
                ScopeHolder.get().ifPresent(nestedScopes::push);
                ScopeHolder.set(scopeName);
            }
            case TEST_FINISHED -> {
                // We need to make sure the test id was known, because kotest
                // can issue new test ids which weren't known at registration
                if (testIdentifiers.remove(testId)) {
                    ScopeHolder.set(nestedScopes.poll());
                    if (ScopeHolder.get().isEmpty()) {
                        ScopeHolder.remove();
                    }
                    if (testIdentifiers.isEmpty()) {
                        testResourcesClient.closeScope(scopeName);
                    }
                }
            }
        }
    }

    public static Optional<TestResourcesScope> findTestResourceScopeAnnotation(Class<?> clazz) {
        return Optional.ofNullable(clazz.getAnnotation(TestResourcesScope.class)).or(() -> findTestResourceScopeAnnotationFromInterfaces(clazz));
    }

    private static Optional<TestResourcesScope> findTestResourceScopeAnnotationFromInterfaces(Class<?> clazz) {
        var annotatedInterfaces = clazz.getAnnotatedInterfaces();
        Map<Class<?>, TestResourcesScope> foundScopes = new LinkedHashMap<>();
        collectScopes(annotatedInterfaces, foundScopes);
        if (foundScopes.size() > 1) {
            // We use System.err instead of an exception here, because the error will happen
            // in the context of JUnit Platform listeners, which will _not_ fail the tests.
            // Neither can we use a logger, since there's no logging library on classpath
            // to reduce the risks of conflicts with user defined logging libraries.
            var first = foundScopes.entrySet().stream().findFirst().get();
            System.err.println("[WARNING] Multiple interfaces declare a test resources scope. " +
                               "Only one can be used, make sure to annotate your class instead. " +
                               "Using scope declared in " + first.getKey());
            return Optional.of(first.getValue());
        }
        return foundScopes.values().stream().findFirst();
    }

    private static void collectScopes(AnnotatedType[] annotatedInterfaces, Map<Class<?>, TestResourcesScope> foundScopes) {
        for (AnnotatedType annotatedInterface : annotatedInterfaces) {
            if (annotatedInterface.getType() instanceof Class<?> annotatedClass) {
                var annotation = annotatedClass.getAnnotation(TestResourcesScope.class);
                if (annotation != null) {
                    foundScopes.put(annotatedClass, annotation);
                } else {
                    collectScopes(annotatedClass.getAnnotatedInterfaces(), foundScopes);
                }
            }
        }
    }

    static ScopeNamingStrategy instantitateStrategy(Class<? extends ScopeNamingStrategy> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Scope naming strategy must have a public constructor without arguments", e);
        }
    }

    private enum EventKind {
        TEST_REGISTERED,
        TEST_STARTED,
        TEST_FINISHED
    }
}
