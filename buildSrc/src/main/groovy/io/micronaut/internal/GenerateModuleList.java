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
package io.micronaut.internal;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;

@CacheableTask
public abstract class GenerateModuleList extends DefaultTask {
    @Input
    public abstract SetProperty<ComponentArtifactIdentifier> getDependencies();

    @Input
    public abstract Property<String> getFileName();

    @OutputDirectory
    public abstract RegularFileProperty getOutputDirectory();

    @TaskAction
    public void writeDependencyList() throws IOException {
        try (var writer = Files.newBufferedWriter(getOutputDirectory().get().getAsFile().toPath().resolve(getFileName().get()))) {
            var ids = getDependencies()
                .get()
                .stream()
                .map(ComponentArtifactIdentifier::getComponentIdentifier)
                .filter(ModuleComponentIdentifier.class::isInstance)
                .map(mi -> ((ModuleComponentIdentifier) mi).getModuleIdentifier().toString())
                .sorted()
                .toList();
            for (String mi : ids) {
                writer.write(mi);
                writer.newLine();
            }
        }
    }
}
