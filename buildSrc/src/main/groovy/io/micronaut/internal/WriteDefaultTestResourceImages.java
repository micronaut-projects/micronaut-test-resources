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
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CacheableTask
public abstract class WriteDefaultTestResourceImages extends DefaultTask {
    private static final Pattern FROM_PATTERN = Pattern.compile("^FROM\\s+(\\S+)\\s+AS\\s+([a-z0-9_]+)$");

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getManifestFile();

    @Input
    abstract Property<String> getPackage();

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory();

    @OutputFile
    abstract RegularFileProperty getPropertiesFile();

    @TaskAction
    public void writeDefaultTestResourceImages() throws IOException {
        Map<String, String> images = loadImages();
        writeJava(images);
        writeProperties(images);
    }

    private void writeJava(Map<String, String> images) throws IOException {
        File outputFile = getOutputDirectory().file(getPackage().map(pkg -> pkg.replace('.', '/') + "/DefaultTestResourceImages.java")).get().getAsFile();
        Path parentPath = outputFile.getParentFile().toPath();
        if (Files.isDirectory(parentPath) || Files.createDirectories(parentPath) != null) {
            try (PrintWriter prn = new PrintWriter(new FileWriter(outputFile))) {
                prn.println("package " + getPackage().get() + ";");
                prn.println();
                prn.println("import io.micronaut.core.annotation.Internal;");
                prn.println();
                prn.println("import java.util.Collections;");
                prn.println("import java.util.LinkedHashMap;");
                prn.println("import java.util.Map;");
                prn.println();
                prn.println("/**");
                prn.println(" * Default Docker images used by test resource providers.");
                prn.println(" */");
                prn.println("@Internal");
                prn.println("public final class DefaultTestResourceImages {");
                images.forEach((alias, image) ->
                    prn.println("    public static final String " + constantName(alias) + " = \"" + image + "\";")
                );
                prn.println();
                prn.println("    private static final Map<String, String> IMAGES = imagesMap();");
                prn.println();
                prn.println("    private DefaultTestResourceImages() {");
                prn.println("    }");
                prn.println();
                prn.println("    /**");
                prn.println("     * Returns the default image for the given manifest alias.");
                prn.println("     *");
                prn.println("     * @param alias The manifest alias");
                prn.println("     * @return The Docker image name");
                prn.println("     */");
                prn.println("    public static String image(String alias) {");
                prn.println("        String image = IMAGES.get(alias);");
                prn.println("        if (image == null) {");
                prn.println("            throw new IllegalArgumentException(\"No default Docker image is configured for '\" + alias + \"'\");");
                prn.println("        }");
                prn.println("        return image;");
                prn.println("    }");
                prn.println();
                prn.println("    /**");
                prn.println("     * Returns all configured default images keyed by manifest alias.");
                prn.println("     *");
                prn.println("     * @return The default images");
                prn.println("     */");
                prn.println("    public static Map<String, String> images() {");
                prn.println("        return IMAGES;");
                prn.println("    }");
                prn.println();
                prn.println("    private static Map<String, String> imagesMap() {");
                prn.println("        Map<String, String> images = new LinkedHashMap<>();");
                images.forEach((alias, image) ->
                    prn.println("        images.put(\"" + alias + "\", " + constantName(alias) + ");")
                );
                prn.println("        return Collections.unmodifiableMap(images);");
                prn.println("    }");
                prn.println("}");
            }
        }
    }

    private void writeProperties(Map<String, String> images) throws IOException {
        File outputFile = getPropertiesFile().get().getAsFile();
        Path parentPath = outputFile.getParentFile().toPath();
        if (Files.isDirectory(parentPath) || Files.createDirectories(parentPath) != null) {
            try (PrintWriter prn = new PrintWriter(new FileWriter(outputFile))) {
                images.forEach((alias, image) -> {
                    String propertyAlias = alias.replace('_', '-');
                    prn.println("default-image-" + propertyAlias + "=" + image);
                    prn.println("default-image-name-" + propertyAlias + "=" + imageName(image));
                    prn.println("default-image-tag-" + propertyAlias + "=" + imageTag(image));
                });
            }
        }
    }

    private Map<String, String> loadImages() throws IOException {
        Map<String, String> images = new LinkedHashMap<>();
        for (String line : Files.readAllLines(getManifestFile().get().getAsFile().toPath())) {
            Matcher matcher = FROM_PATTERN.matcher(line);
            if (matcher.matches()) {
                images.put(matcher.group(2), matcher.group(1));
            }
        }
        if (images.isEmpty()) {
            throw new IllegalStateException("Default Docker image manifest does not declare any images");
        }
        return images;
    }

    private static String constantName(String alias) {
        return "DEFAULT_" + alias.toUpperCase() + "_IMAGE";
    }

    private static String imageName(String image) {
        return image.substring(0, image.lastIndexOf(':'));
    }

    private static String imageTag(String image) {
        return image.substring(image.lastIndexOf(':') + 1);
    }
}
