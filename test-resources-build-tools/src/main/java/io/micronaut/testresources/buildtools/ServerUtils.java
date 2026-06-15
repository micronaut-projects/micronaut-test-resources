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

import org.jspecify.annotations.Nullable;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities used to manage the lifecycle of a server process from build tools.
 */
public class ServerUtils {
    public static final String PROPERTIES_FILE_NAME = "test-resources.properties";

    /**
     * Used only in tests to skip actual port checking.
     */
    protected static final String SERVER_TEST_PROPERTY = "test.resources.internal.server.started";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerUtils.class.getName());
    private static final int STARTUP_TIME_WAIT_MS = 200;
    private static final int MAX_READS = 10;

    private static final String SERVER_URI = "server.uri";
    private static final String SERVER_ACCESS_TOKEN_MICRONAUT_PROPERTY = "server.access-token";
    private static final String SERVER_ACCESS_TOKEN = "server.access.token";
    private static final String ACCESS_TOKEN_HEADER = "Access-Token";
    private static final String SERVER_CLIENT_READ_TIMEOUT = "server.client.read.timeout";
    private static final String CLIENT_PROJECT_PATH_URI = "micronaut.test.resources.project-path-uri";
    private static final String SERVER_IDLE_TIMEOUT_MINUTES = "server.idle.timeout.minutes";
    private static final String SERVER_ENTRY_POINT =
        "io.micronaut.testresources.server.TestResourcesService";
    private static final String REQUIREMENTS_ENTRIES_PATH = "/requirements/entries";
    private static final String TEST_RESOURCES_BINARY_MEDIA_TYPE = "application/x-test-resources+binary";
    private static final String MICRONAUT_SERVER_PORT = "micronaut.server.port";
    private static final String JMX_SYSTEM_PROPERTY = "com.sun.management.jmxremote";
    private static final String CDS_HASH = "cds.bin";
    private static final String CDS_FILE = "cds.jsa";
    private static final String CDS_CLASS_LST = "cds.classlist";
    private static final String CDS_LOGGING_OFF = "-Xlog:cds*=off";
    private static final String FLAT_JAR = "flat.jar";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final int SERVER_PROBE_TIMEOUT_MS = 1000;

    // See io.micronaut.testresources.testcontainers.DockerSupport.TIMEOUT
    private static final String DOCKER_CHECK_TIMEOUT_SECONDS_ENV =
        "TEST_RESOURCES_DOCKER_CHECK_TIMEOUT_SECONDS";
    private static final String DOCKER_CHECK_TIMEOUT_SECONDS_PROPERTY =
        "docker.check.timeout.seconds";

    /**
     * Writes the server settings in an output directory.
     *
     * @param destinationDirectory the destination directory
     * @param settings the settings
     * @throws IOException if an error occurs
     */
    public static void writeServerSettings(Path destinationDirectory, ServerSettings settings)
        throws IOException {
        Files.createDirectories(destinationDirectory);
        Path propertiesFile = destinationDirectory.resolve(PROPERTIES_FILE_NAME);
        try (PrintWriter prn = new PrintWriter(Files.newOutputStream(propertiesFile))) {
            prn.println(SERVER_URI + "=http\\://localhost\\:" + settings.getPort());
            settings.getAccessToken()
                .ifPresent(token -> prn.println(SERVER_ACCESS_TOKEN + "=" + token));
            settings.getClientTimeout()
                .ifPresent(timeout -> prn.println(SERVER_CLIENT_READ_TIMEOUT + "=" + timeout));
            inferProjectDirectory(destinationDirectory)
                .ifPresent(projectDirectory -> prn.println(CLIENT_PROJECT_PATH_URI + "=" + projectDirectory.toUri()));
        }
    }

    /**
     * Reads the server settings from an input directory.
     *
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
                        .orElse(null),
                    Optional.ofNullable(props.getProperty(SERVER_IDLE_TIMEOUT_MINUTES))
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
     *
     * @param port the port to check
     * @return true if the port is already bound
     */
    public static boolean isServerStarted(int port) {
        try {
            if (System.getProperty(SERVER_TEST_PROPERTY) != null) {
                return Boolean.getBoolean(SERVER_TEST_PROPERTY);
            }
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
     *
     * @param explicitPort the explicit port to connect to, if it exists.
     * @param portFilePath the path to the port file, where the port will be written.
     * @param serverSettingsDirectory the server settings directory, will be written.
     * @param accessToken the access token, if any
     * @param cdsDirectory the CDS directory. If not null, class data sharing will be enabled
     * @param serverClasspath the server classpath
     * @param clientTimeoutMs the client timeout
     * @param serverIdleTimeoutMinutes the server idle timeout
     * @param serverFactory the server factory, responsible for forking a process
     * @return the server settings once the server is started
     * @throws IOException if an error occurs
     */
    @SuppressWarnings("java:S3776")
    public static ServerSettings startOrConnectToExistingServer(@Nullable Integer explicitPort,
                                                                Path portFilePath,
                                                                Path serverSettingsDirectory,
                                                                @Nullable String accessToken,
                                                                @Nullable Path cdsDirectory,
                                                                Collection<File> serverClasspath,
                                                                @Nullable Integer clientTimeoutMs,
                                                                @Nullable Integer serverIdleTimeoutMinutes,
                                                                ServerFactory serverFactory)
        throws IOException {
        Optional<ServerSettings> maybeServerSettings = readServerSettings(serverSettingsDirectory);
        if (maybeServerSettings.isPresent()) {
            LOGGER.info("Server settings found in {}", serverSettingsDirectory);
        }
        if (explicitPort != null) {
            ServiceProbeResult savedServerProbe = null;
            if (maybeServerSettings.isPresent() && maybeServerSettings.get().getPort() == explicitPort) {
                ServerSettings serverSettings = maybeServerSettings.get();
                savedServerProbe = probeServer(serverSettings);
                if (savedServerProbe.isReusable()) {
                    return serverSettings;
                }
            }
            ServiceProbeResult explicitPortProbe =
                probeServer(new ServerSettings(explicitPort, accessToken, clientTimeoutMs, serverIdleTimeoutMinutes));
            if (explicitPortProbe.isReusable()) {
                ServerSettings settings =
                    new ServerSettings(explicitPort, accessToken, clientTimeoutMs, serverIdleTimeoutMinutes);
                writeServerSettings(serverSettingsDirectory, settings);
                return settings;
            }
            if (explicitPortProbe.isRunning()) {
                throw explicitPortReuseFailure(explicitPort, explicitPortProbe.getFailureReason());
            }
            if (savedServerProbe != null && savedServerProbe.isRunning()) {
                throw explicitPortReuseFailure(explicitPort, savedServerProbe.getFailureReason());
            }
        } else if (maybeServerSettings.isPresent()) {
            ServerSettings serverSettings = maybeServerSettings.get();
            ServiceProbeResult serverProbe = probeServer(serverSettings);
            if (serverProbe.isReusable()) {
                return serverSettings;
            }
            if (serverProbe.isRunning()) {
                LOGGER.warn("Ignoring stale test resources server settings in {} because {}",
                    serverSettingsDirectory, serverProbe.getFailureReason());
            }
        }
        if (Files.exists(portFilePath)) {
            Files.delete(portFilePath);
        }

        Files.createDirectories(portFilePath.getParent());
        startAndWait(serverFactory, explicitPort, serverIdleTimeoutMinutes, portFilePath,
            accessToken, serverClasspath, cdsDirectory);
        int port;
        if (explicitPort == null) {
            List<String> lines = Files.readAllLines(portFilePath);
            int attempts = 1;
            while (lines.isEmpty()) {
                if (attempts == MAX_READS) {
                    throw new IllegalStateException(
                        "Unable to read port file " + portFilePath + ": file is empty");
                }
                // It is still possible to see the file, but that its contents isn't flushed yet
                try {
                    serverFactory.waitFor(Duration.of(STARTUP_TIME_WAIT_MS, ChronoUnit.MILLIS));
                    lines = Files.readAllLines(portFilePath);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                attempts++;
            }
            port = Integer.parseInt(lines.get(0));
        } else {
            port = explicitPort;
        }
        ServerSettings settings =
            new ServerSettings(port, accessToken, clientTimeoutMs, serverIdleTimeoutMinutes);
        writeServerSettings(serverSettingsDirectory, settings);
        return settings;
    }

    private static IllegalStateException explicitPortReuseFailure(int explicitPort, @Nullable String failureReason) {
        return new IllegalStateException("Explicit test resources port " + explicitPort + " is already in use "
            + "by a service that could not be validated as Micronaut Test Resources: " + failureReason);
    }

    private static ServiceProbeResult probeServer(ServerSettings serverSettings) {
        int port = serverSettings.getPort();
        if (!isServerStarted(port)) {
            return ServiceProbeResult.notRunning();
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:" + port + REQUIREMENTS_ENTRIES_PATH);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(SERVER_PROBE_TIMEOUT_MS);
            conn.setReadTimeout(SERVER_PROBE_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", JSON_CONTENT_TYPE);
            String accessToken = serverSettings.getAccessToken().orElse(null);
            if (accessToken != null) {
                conn.setRequestProperty(ACCESS_TOKEN_HEADER, accessToken);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return ServiceProbeResult.runningButInvalid(accessToken != null
                    ? "the access token was rejected"
                    : "an access token is required");
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return ServiceProbeResult.runningButInvalid("it responded with HTTP " + responseCode);
            }
            if (!isJsonContentType(conn.getContentType())) {
                return ServiceProbeResult.runningButInvalid("it did not return JSON");
            }
            try (InputStream inputStream = conn.getInputStream()) {
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                if (!isJsonStringArray(body)) {
                    return ServiceProbeResult.runningButInvalid("it did not return the expected JSON array payload");
                }
            }
            return ServiceProbeResult.reusableServer();
        } catch (IOException e) {
            return ServiceProbeResult.runningButInvalid("probing failed with " + e.getClass().getSimpleName());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static boolean isJsonContentType(@Nullable String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).contains(JSON_CONTENT_TYPE);
    }

    private static boolean isJsonStringArray(String body) {
        int cursor = skipWhitespace(body, 0);
        if (cursor >= body.length() || body.charAt(cursor) != '[') {
            return false;
        }
        cursor = skipWhitespace(body, cursor + 1);
        if (cursor < body.length() && body.charAt(cursor) == ']') {
            return skipWhitespace(body, cursor + 1) == body.length();
        }
        while (cursor < body.length()) {
            cursor = consumeJsonString(body, cursor);
            if (cursor < 0) {
                return false;
            }
            cursor = skipWhitespace(body, cursor);
            if (cursor >= body.length()) {
                return false;
            }
            char next = body.charAt(cursor);
            if (next == ']') {
                return skipWhitespace(body, cursor + 1) == body.length();
            }
            if (next != ',') {
                return false;
            }
            cursor = skipWhitespace(body, cursor + 1);
        }
        return false;
    }

    private static int skipWhitespace(String body, int cursor) {
        while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static int consumeJsonString(String body, int cursor) {
        if (cursor >= body.length() || body.charAt(cursor) != '"') {
            return -1;
        }
        cursor++;
        while (cursor < body.length()) {
            char current = body.charAt(cursor);
            if (current == '"') {
                return cursor + 1;
            }
            if (current == '\\') {
                cursor++;
                if (cursor >= body.length()) {
                    return -1;
                }
            } else if (Character.isISOControl(current)) {
                return -1;
            }
            cursor++;
        }
        return -1;
    }

    /**
     * Starts a server at the given port, or connects to an existing server running
     * at the given port.
     *
     * @param explicitPort the explicit port to connect to, if it exists.
     * @param portFilePath the path to the port file, where the port will be written.
     * @param serverSettingsDirectory the server settings directory, will be written.
     * @param accessToken the access token, if any
     * @param serverClasspath the server classpath
     * @param clientTimeoutMs the client timeout
     * @param serverIdleTimeoutMinutes the server idle timeout
     * @param serverFactory the server factory, responsible for forking a process
     * @return the server settings once the server is started
     * @throws IOException if an error occurs
     */
    public static ServerSettings startOrConnectToExistingServer(@Nullable Integer explicitPort,
                                                                Path portFilePath,
                                                                Path serverSettingsDirectory,
                                                                @Nullable String accessToken,
                                                                Collection<File> serverClasspath,
                                                                @Nullable Integer clientTimeoutMs,
                                                                @Nullable Integer serverIdleTimeoutMinutes,
                                                                ServerFactory serverFactory)
        throws IOException {
        return startOrConnectToExistingServer(
            explicitPort,
            portFilePath,
            serverSettingsDirectory,
            accessToken,
            null,
            serverClasspath,
            clientTimeoutMs,
            serverIdleTimeoutMinutes,
            serverFactory
        );
    }

    /**
     * Stops a running server. The server will be contacted thanks
     * to the settings in the given directory.
     *
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
            conn.setRequestProperty("Content-Type", TEST_RESOURCES_BINARY_MEDIA_TYPE);
            conn.setRequestProperty("Accept", TEST_RESOURCES_BINARY_MEDIA_TYPE);
            conn.setFixedLengthStreamingMode(0);
            conn.setDoOutput(true);
            serverSettings.getAccessToken()
                .ifPresent(token -> conn.setRequestProperty(ACCESS_TOKEN_HEADER, token));
            try (var os = conn.getOutputStream()) {
                os.flush();
            }
            try (InputStream is = conn.getInputStream()) {
                is.read();
            }
            Files.delete(serverSettingsDirectory.resolve(PROPERTIES_FILE_NAME));
        }
    }

    /**
     * Returns the default path to the settings directory for the test
     * resources server in case it needs to be shared between builds.
     * Equivalent to calling {@link #getDefaultSharedSettingsPath(String)}
     * without a namespace.
     *
     * @return the default path to the settings directory
     */
    public static Path getDefaultSharedSettingsPath() {
        return getDefaultSharedSettingsPath(null);
    }

    /**
     * Returns the default path to the settings directory for the test
     * resources server in case it needs to be shared between builds.
     *
     * @param namespace the namespace of the shared settings
     * @return the default path to the settings directory
     */
    public static Path getDefaultSharedSettingsPath(@Nullable String namespace) {
        String ns = namespace == null ? "test-resources" : "test-resources-" + namespace;
        return Paths.get(System.getProperty("user.home"), ".micronaut/" + ns);
    }

    private static Optional<Path> inferProjectDirectory(Path settingsDirectory) {
        Path current = settingsDirectory.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle"))
                || Files.exists(current.resolve("settings.gradle.kts"))
                || Files.exists(current.resolve("gradlew"))
                || Files.exists(current.resolve("gradlew.bat"))) {
                return Optional.of(current);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    private static void startAndWait(ServerFactory serverFactory,
                                     @Nullable Integer explicitPort,
                                     @Nullable Integer idleTimeoutMinutes,
                                     Path portFilePath,
                                     @Nullable String accessToken,
                                     Collection<File> serverClasspath,
                                     @Nullable Path cdsDirectory) throws IOException {
        ProcessParameters processParameters =
            createProcessParameters(explicitPort, idleTimeoutMinutes, portFilePath, accessToken,
                serverClasspath, cdsDirectory);
        serverFactory.startServer(processParameters);
        // If the call is a CDS dump, we need to perform a second invocation
        // which doesn't dump
        if (processParameters.isCDSDumpInvocation()) {
            startAndWait(serverFactory, explicitPort, idleTimeoutMinutes, portFilePath, accessToken,
                serverClasspath, cdsDirectory);
            return;
        }
        waitForServerToBeAvailable(serverFactory, explicitPort, portFilePath);
    }

    @SuppressWarnings("java:S3776")
    private static void waitForServerToBeAvailable(ServerFactory serverFactory,
                                                   @Nullable Integer explicitPort,
                                                   Path portFilePath) {
        Integer port = explicitPort;
        if (explicitPort == null) {
            int retries = 12;
            long dur = STARTUP_TIME_WAIT_MS;
            while (--retries > 0 && !Files.exists(portFilePath)) {
                try {
                    serverFactory.waitFor(Duration.of(dur, ChronoUnit.MILLIS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                dur *= 2;
            }
            if (!Files.exists(portFilePath)) {
                throw new IllegalStateException("Port file not created. Server probably failed to start.");
            }
            try {
                port = Integer.parseInt(Files.readString(portFilePath));
            } catch (Exception ex) {
                return;
            }
        }
        if (port == null) {
            return;
        }
        int actualPort = port;
        // Make sure the service is started: in case we use an explicit port,
        // there can be some delay before the service is available
        int retries = 8;
        int waitMs = 25;
        while (--retries > 0 && !isServerStarted(actualPort)) {
            try {
                serverFactory.waitFor(Duration.of(waitMs, ChronoUnit.MILLIS));
                // exponential backoff
                waitMs *= 2;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // At this stage if the server is still not started we have a bigger
        // issue and it will be handled by the test resources client
    }

    private static ProcessParameters createProcessParameters(@Nullable Integer explicitPort,
                                                             @Nullable Integer serverIdleTimeoutMinutes,
                                                             Path portFilePath, @Nullable String accessToken,
                                                             Collection<File> serverClasspath,
                                                             @Nullable Path cdsDirectory) {
        return new DefaultProcessParameters(explicitPort, serverIdleTimeoutMinutes, accessToken,
            cdsDirectory, serverClasspath, portFilePath);

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

        /**
         * The JVM process arguments.
         *
         * @return the JVM process arguments.
         */
        List<String> getJvmArguments();

        default boolean isCDSDumpInvocation() {
            return false;
        }
    }

    private record ServiceProbeResult(boolean reusable, boolean running, @Nullable String failureReason) {
        private static ServiceProbeResult reusableServer() {
            return new ServiceProbeResult(true, true, null);
        }

        private static ServiceProbeResult notRunning() {
            return new ServiceProbeResult(false, false, "it is not running");
        }

        private static ServiceProbeResult runningButInvalid(String failureReason) {
            return new ServiceProbeResult(false, true, failureReason);
        }

        private boolean isReusable() {
            return reusable;
        }

        private boolean isRunning() {
            return running;
        }

        private @Nullable String getFailureReason() {
            return failureReason;
        }
    }

    private static final class DefaultProcessParameters implements ProcessParameters {
        private final @Nullable Integer explicitPort;
        private final @Nullable String accessToken;
        private final @Nullable Path cdsDirectory;
        private final Collection<File> serverClasspath;
        private final Path portFilePath;
        private final @Nullable Integer idleTimeoutMinutes;
        private @Nullable List<String> jvmArgs;
        private @Nullable List<File> classpath;

        DefaultProcessParameters(@Nullable Integer explicitPort,
                                        @Nullable Integer idleTimeoutMinutes,
                                        @Nullable String accessToken,
                                        @Nullable Path cdsDirectory,
                                        Collection<File> serverClasspath,
                                        Path portFilePath) {
            this.explicitPort = explicitPort;
            this.idleTimeoutMinutes = idleTimeoutMinutes;
            this.accessToken = accessToken;
            this.cdsDirectory = cdsDirectory;
            this.serverClasspath = serverClasspath;
            this.portFilePath = portFilePath;
        }

        @Override
        public String getMainClass() {
            return SERVER_ENTRY_POINT;
        }

        @Override
        public boolean isCDSDumpInvocation() {
            return getJvmArguments()
                .stream()
                .anyMatch(arg -> arg.contains("-Xshare:dump"));
        }

        @Override
        public Map<String, String> getSystemProperties() {
            Map<String, String> systemProperties = new HashMap<>();
            if (!isCDSDumpInvocation()) {
                systemProperties.put(JMX_SYSTEM_PROPERTY, null);
            }
            String dockerCheckTimeout = System.getProperty(DOCKER_CHECK_TIMEOUT_SECONDS_PROPERTY);
            if (dockerCheckTimeout == null) {
                dockerCheckTimeout = System.getenv(DOCKER_CHECK_TIMEOUT_SECONDS_ENV);
            }
            if (dockerCheckTimeout != null) {
                systemProperties.put(DOCKER_CHECK_TIMEOUT_SECONDS_PROPERTY, dockerCheckTimeout);
            }
            if (explicitPort != null) {
                systemProperties.put(MICRONAUT_SERVER_PORT, String.valueOf(explicitPort));
            }
            if (accessToken != null) {
                systemProperties.put(SERVER_ACCESS_TOKEN_MICRONAUT_PROPERTY, accessToken);
            }
            if (idleTimeoutMinutes != null) {
                systemProperties.put(SERVER_IDLE_TIMEOUT_MINUTES,
                    String.valueOf(idleTimeoutMinutes));
            }
            return systemProperties;
        }

        @Override
        public List<File> getClasspath() {
            if (classpath != null) {
                return classpath;
            }
            if (cdsDirectory != null && serverClasspath.stream().anyMatch(File::isDirectory)) {
                // CDS doesn't support directories, so we have to create an arbitrary jar
                File flatDirsJar = cdsDirectory.resolve(FLAT_JAR).toFile();
                buildFlatJar(flatDirsJar);
                classpath = Stream.concat(
                    Stream.of(flatDirsJar),
                    serverClasspath.stream().filter(File::isFile)
                ).collect(Collectors.toList());
            } else {
                classpath = Collections.unmodifiableList(new ArrayList<>(serverClasspath));
            }
            return classpath;
        }

        /**
         * AppCDS doesn't support directories, so if we find some on classpath, we
         * build a jar out of them. That jar must be updated if there's any change,
         * so we also build a hash of its contents.
         */
        private void buildFlatJar(File flatDirsJar) {
            byte[] hash = computeClasspathHash(
                serverClasspath.stream()
                    .filter(File::isDirectory)
                    .map(File::toPath)
                    .flatMap(path -> {
                        try (Stream<Path> files = Files.walk(path)) {
                            // Need to go with intermediate list in order to avoid illegal state
                            return files.map(Path::toFile).collect(Collectors.toList()).stream();
                        } catch (IOException e) {
                            throw new ClassDataSharingException(e);
                        }
                    })
            );
            File hashFile = new File(flatDirsJar.getParentFile(), flatDirsJar.getName() + ".bin");
            if (flatDirsJar.exists()) {
                try {
                    if (hashFile.exists() &&
                        Arrays.equals(Files.readAllBytes(hashFile.toPath()), hash)) {
                        return;
                    }
                } catch (IOException e) {
                    throw new ClassDataSharingException("Cannot read hash file", e);
                }
                deleteCdsFiles(flatDirsJar);
            }
            createFlatJarArchiveFile(flatDirsJar, hash, hashFile);
        }

        private void createFlatJarArchiveFile(File flatDirsJar, byte[] hash, File hashFile) {
            try (JarOutputStream jos = new JarOutputStream(
                Files.newOutputStream(flatDirsJar.toPath()))) {
                Files.write(hashFile.toPath(), hash);
                Set<String> addedEntries = new HashSet<>();
                for (File dir : serverClasspath) {
                    if (dir.isDirectory()) {
                        Path rootDir = dir.toPath();
                        compressDirectory(jos, addedEntries, rootDir);
                    }
                }
            } catch (IOException e) {
                throw new ClassDataSharingException(e);
            }
        }

        private void compressDirectory(JarOutputStream jos, Set<String> addedEntries, Path rootDir)
            throws IOException {
            try (Stream<Path> stream = Files.walk(rootDir)) {
                List<Path> allpaths = stream.collect(Collectors.toList());
                for (Path sourcePath : allpaths) {
                    if (!sourcePath.equals(rootDir)) {
                        String zipFsPath = rootDir.relativize(sourcePath).toString();
                        JarEntry ze = new JarEntry(zipFsPath);
                        if (Files.isRegularFile(sourcePath) && addedEntries.add(zipFsPath)) {
                            jos.putNextEntry(ze);
                            Files.copy(sourcePath, jos);
                        }
                    }
                }
            }
        }

        @Override
        public List<String> getArguments() {
            if (explicitPort == null) {
                return Collections.singletonList("--port-file=" + portFilePath.toAbsolutePath());
            }
            return Collections.emptyList();
        }

        @Override
        public List<String> getJvmArguments() {
            if (jvmArgs != null) {
                return jvmArgs;
            }
            List<String> jvmArguments = new ArrayList<>();
            jvmArguments.add("-XX:+TieredCompilation");
            jvmArguments.add("-XX:TieredStopAtLevel=1");
            if (cdsDirectory != null) {
                File cdsDir = cdsDirectory.toFile();
                boolean useCDS = cdsDir.isDirectory() || cdsDir.mkdirs();
                if (useCDS) {
                    File cdsFile = new File(cdsDir, CDS_FILE);
                    File cdsClassList = new File(cdsDir, CDS_CLASS_LST);
                    File cdsHashFile = new File(cdsDir, CDS_HASH);
                    configureCdsOptions(jvmArguments, cdsFile, cdsClassList, cdsHashFile);
                }
            }
            jvmArgs = Collections.unmodifiableList(jvmArguments);
            return jvmArgs;
        }

        private void configureCdsOptions(List<String> jvmArguments,
                                         File cdsFile,
                                         File cdsClassList,
                                         File cdsHashFile) {
            jvmArguments.add(CDS_LOGGING_OFF);
            if (cdsClassList.exists()) {
                try {
                    byte[] actualHash = computeClasspathHash(getClasspath().stream());
                    if (cdsHashFile.exists()) {
                        byte[] cdsHash = Files.readAllBytes(cdsHashFile.toPath());
                        if (!Arrays.equals(actualHash, cdsHash)) {
                            // Classpath changed, invalidate CDS cache
                            deleteCdsFiles(cdsFile, cdsClassList, cdsHashFile);
                        }
                    } else {
                        Files.write(cdsHashFile.toPath(), actualHash);
                    }
                } catch (IOException e) {
                    deleteCdsFiles(cdsFile, cdsClassList, cdsHashFile);
                }
            }
            if (cdsClassList.exists()) {
                if (!cdsFile.exists()) {
                    configureCdsDump(jvmArguments, cdsFile, cdsClassList);
                } else {
                    jvmArguments.add("-XX:SharedArchiveFile=" + cdsFile);
                }
            } else {
                configureExportCdsClassList(jvmArguments, cdsClassList);
            }
        }

        private static void deleteCdsFiles(File... cdsFiles) {
            for (File cdsFile : cdsFiles) {
                try {
                    Files.deleteIfExists(cdsFile.toPath());
                } catch (IOException e) {
                    throw new ClassDataSharingException(e);
                }
            }
        }

        private static void configureExportCdsClassList(List<String> jvmArguments,
                                                        File cdsClassList) {
            jvmArguments.add("-Xshare:off");
            jvmArguments.add("-XX:DumpLoadedClassList=" + cdsClassList);
        }

        private static void configureCdsDump(List<String> jvmArguments, File cdsFile,
                                             File cdsClassList) {
            try {
                Path cdsListPath = cdsClassList.toPath();
                List<String> fileContent =
                    new ArrayList<>(Files.readAllLines(cdsListPath, StandardCharsets.UTF_8));
                // Workaround for https://bugs.openjdk.org/browse/JDK-8290417
                fileContent.removeIf(content ->
                    content.contains("SingleThreadedBufferingProcessor") ||
                    content.contains("org/testcontainers") ||
                    content.contains("org/graalvm") ||
                    content.contains("io/netty/handler") ||
                    content.contains("jdk/proxy"));
                Files.write(cdsListPath, fileContent, StandardCharsets.UTF_8);
            } catch (IOException e) {
                // ignore
            }
            jvmArguments.add("-Xshare:dump");
            jvmArguments.add("-XX:SharedClassListFile=" + cdsClassList);
            jvmArguments.add("-XX:SharedArchiveFile=" + cdsFile);
        }

        private static byte[] computeClasspathHash(Stream<File> files) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA1");
                files.flatMap(fileOrDir -> {
                    try (Stream<Path> s = Files.walk(fileOrDir.toPath())) {
                        return s.map(p -> {
                            File file = p.toFile();
                            return file.getAbsolutePath() + ":" + file.length() + ":" +
                                   file.lastModified();
                        }).collect(Collectors.toList()).stream();
                    } catch (IOException e) {
                        return Stream.empty();
                    }
                }).forEachOrdered(line -> digest.update(line.getBytes(StandardCharsets.UTF_8)));
                return digest.digest();
            } catch (NoSuchAlgorithmException e) {
                return new byte[0];
            }
        }
    }
}
