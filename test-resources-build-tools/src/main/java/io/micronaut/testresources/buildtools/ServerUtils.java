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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Utilities used to manage the lifecycle of a server process from build tools.
 */
public class ServerUtils {
    public static final String PROPERTIES_FILE_NAME = "test-resources.properties";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerUtils.class.getName());
    private static final int STARTUP_TIME_WAIT_MS = 200;

    private static final String SERVER_URI = "server.uri";
    private static final String SERVER_ACCESS_TOKEN = "server.access.token";
    private static final String SERVER_CLIENT_READ_TIMEOUT = "server.client.read.timeout";
    private static final String SERVER_ENTRY_POINT = "io.micronaut.testresources.server.Application";
    private static final String MICRONAUT_SERVER_PORT = "micronaut.server.port";

    /**
     * Writes the server settings in an output directory.
     *
     * @param destinationDirectory the destination directory
     * @param settings the settings
     * @throws IOException if an error occurs
     */
    public static void writeServerSettings(Path destinationDirectory, ServerSettings settings) throws IOException {
        Files.createDirectories(destinationDirectory);
        Path propertiesFile = destinationDirectory.resolve(PROPERTIES_FILE_NAME);
        try (PrintWriter prn = new PrintWriter(Files.newOutputStream(propertiesFile))) {
            prn.println(SERVER_URI + "=http\\://localhost\\:" + settings.getPort());
            settings.getAccessToken().ifPresent(token -> prn.println(SERVER_ACCESS_TOKEN + "=" + token));
            settings.getClientTimeout().ifPresent(timeout -> prn.println(SERVER_CLIENT_READ_TIMEOUT + "=" + timeout));
        }
    }

    /**
     * Reads the server settings from an input directory.
     * @param settingsDirectory the settings directory
     * @return the server settings, if any
     */
    public static Optional<ServerSettings> readServerSettings(Path settingsDirectory) {
        Path propertiesFile = settingsDirectory.resolve(PROPERTIES_FILE_NAME);
        if (Files.exists(propertiesFile)) {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(propertiesFile)) {
                props.load(in);
                return Optional.of(new ServerSettings(
                    new URI(props.getProperty(SERVER_URI)).getPort(),
                    props.getProperty(SERVER_ACCESS_TOKEN),
                    Optional.ofNullable(props.getProperty(SERVER_CLIENT_READ_TIMEOUT))
                        .map(Integer::parseInt)
                        .orElse(null)
                ));
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException("Unable to read properties file", e);
            }
        }
        return Optional.empty();
    }

    /**
     * Determines if a server is already started at the given port.
     * @param port the port to check
     * @return true if the port is already bound
     */
    public static boolean isServerStarted(int port) {
        try {
            Socket socket = new Socket("localhost", port);
            socket.close();
            LOGGER.info("Test resources service already started on port {}", port);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Starts a server at the given port, or connects to an existing server running
     * at the given port.
     * @param explicitPort the explicit port to connect to, if it exists.
     * @param portFilePath the path to the port file, where the port will be written.
     * @param serverSettingsDirectory the server settings directory, will be written.
     * @param accessToken the access token, if any
     * @param serverClasspath the server classpath
     * @param clientTimeoutMs the client timeout
     * @param serverFactory the server factory, responsible for forking a process
     * @return the server settings once the server is started
     * @throws IOException if an error occurs
     */
    public static ServerSettings startOrConnectToExistingServer(Integer explicitPort,
                                                                Path portFilePath,
                                                                Path serverSettingsDirectory,
                                                                String accessToken,
                                                                Collection<File> serverClasspath,
                                                                Integer clientTimeoutMs,
                                                                ServerFactory serverFactory) throws IOException {
        Optional<ServerSettings> maybeServerSettings = readServerSettings(serverSettingsDirectory);
        if (maybeServerSettings.isPresent()) {
            LOGGER.info("Server settings found in {}", serverSettingsDirectory);
            ServerSettings serverSettings = maybeServerSettings.get();
            if (explicitPort != null && isServerStarted(explicitPort)) {
                if (serverSettings.getPort() == explicitPort) {
                    return serverSettings;
                }
                throw new IllegalStateException("Server already started on port " + explicitPort + " but settings file says it should be on port " + serverSettings.getPort());
            }
            if (isServerStarted(serverSettings.getPort())) {
                return serverSettings;
            }
        }
        if (Files.exists(portFilePath)) {
            Files.delete(portFilePath);
        }

        Files.createDirectories(portFilePath.getParent());
        startAndWait(serverFactory, explicitPort, portFilePath, accessToken, serverClasspath);
        int port;
        if (explicitPort == null) {
            port = Integer.parseInt(Files.readAllLines(portFilePath).get(0));
        } else {
            port = explicitPort;
        }
        ServerSettings settings = new ServerSettings(port, accessToken, clientTimeoutMs);
        writeServerSettings(serverSettingsDirectory, settings);
        return settings;
    }

    /**
     * Stops a running server. The server will be contacted thanks
     * to the settings in the given directory.
     * @param serverSettingsDirectory the settings directory
     * @throws IOException if an error occurs
     */
    public static void stopServer(Path serverSettingsDirectory) throws IOException {
        Optional<ServerSettings> maybeServerSettings = readServerSettings(serverSettingsDirectory);
        if (maybeServerSettings.isPresent()) {
            ServerSettings serverSettings = maybeServerSettings.get();
            URL url = new URL("http://localhost:" + serverSettings.getPort() + "/stop");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            serverSettings.getAccessToken().ifPresent(token -> conn.setRequestProperty("Access-Token", token));
            try (InputStream is = conn.getInputStream()) {
                is.read();
            }
            Files.delete(serverSettingsDirectory.resolve(PROPERTIES_FILE_NAME));
        }
    }

    /**
     * Returns the default path to the settings directory for the test
     * resources server in case it needs to be shared between builds.
     * @return the default path to the settings directory
     */
    public static Path getDefaultSharedSettingsPath() {
        return Paths.get(System.getProperty("user.home"), ".micronaut/test-resources");
    }

    private static void startAndWait(ServerFactory serverFactory,
                                     Integer explicitPort,
                                     Path portFilePath,
                                     String accessToken,
                                     Collection<File> serverClasspath) throws IOException {
        serverFactory.startServer(new ProcessParameters() {
            @Override
            public String getMainClass() {
                return SERVER_ENTRY_POINT;
            }

            @Override
            public Map<String, String> getSystemProperties() {
                Map<String, String> systemProperties = new HashMap<>();
                if (explicitPort != null) {
                    systemProperties.put(MICRONAUT_SERVER_PORT, String.valueOf(explicitPort));
                }
                if (accessToken != null) {
                    systemProperties.put(SERVER_ACCESS_TOKEN, accessToken);
                }
                return systemProperties;
            }

            @Override
            public List<File> getClasspath() {
                return Collections.unmodifiableList(new ArrayList<>(serverClasspath));
            }

            @Override
            public List<String> getArguments() {
                if (explicitPort == null) {
                    return Collections.singletonList("--port-file=" + portFilePath.toAbsolutePath());
                }
                return Collections.emptyList();
            }
        });
        while (!Files.exists(portFilePath)) {
            try {
                serverFactory.waitFor(Duration.of(STARTUP_TIME_WAIT_MS, ChronoUnit.MILLIS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Forking process parameters.
     */
    public interface ProcessParameters {
        /**
         * The main class name.
         *
         * @return the main class name.
         */
        String getMainClass();

        /**
         * The system properties.
         *
         * @return the system properties.
         */
        Map<String, String> getSystemProperties();

        /**
         * The classpath for the server.
         *
         * @return the classpath.
         */
        List<File> getClasspath();

        /**
         * The process arguments.
         *
         * @return the arguments.
         */
        List<String> getArguments();
    }
}
