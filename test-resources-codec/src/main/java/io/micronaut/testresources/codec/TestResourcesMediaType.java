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
package io.micronaut.testresources.codec;

import io.micronaut.http.MediaType;

/**
 * Provides constants used to declare the test resources binary
 * protocol media type.
 *
 * @since 2.0.0
 */
public final class TestResourcesMediaType {
    public static final String TEST_RESOURCES_BINARY = "application/x-test-resources+binary";
    public static final MediaType TEST_RESOURCES_BINARY_MEDIA_TYPE = MediaType.of(TEST_RESOURCES_BINARY);

    private TestResourcesMediaType() {

    }
}
