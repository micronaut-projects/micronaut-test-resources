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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Abstraction over the Docker Compose V2 CLI.
 */
@Internal
interface ComposeCli {
    ComposeCommandResult run(ComposeConfiguration configuration, List<String> arguments);

    final class Default implements ComposeCli {
        private final List<String> commandPrefix;

        Default() {
            this(List.of("docker", "compose"));
        }

        Default(List<String> commandPrefix) {
            this.commandPrefix = List.copyOf(commandPrefix);
        }

        @Override
        public ComposeCommandResult run(ComposeConfiguration configuration, List<String> arguments) {
            List<String> command = new ArrayList<>(commandPrefix);
            for (Path file : configuration.files()) {
                command.add("-f");
                command.add(file.toString());
            }
            if (configuration.projectName() != null) {
                command.add("--project-name");
                command.add(configuration.projectName());
            }
            for (String profile : configuration.profiles()) {
                command.add("--profile");
                command.add(profile);
            }
            command.addAll(arguments);

            ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(configuration.workingDirectory().toFile())
                .redirectErrorStream(false);
            if (!configuration.profiles().isEmpty()) {
                processBuilder.environment().put("COMPOSE_PROFILES", String.join(",", configuration.profiles()));
            }
            return execute(processBuilder, configuration.startupTimeout());
        }

        private static ComposeCommandResult execute(ProcessBuilder processBuilder, Duration timeout) {
            try {
                Process process = processBuilder.start();
                CompletableFuture<String> output = readAsync(process.getInputStream());
                CompletableFuture<String> error = readAsync(process.getErrorStream());
                boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new ComposeCliException("Docker Compose command timed out");
                }
                return new ComposeCommandResult(process.exitValue(), join(output), join(error));
            } catch (IOException e) {
                throw new ComposeCliException("Docker Compose V2 is not available. Install Docker with the Compose V2 plugin to use Micronaut Test Resources Compose support.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ComposeCliException("Interrupted while running Docker Compose", e);
            }
        }

        private static CompletableFuture<String> readAsync(InputStream inputStream) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new ComposeCliException("Unable to read Docker Compose output", e);
                }
            });
        }

        private static String join(CompletableFuture<String> output) {
            try {
                return output.join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof ComposeCliException composeCliException) {
                    throw composeCliException;
                }
                throw e;
            }
        }
    }
}

@Internal
record ComposeCommandResult(int exitCode, String output, String error) {
    boolean successful() {
        return exitCode == 0;
    }

    String diagnostic() {
        if (error != null && !error.isBlank()) {
            return error.strip();
        }
        return output == null ? "" : output.strip();
    }
}

@Internal
final class ComposeCliException extends RuntimeException {
    ComposeCliException(String message) {
        super(message);
    }

    ComposeCliException(String message, Throwable cause) {
        super(message, cause);
    }
}
