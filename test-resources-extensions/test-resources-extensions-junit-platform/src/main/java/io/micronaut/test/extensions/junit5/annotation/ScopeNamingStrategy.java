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
package io.micronaut.test.extensions.junit5.annotation;

import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestIdentifier;

/**
 * Provides the name of a test resources scope to
 * be used in a test class.
 */
@FunctionalInterface
public interface ScopeNamingStrategy {
    String scopeNameFor(TestIdentifier testId);

    class TestClassName implements ScopeNamingStrategy {
        @Override
        public String scopeNameFor(TestIdentifier testId) {
            var source = testId.getSource();
            if (source.isPresent()) {
                var testSource = source.get();
                if (testSource instanceof ClassSource classSource) {
                    return classSource.getClassName();
                }
            }
            return null;
        }
    }

    class PackageName implements ScopeNamingStrategy {

        @Override
        public String scopeNameFor(TestIdentifier testId) {
            var source = testId.getSource();
            if (source.isPresent()) {
                var testSource = source.get();
                if (testSource instanceof ClassSource classSource) {
                    return classSource.getJavaClass().getPackageName();
                }
            }
            return null;
        }
    }
}
