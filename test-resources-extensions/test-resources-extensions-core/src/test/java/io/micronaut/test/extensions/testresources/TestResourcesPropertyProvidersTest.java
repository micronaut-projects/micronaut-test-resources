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
package io.micronaut.test.extensions.testresources;

import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.extensions.testresources.annotation.TestResourcesProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
@TestResourcesProperties(
    value = "some-property",
    providers = TestResourcesPropertyProvidersTest.MyProvider.class
)
public class TestResourcesPropertyProvidersTest {

    @Value("${some-other-property}")
    String derivedFromTestResources;

    @Test
    @DisplayName("properties can be computed from test resources")
    public void canDerivePropertiesFromTestResources() {
        assertEquals("supplied by test resources and transformed", derivedFromTestResources);
    }

    public static class MyProvider implements TestResourcesPropertyProvider {
        @Override
        public Map<String, String> provide(Map<String, Object> testProperties) {
            String str = (String) testProperties.get("some-property");
            return Map.of(
                "some-other-property", str + " and transformed"
            );
        }
    }

}
