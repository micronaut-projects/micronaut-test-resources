package io.micronaut.test.extensions.testresources;

import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.extensions.testresources.annotation.TestResourcesProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
@MetaAnnotationTestResourcesPropertiesTest.MyCustomTestResources
class MetaAnnotationTestResourcesPropertiesTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @TestResourcesProperties(
        value = "some-property",
        providers = MetaAnnotationTestResourcesPropertiesTest.MyCustomProvider.class)
    @interface MyCustomTestResources {}

    static class MyCustomProvider implements TestResourcesPropertyProvider {
        @Override
        public Map<String, String> provide(Map<String, Object> testProperties) {
            String str = (String) testProperties.get("some-property");
            return Map.of(
                "some-custom-property", str + " and transformed from custom meta annotation"
            );
        }
    }

    @Value("${some-custom-property}")
    String derivedFromTestResources;

    @Test
    @DisplayName("properties can be computed from test resources meta annotations")
    void canDerivePropertiesFromTestResourcesMetaAnnotations() {
        assertEquals("supplied by test resources and transformed from custom meta annotation", derivedFromTestResources);
    }

}
