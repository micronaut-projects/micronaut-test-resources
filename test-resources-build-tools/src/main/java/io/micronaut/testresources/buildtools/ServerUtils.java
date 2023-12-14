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
    private static final String SERVER_CLIENT_READ_TIMEOUT = "server.client.read.timeout";
    private static final String SERVER_IDLE_TIMEOUT_MINUTES = "server.idle.timeout.minutes";
    private static final String SERVER_ENTRY_POINT =
        "io.micronaut.testresources.server.TestResourcesService";
    private static final String MICRONAUT_SERVER_PORT = "micronaut.server.port";
    private static final String JMX_SYSTEM_PROPERTY = "com.sun.management.jmxremote";
    private static final String CDS_HASH = "cds.bin";
    private static final String CDS_FILE = "cds.jsa";
    private static final String CDS_CLASS_LST = "cds.classlist";
    private static final String FLAT_JAR = "flat.jar";

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
    public static ServerSettings startOrConnectToExistingServer(Integer explicitPort,
                                                                Path portFilePath,
                                                                Path serverSettingsDirectory,
                                                                String accessToken,
                                                                Path cdsDirectory,
                                                                Collection<File> serverClasspath,
                                                                Integer clientTimeoutMs,
                                                                Integer serverIdleTimeoutMinutes,
                                                                ServerFactory serverFactory)
        throws IOException {
        Optional<ServerSettings> maybeServerSettings = readServerSettings(serverSettingsDirectory);
        if (maybeServerSettings.isPresent()) {
            LOGGER.info("Server settings found in {}", serverSettingsDirectory);
            ServerSettings serverSettings = maybeServerSettings.get();
            if (explicitPort != null && isServerStarted(explicitPort)) {
                if (serverSettings.getPort() == explicitPort) {
                    return serverSettings;
                }
                throw new IllegalStateException("Server already started on port " + explicitPort +
                                                " but settings file says it should be on port " +
                                                serverSettings.getPort());
            }
            if (isServerStarted(serverSettings.getPort())) {
                return serverSettings;
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
    public static ServerSettings startOrConnectToExistingServer(Integer explicitPort,
                                                                Path portFilePath,
                                                                Path serverSettingsDirectory,
                                                                String accessToken,
                                                                Collection<File> serverClasspath,
                                                                Integer clientTimeoutMs,
                                                                Integer serverIdleTimeoutMinutes,
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
            serverSettings.getAccessToken()
                .ifPresent(token -> conn.setRequestProperty("Access-Token", token));
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
    public static Path getDefaultSharedSettingsPath(String namespace) {
        String ns = namespace == null ? "test-resources" : "test-resources-" + namespace;
        return Paths.get(System.getProperty("user.home"), ".micronaut/" + ns);
    }

    private static void startAndWait(ServerFactory serverFactory,
                                     Integer explicitPort,
                                     Integer idleTimeoutMinutes,
                                     Path portFilePath,
                                     String accessToken,
                                     Collection<File> serverClasspath,
                                     Path cdsDirectory) throws IOException {
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

    private static void waitForServerToBeAvailable(ServerFactory serverFactory, Integer explicitPort,
                                  Path portFilePath) {
        Integer port = explicitPort;
        if (explicitPort == null) {
            while (!Files.exists(portFilePath)) {
                try {
                    serverFactory.waitFor(Duration.of(STARTUP_TIME_WAIT_MS, ChronoUnit.MILLIS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            try {
                port = Integer.parseInt(Files.readString(portFilePath));
            } catch (Exception ex) {
                return;
            }
        }
        // Make sure the service is started: in case we use an explicit port,
        // there can be some delay before the service is available
        int retries = 8;
        int waitMs = 25;
        while (--retries > 0 && !isServerStarted(port)) {
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

    private static ProcessParameters createProcessParameters(Integer explicitPort,
                                                             Integer serverIdleTimeoutMinutes,
                                                             Path portFilePath, String accessToken,
                                                             Collection<File> serverClasspath,
                                                             Path cdsDirectory) {
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

    private static class DefaultProcessParameters implements ProcessParameters {
        private final Integer explicitPort;
        private final String accessToken;
        private final Path cdsDirectory;
        private final Collection<File> serverClasspath;
        private final Path portFilePath;
        private final Integer idleTimeoutMinutes;
        private List<String> jvmArgs;
        private List<File> classpath;
        private File flatDirsJar;

        public DefaultProcessParameters(Integer explicitPort,
                                        Integer idleTimeoutMinutes,
                                        String accessToken,
                                        Path cdsDirectory,
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
            systemProperties.put(JMX_SYSTEM_PROPERTY, null);
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
                flatDirsJar = cdsDirectory.resolve(FLAT_JAR).toFile();
                buildFlatJar();
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
        private void buildFlatJar() {
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
            createFlatJarArchiveFile(hash, hashFile);
        }

        private void createFlatJarArchiveFile(byte[] hash, File hashFile) {
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
