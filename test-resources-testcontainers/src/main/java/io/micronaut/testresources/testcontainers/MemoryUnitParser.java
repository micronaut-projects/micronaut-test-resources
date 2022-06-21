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
package io.micronaut.testresources.testcontainers;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MemoryUnitParser {
    private static final Pattern MEMORY_UNIT_PATTERN = Pattern.compile("^(\\d+(?:[.]\\d+)?)\\s*([kmg])?(b?)$");

    private MemoryUnitParser() {
    }

    /**
     * Parses a memory unit string into a {@link Long} value (in bytes).
     * @param memory the memory string to parse
     * @return the parsed value
     */
    static Long parse(String memory) {
        Matcher m = MEMORY_UNIT_PATTERN.matcher(memory.trim().toLowerCase(Locale.ROOT));
        if (m.find()) {
            double value = Double.parseDouble(m.group(1));
            String unit = m.group(2);
            if ("k".equals(unit)) {
                return (long) (value * 1024);
            } else if ("m".equals(unit)) {
                return (long) (1024 * 1024 * value);
            } else if ("g".equals(unit)) {
                return (long) (1024 * 1024 * 1024 * value);
            } else {
                return (long) value;
            }
        }
        return null;
    }
}
