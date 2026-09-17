package io.micronaut.testresources.docs.junitplatform;

// tag::imports[]
import io.micronaut.test.extensions.junit5.ScopeHolder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.extensions.junit5.annotation.TestResourcesScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
// end::imports[]

// tag::clazz[]
@MicronautTest
@TestResourcesScope("my scope") // <1>
class ScopeNameTest {

    @Test
    void testSomething() {
        assertEquals("my scope", ScopeHolder.get().orElse(null)); // <2>
    }
}
// end::clazz[]
