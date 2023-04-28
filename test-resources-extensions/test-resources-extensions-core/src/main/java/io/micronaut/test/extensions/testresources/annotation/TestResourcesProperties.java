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
package io.micronaut.test.extensions.testresources.annotation;

import io.micronaut.test.extensions.testresources.TestResourcesPropertyProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be applied to tests annotated with {@literal @MicronautTest}
 * in order to require some properties to be resolved by Micronaut Test Resources
 * before the application context is available.
 * <p/>
 * It is conceptually similar to a {@link io.micronaut.test.support.TestPropertyProvider}
 * except that it allows accessing the test resources properties declaratively
 * instead of having to use the test resources client directly.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
public @interface TestResourcesProperties {
    /**
     * Declares the list of properties which should be made available
     * to the test before the application context is started.
     * These properties will be queried by the test resources client
     * and exposed as properties to the test.
     *
     * @return the list of properties
     */
    String[] value() default {};

    /**
     * The optional list of test resource property providers which
     * can be applied to enhance the set of properties available to
     * a test.
     * @return the list of providers
     */
    Class<? extends TestResourcesPropertyProvider>[] providers() default {};
}
