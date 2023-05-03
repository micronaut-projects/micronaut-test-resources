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
package io.micronaut.test.extensions.junit5;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.extensions.junit5.annotation.TestResourcesScope;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Set;

import static io.micronaut.test.extensions.testresources.junit5.FakeTestResourcesClient.closedScopes;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestResourcesScope("hello")
public class ScopeWithinClassTest extends AbstractScopedTest {
    @Nested
    @TestResourcesScope("scope1")
    @Order(1)
    class UsesCustomScopes {
        @Test
        @Order(1)
        @DisplayName("Current scope is scope 1")
        public void test1() {
            assertEquals("scope1", currentScope());
            assertEquals(Set.of(), closedScopes());
        }
    }

    @Nested
    @TestResourcesScope("scope2")
    @Order(2)
    class ReusesScopes {
        @Test
        @Order(1)
        @DisplayName("Current scope is scope 2")
        public void test1() {
            assertEquals("scope2", currentScope());
            assertEquals(Set.of("scope1"), closedScopes());
        }

        @Test
        @Order(2)
        @DisplayName("Current scope is still scope 2")
        public void test2() {
            assertEquals("scope2", currentScope());
            assertEquals(Set.of("scope1"), closedScopes());
        }
    }

    @Nested
    @Order(3)
    class AllScopesMustBeClosed {

        @Test
        @Order(1)
        @DisplayName("Current scope is the scope of the top level class")
        public void test1() {
            assertEquals("hello", currentScope());
            assertEquals(Set.of("scope1", "scope2"), closedScopes());
        }
    }

    private static String currentScope() {
        return ScopeHolder.get().orElse(null);
    }
}
