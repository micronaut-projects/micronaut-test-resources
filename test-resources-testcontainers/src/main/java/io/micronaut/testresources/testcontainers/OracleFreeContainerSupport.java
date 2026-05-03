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

import io.micronaut.core.annotation.Internal;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Provides shared Oracle Free container defaults.
 */
@Internal
public final class OracleFreeContainerSupport {
    private static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofMinutes(2);

    private OracleFreeContainerSupport() {
    }

    /**
     * Creates an Oracle Free container with the provider default startup timeout.
     *
     * @param containerSupplier the container supplier
     * @return the configured container
     * @param <T> the container type
     */
    public static <T extends GenericContainer<? extends T>> T createContainer(Supplier<T> containerSupplier) {
        return containerSupplier.get()
            .withStartupTimeout(DEFAULT_STARTUP_TIMEOUT);
    }
}
