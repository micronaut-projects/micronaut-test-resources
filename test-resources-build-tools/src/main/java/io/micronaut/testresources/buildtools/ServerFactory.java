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
package io.micronaut.testresources.buildtools;

import java.io.IOException;
import java.time.Duration;

/**
 * Implemented by built tools, in order to fork the server process.
 */
public interface ServerFactory {
    /**
     * Starts a new server in a forked process.
     *
     * @param processParameters the process parameters
     * @throws IOException in case forking fails
     */
    void startServer(ServerUtils.ProcessParameters processParameters) throws IOException;

    /**
     * Waits for the server to start.
     *
     * @param duration the amount of time to wait
     * @throws InterruptedException in case the thread is interrupted
     */
    void waitFor(Duration duration) throws InterruptedException;
}
