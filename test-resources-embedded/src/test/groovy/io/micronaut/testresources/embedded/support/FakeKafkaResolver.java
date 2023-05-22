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
package io.micronaut.testresources.embedded.support;

import io.micronaut.testresources.core.TestResourcesResolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeKafkaResolver implements TestResourcesResolver {

    public static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap-servers";
    public static final String KAFKA_TOPIC = "kafka.topic";
    public static final String KAFKA_TEST_PORT = "kafka.test-port";

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return Arrays.asList(KAFKA_BOOTSTRAP_SERVERS, KAFKA_TOPIC);
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        return Collections.singletonList(KAFKA_TEST_PORT);
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        if (KAFKA_BOOTSTRAP_SERVERS.equals(propertyName)) {
            return Optional.of("http://localhost:" + properties.get(KAFKA_TEST_PORT));
        }
        if (KAFKA_TOPIC.equals(propertyName)) {
            return Optional.of("This value should not be seen in tests");
        }
        return Optional.empty();
    }
}
