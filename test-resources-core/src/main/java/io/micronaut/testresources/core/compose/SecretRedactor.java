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
package io.micronaut.testresources.core.compose;

import io.micronaut.core.annotation.Internal;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redacts sensitive diagnostic values.
 */
@Internal
final class SecretRedactor {
    private static final String REDACTED = "******";

    private SecretRedactor() {
    }

    static Map<String, String> redact(Map<String, String> values) {
        return values.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> isSensitive(e.getKey()) ? REDACTED : e.getValue()
            ));
    }

    static String redact(String name, String value) {
        return isSensitive(name) ? REDACTED : value;
    }

    static boolean isSensitive(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("password")
            || normalized.contains("passwd")
            || normalized.contains("pwd")
            || normalized.contains("secret")
            || normalized.contains("token")
            || normalized.contains("api-key")
            || normalized.contains("apikey")
            || normalized.endsWith(".key")
            || normalized.endsWith("_key")
            || normalized.equals("key")
            || normalized.contains("credential");
    }
}
