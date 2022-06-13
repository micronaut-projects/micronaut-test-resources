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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@CacheableTask
public abstract class WriteModuleList extends DefaultTask {
    @Input
    abstract MapProperty<String, List<String>> getModules();

    @Input
    abstract MapProperty<String, String> getModuleDescriptions();

    @Input
    abstract Property<String> getPackage();

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void writeVersion() throws IOException {
        File outputFile = getOutputDirectory().file(getPackage().map(pkg -> pkg.replace('.', '/') + "/KnownModules.java")).get().getAsFile();
        Path parentPath = outputFile.getParentFile().toPath();
        if (Files.isDirectory(parentPath) || Files.createDirectories(parentPath) != null) {
            try (PrintWriter prn = new PrintWriter(new FileWriter(outputFile))) {
                prn.println("package " + getPackage().get() + ";\n");
                prn.println();
                prn.println("/**");
                prn.println(" * Micronaut Test Resources modules.");
                prn.println(" */");
                prn.println("public interface KnownModules {");
                Map<String, String> moduleDescriptions = getModuleDescriptions().get();
                getModules().get().entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        String key = e.getKey();
                        List<String> moduleList = e.getValue();
                        String description = moduleDescriptions.get(key);
                        if (description != null) {
                            prn.println("    // " + description);
                        }
                        moduleList.forEach(module -> {
                            String constant = module
                                .replace("test-resources-", "")
                                .replace('-', '_')
                                .toUpperCase();
                            prn.println("    String " + constant + " = \"" + module + "\";");
                        });
                        prn.println();
                    });
                prn.println("}");
            }
        }
    }
}
