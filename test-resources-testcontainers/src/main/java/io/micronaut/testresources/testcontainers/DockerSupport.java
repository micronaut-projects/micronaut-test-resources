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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides utilities around Docker support.
 */
public final class DockerSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerSupport.class);
    private static final int TIMEOUT = Integer.getInteger("docker.check.timeout.seconds", 10);
    private static final Lock LOCK = new ReentrantLock();
    private static final AtomicReference<Boolean> AVAILABLE = new AtomicReference<>();

    private DockerSupport() {

    }

    private static boolean performDockerCheck() {
        var executor = Executors.newSingleThreadExecutor();
        boolean available;
        try {
            var future = executor.submit(() -> DockerClientFactory.instance().isDockerAvailable());
            available = future.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            available = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            available = false;
        } finally {
            executor.shutdown();
        }
        if (!available) {
            LOGGER.error("Docker support doesn't seem to be available, test resources will not work correctly. Please check your Docker install.");
        }
        return available;
    }

    /**
     * Returns true if the Docker client is available and ready to accept
     * connections. It runs the check in a separate thread in order to
     * avoid blocking the caller, and times out after 2s. If the client
     * didn't answer after 2 seconds, we consider it's not available.
     * @return true if Docker is available
     */
    public static boolean isDockerAvailable() {
        var available = AVAILABLE.get();
        if (available != null) {
            return available;
        }
        LOCK.lock();
        try {
            available = AVAILABLE.get();
            if (available != null) {
                return available;
            }
            available = performDockerCheck();
            AVAILABLE.set(available);
            return available;
        } finally {
            LOCK.unlock();
        }

    }
}
