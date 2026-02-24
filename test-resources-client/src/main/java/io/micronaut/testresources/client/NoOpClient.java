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
package io.micronaut.testresources.client;

import io.micronaut.core.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class NoOpClient implements TestResourcesClient {
    static final TestResourcesClient INSTANCE = new NoOpClient();

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries,
                                                Map<String, Object> testResourcesConfig) {
        return Collections.emptyList();
    }

    @Override
    public Optional<String> resolve(String name, Map<String, Object> properties,
                                    Map<String, Object> testResourcesConfig) {
        return Optional.empty();
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return Collections.emptyList();
    }

    @Override
    public boolean closeAll() {
        return true;
    }

    @Override
    public boolean closeScope(@Nullable String id) {
        return true;
    }
}
