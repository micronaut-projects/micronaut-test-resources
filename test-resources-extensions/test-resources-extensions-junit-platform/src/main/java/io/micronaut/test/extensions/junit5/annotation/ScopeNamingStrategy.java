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

/**
 * Provides the name of a test resources scope to
 * be used in a test class.
 */
@FunctionalInterface
public interface ScopeNamingStrategy {
    String scopeNameFor(Class<?> testClass);

    class TestClassName implements ScopeNamingStrategy {
        @Override
        public String scopeNameFor(Class<?> testClass) {
            return testClass.getName();
        }
    }

    class PackageName implements ScopeNamingStrategy {

        @Override
        public String scopeNameFor(Class<?> testClass) {
            return testClass.getPackageName();
        }
    }
}
