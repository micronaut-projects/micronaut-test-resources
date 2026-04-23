/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.testresources.seaweedfs;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn a SeaweedFS test container.
 */
public class SeaweedFsTestResourceProvider extends AbstractTestContainersProvider<GenericContainer<?>> {

    public static final String SEAWEEDFS_URL = "seaweedfs.url";
    public static final String SEAWEEDFS_ACCESS_KEY = "seaweedfs.access-key";
    public static final String SEAWEEDFS_SECRET_KEY = "seaweedfs.secret-key";
    public static final String DEFAULT_IMAGE = "chrislusf/seaweedfs";
    public static final String DEFAULT_ACCESS_KEY = "some_access_key1";
    public static final String DEFAULT_SECRET_KEY = "some_secret_key1";
    public static final String DISPLAY_NAME = "SeaweedFS";
    public static final String SIMPLE_NAME = "seaweedfs";
    public static final int S3_PORT = 8333;
    private static final String S3_CONFIG_PATH = System.getProperty(
        "io.micronaut.testresources.seaweedfs.s3-config-path",
        "/etc/seaweedfs/s3.json"
    );
    private static final List<String> SUPPORTED_KEYS = List.of(
        SEAWEEDFS_URL,
        SEAWEEDFS_ACCESS_KEY,
        SEAWEEDFS_SECRET_KEY
    );

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_KEYS;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    protected GenericContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        GenericContainer<?> container = new GenericContainer<>(imageName);
        container.withCopyFileToContainer(seaweedFsS3ConfigFile(), S3_CONFIG_PATH);
        container.withCommand("server", "-s3", "-s3.config=" + S3_CONFIG_PATH);
        container.withExposedPorts(S3_PORT);
        return container;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, GenericContainer<?> container) {
        return switch (propertyName) {
            case SEAWEEDFS_URL -> Optional.of("http://" + container.getHost() + ":" + container.getMappedPort(S3_PORT));
            case SEAWEEDFS_ACCESS_KEY -> Optional.of(DEFAULT_ACCESS_KEY);
            case SEAWEEDFS_SECRET_KEY -> Optional.of(DEFAULT_SECRET_KEY);
            default -> Optional.empty();
        };
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_KEYS.contains(propertyName);
    }

    private static MountableFile seaweedFsS3ConfigFile() {
        try {
            Path tempFile = Files.createTempFile("seaweedfs-s3-", ".json");
            Files.writeString(tempFile, """
            {
              "identities": [
                {
                  "name": "test-user",
                  "credentials": [
                    {
                      "accessKey": "%s",
                      "secretKey": "%s"
                    }
                  ],
                  "actions": [
                    "Admin",
                    "Read",
                    "List",
                    "Tagging",
                    "Write"
                  ]
                }
              ]
            }
            """.formatted(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY), StandardCharsets.UTF_8);
            setReadablePermissions(tempFile);
            File file = tempFile.toFile();
            file.deleteOnExit();
            return MountableFile.forHostPath(tempFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create SeaweedFS S3 config", e);
        }
    }

    private static void setReadablePermissions(Path tempFile) throws IOException {
        try {
            Files.setPosixFilePermissions(tempFile, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ
            ));
        } catch (UnsupportedOperationException e) {
            // Fall back on platforms without POSIX file permissions.
            makeReadableByAllUsers(tempFile.toFile());
        }
    }

    private static void makeReadableByAllUsers(File file) {
        if (!file.setReadable(true, false)) {
            throw new IllegalStateException("Failed to make SeaweedFS S3 config readable");
        }
    }
}
