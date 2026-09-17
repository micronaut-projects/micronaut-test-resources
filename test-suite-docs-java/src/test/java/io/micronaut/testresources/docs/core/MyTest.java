package io.micronaut.testresources.docs.core;

// tag::imports[]
import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
// end::imports[]

// tag::clazz[]
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyTest implements TestPropertyProvider {

    @Value("${myapp.someProperty}")
    String someProperty;

    @Override
    public Map<String, String> getProperties() { // <1>
        return Map.of(
                "myapp.someProperty", "value"
        );
    }

    @Test
    void thePropertyIsAvailableToTheTest() {
        assertEquals("value", someProperty);
    }
}
// end::clazz[]
