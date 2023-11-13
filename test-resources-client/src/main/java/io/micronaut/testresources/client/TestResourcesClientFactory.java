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
package io.micronaut.testresources.client;

import io.micronaut.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Properties;

/**
 * A factory responsible for creating a {@link TestResourcesClient}.
 * Because this client is used in services which are loaded via
 * service loading during application context building, we can't use
 * regular dependency injection, so this factory creates an application
 * context to create the client.
 */
public final class TestResourcesClientFactory {
    private static final String DEFAULT_TIMEOUT_SECONDS = "60";
    private static final String TEST_RESOURCES_PROPERTIES = "test-resources.properties";
    private static final String DEFAULT_MICRONAUT_DIR = ".micronaut/test-resources/";
    private static final String DEFAULT_PROPERTIES_RELATIVE_PATH = DEFAULT_MICRONAUT_DIR + TEST_RESOURCES_PROPERTIES;
    private static final String TEST_RESOURCES_NAMESPACE = "TEST_RESOURCES_NAMESPACE";
    private static final String NAMESPACE_PREFIX = ".micronaut/test-resources-";

    private static WeakReference<TestResourcesClient> cachedClient;

    private TestResourcesClientFactory() {

    }

    /**
     * Tries to configure a test resources client by looking for configuration
     * in conventional places. It will first look for the configuration as
     * system properties, which is the most reliable way for the client to
     * figure out how to connect to the server.
     * <p>
     * If not found, then it will try to read configuration
     * from the working directory, and then, from the user home.
     * It is recommended that consumers prefer passing the configuration via system
     * properties in order to support all possible options to configure the
     * client (in particular, shared mode or namespaces, which is not possible
     * to figure out via standard file system lookups).
     *
     * @return a configured client, or an empty optional.
     */
    public static Optional<TestResourcesClient> findByConvention() {
        return fromSystemProperties()
            .or(() -> fromFileSystem(Paths.get(DEFAULT_PROPERTIES_RELATIVE_PATH)))
            .or(() -> {
                var homedir = Paths.get(System.getProperty("user.home"));
                var namespace = System.getenv(TEST_RESOURCES_NAMESPACE);
                if (namespace != null) {
                    return fromFileSystem(homedir.resolve(
                        NAMESPACE_PREFIX + namespace + "/" + TEST_RESOURCES_PROPERTIES));
                }
                return fromFileSystem(homedir.resolve(DEFAULT_PROPERTIES_RELATIVE_PATH));
            });
    }

    /**
     * Tries to configure a test resources client from properties configuration file
     * found on a specific file system location.
     *
     * @param location the path to the configuration file
     * @return a configured client if found, otherwise an empty optional
     */
    public static Optional<TestResourcesClient> fromFileSystem(Path location) {
        if (Files.exists(location) && location.getFileName().endsWith(TEST_RESOURCES_PROPERTIES)) {
            Properties props = new Properties();
            try (InputStream input = Files.newInputStream(location, StandardOpenOption.READ)) {
                props.load(input);
            } catch (IOException e) {
                throw new TestResourcesException(e);
            }
            String serverUri = props.getProperty(TestResourcesClient.SERVER_URI);
            String accessToken = props.getProperty(TestResourcesClient.ACCESS_TOKEN);
            int clientReadTimeout = Integer.parseInt(props.getProperty(TestResourcesClient.CLIENT_READ_TIMEOUT, DEFAULT_TIMEOUT_SECONDS));
            return Optional.of(new DefaultTestResourcesClient(serverUri, accessToken, clientReadTimeout));
        }
        return Optional.empty();
    }

    /**
     * Creates a new test resources client configured via system properties.
     *
     * @return a new test resources client, if system properties were found.
     */
    public static Optional<TestResourcesClient> fromSystemProperties() {
        var client = cachedClient != null ? cachedClient.get() : null;
        if (client != null) {
            return Optional.of(client);
        }
        String serverUri = System.getProperty(ConfigFinder.systemPropertyNameOf(TestResourcesClient.SERVER_URI));
        if (serverUri != null) {
            String accessToken = System.getProperty(ConfigFinder.systemPropertyNameOf(TestResourcesClient.ACCESS_TOKEN));
            String clientTimeoutString = System.getProperty(ConfigFinder.systemPropertyNameOf(TestResourcesClient.CLIENT_READ_TIMEOUT), DEFAULT_TIMEOUT_SECONDS);
            int clientReadTimeout = Integer.parseInt(clientTimeoutString);
            client = new DefaultTestResourcesClient(serverUri, accessToken, clientReadTimeout);
            cachedClient = new WeakReference<>(client);
            return Optional.of(client);
        }
        return Optional.empty();
    }

    /**
     * Extracts the {@link TestResourcesClient} from the given {@link ApplicationContext}.
     *
     * @param context the application context
     * @return the test resources client
     */
    public static TestResourcesClient extractFrom(ApplicationContext context) {
        return context.getEnvironment()
            .getPropertySourceLoaders()
            .stream()
            .filter(TestResourcesClientPropertySourceLoader.class::isInstance)
            .map(TestResourcesClientPropertySourceLoader.class::cast)
            .map(TestResourcesClientPropertySourceLoader::getClient)
            .findFirst()
            .orElse(Optional.empty())
            .orElse(null);
    }
}
