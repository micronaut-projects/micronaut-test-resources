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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the test resources scope to use in a test
 * class. By default, all tests execute in the same
 * (root) scope, which means that they share the same
 * containers, for example.
 * <p/>
 * In some cases, it may be needed to isolate a test
 * from others, by making sure it runs with its own
 * test resources (e.g containers).
 * <p/>
 * When multiple tests are using the same scope, then
 * the resource will be shared between those tests,
 * and disposed whenever the last test which needed
 * that resource has finished.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Inherited
public @interface TestResourcesScope {
    /**
     * The name of the test resources scope to use.
     * A test can only use a single scope at once.
     * Once the last test using this scope is executed, the
     * scope will be automatically closed. If not set,
     * you must provide a {@link #namingStrategy()}.
     *
     * @return the name of the scope
     */
    String value() default "";

    /**
     * The name of the strategy to use, instead of an explicit
     * name provided with {@link #value()}.
     * @return the naming strategy
     */
    Class<? extends ScopeNamingStrategy> namingStrategy() default ScopeNamingStrategy.class;
}
