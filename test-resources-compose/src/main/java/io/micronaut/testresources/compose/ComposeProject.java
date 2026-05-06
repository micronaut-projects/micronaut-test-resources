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
package io.micronaut.testresources.compose;

import io.micronaut.core.annotation.Internal;

import java.nio.file.Path;
import java.util.List;

/**
 * Started or inspected Compose project state.
 *
 * @param key The project cache key.
 * @param services The discovered Compose services.
 * @param startedByTestResources Whether Test Resources started the project.
 * @param stopManaged Whether Test Resources should stop services it started.
 */
@Internal
record ComposeProject(ComposeProjectKey key, List<ComposeService> services, boolean startedByTestResources, boolean stopManaged) {
}

@Internal
record ComposeProjectKey(Path workingDirectory, List<Path> files, List<String> profiles, String projectName) {
    static ComposeProjectKey from(ComposeConfiguration configuration) {
        return new ComposeProjectKey(
            configuration.workingDirectory(),
            configuration.files(),
            configuration.profiles(),
            configuration.projectName()
        );
    }
}
