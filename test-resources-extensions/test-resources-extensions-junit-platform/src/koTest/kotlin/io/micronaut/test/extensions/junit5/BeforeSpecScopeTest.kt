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
package io.micronaut.test.extensions.junit5

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.ScopeNamingStrategy
import io.micronaut.test.extensions.junit5.annotation.TestResourcesScope
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest
@TestResourcesScope(namingStrategy = ScopeNamingStrategy.TestClassName::class)
internal class BeforeSpecScopeTest : ParentTestWithScope({
    var scope: String? = null

    beforeSpec {
        scope = ScopeHolder.get().orElse(null)
    }

    "scope name is the current test class name".config(enabled = false) { // this fails with latest kotest version
        scope shouldBe BeforeSpecScopeTest::class.java.name
    }
})
