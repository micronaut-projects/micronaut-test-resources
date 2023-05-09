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
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.extension.IMethodInvocation;
import org.spockframework.runtime.model.SpecInfo;

import static io.micronaut.test.extensions.junit5.TestResourcesScopeListener.findTestResourceScopeAnnotation;

/**
 * A Spock extension which handles fields annotated with the shared
 * annotation.
 */
public class SpockScopeExtension implements IGlobalExtension {
    @Override
    public void visitSpec(SpecInfo spec) {
        spec.addSharedInitializerInterceptor(invocation -> {
            maybeSetScope(invocation);
            invocation.proceed();
        });
    }

    private static void maybeSetScope(IMethodInvocation invocation) {
        var clazz = invocation.getInstance().getClass();
        findTestResourceScopeAnnotation(clazz).ifPresent(scopeAnn -> {
            String scopeName = scopeAnn.value();
            if (scopeName == null || scopeName.isEmpty()) {
                var namingStrategy = scopeAnn.namingStrategy();
                if (!namingStrategy.equals(ScopeNamingStrategy.class)) {
                    var scopeNamingStrategy = TestResourcesScopeListener.instantitateStrategy(namingStrategy);
                    scopeName = scopeNamingStrategy.scopeNameFor(clazz);
                }
            }
            ScopeHolder.set(scopeName);
        });
    }
}
