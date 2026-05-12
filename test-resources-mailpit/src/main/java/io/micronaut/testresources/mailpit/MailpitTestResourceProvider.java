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
package io.micronaut.testresources.mailpit;

import io.micronaut.testresources.core.DefaultTestResourceImages;
import io.micronaut.testresources.core.Scope;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import io.micronaut.testresources.testcontainers.TestContainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which will spawn a Mailpit test container.
 */
public class MailpitTestResourceProvider extends AbstractTestContainersProvider<MailpitTestResourceProvider.MailpitContainer> {

    public static final String JAVAMAIL_SMTP_HOST = "javamail.properties.mail.smtp.host";
    public static final String JAVAMAIL_SMTP_PORT = "javamail.properties.mail.smtp.port";
    public static final String JAVAMAIL_SMTP_AUTH = "javamail.properties.mail.smtp.auth";
    public static final String JAVAMAIL_SMTP_STARTTLS = "javamail.properties.mail.smtp.starttls.enable";
    public static final String MAILPIT_UI_URL = "mailpit.ui.url";
    public static final String MAILPIT_API_URL = "mailpit.api.url";
    public static final String DEFAULT_IMAGE = DefaultTestResourceImages.DEFAULT_MAILPIT_IMAGE;
    public static final String DISPLAY_NAME = "Mailpit";
    public static final String SIMPLE_NAME = "mailpit";

    private static final int DEFAULT_SMTP_PORT = 1025;
    private static final int DEFAULT_UI_PORT = 8025;
    private static final String SMTP_PORT_CONFIG = "containers.mailpit.smtp-port";
    private static final String UI_PORT_CONFIG = "containers.mailpit.ui-port";
    private static final List<String> SUPPORTED_PROPERTIES = List.of(
        JAVAMAIL_SMTP_HOST,
        JAVAMAIL_SMTP_PORT,
        JAVAMAIL_SMTP_AUTH,
        JAVAMAIL_SMTP_STARTTLS,
        MAILPIT_UI_URL,
        MAILPIT_API_URL
    );
    private static final Set<String> SUPPORTED_PROPERTY_SET = Set.copyOf(SUPPORTED_PROPERTIES);
    private static final ThreadLocal<Set<String>> RESOLVING_ENDPOINT_PROPERTIES = ThreadLocal.withInitial(HashSet::new);

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES;
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        if (JAVAMAIL_SMTP_HOST.equals(expression)) {
            return requiredEndpointProperty(expression, JAVAMAIL_SMTP_PORT);
        }
        if (JAVAMAIL_SMTP_PORT.equals(expression)) {
            return requiredEndpointProperty(expression, JAVAMAIL_SMTP_HOST);
        }
        if (SUPPORTED_PROPERTY_SET.contains(expression)) {
            clearEndpointResolution();
            return List.of(JAVAMAIL_SMTP_HOST, JAVAMAIL_SMTP_PORT);
        }
        clearEndpointResolution();
        return List.of();
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    @SuppressWarnings("java:S2095")
    protected MailpitContainer createContainer(DockerImageName imageName,
                                               Map<String, Object> requestedProperties,
                                               Map<String, Object> testResourcesConfig) {
        int smtpPort = configuredPort(testResourcesConfig, SMTP_PORT_CONFIG, DEFAULT_SMTP_PORT);
        int uiPort = configuredPort(testResourcesConfig, UI_PORT_CONFIG, DEFAULT_UI_PORT);
        return new MailpitContainer(imageName, smtpPort, uiPort)
            .withCommand("--smtp", "[::]:" + smtpPort, "--listen", "[::]:" + uiPort)
            .withExposedPorts(smtpPort, uiPort)
            .waitingFor(Wait.forHttp("/").forPort(uiPort));
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, MailpitContainer container) {
        clearEndpointResolution();
        return switch (propertyName) {
            case JAVAMAIL_SMTP_HOST -> Optional.of(container.getHost());
            case JAVAMAIL_SMTP_PORT -> Optional.of(String.valueOf(container.getMappedPort(container.smtpPort)));
            case JAVAMAIL_SMTP_AUTH, JAVAMAIL_SMTP_STARTTLS -> Optional.of("false");
            case MAILPIT_UI_URL -> Optional.of(httpUrl(container));
            case MAILPIT_API_URL -> Optional.of(httpUrl(container) + "/api/v1");
            default -> Optional.empty();
        };
    }

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected boolean shouldAnswer(String propertyName,
                                   Map<String, Object> requestedProperties,
                                   Map<String, Object> testResourcesConfig) {
        if (!SUPPORTED_PROPERTY_SET.contains(propertyName)) {
            clearEndpointResolution();
            return false;
        }
        boolean hasHost = requestedProperties.containsKey(JAVAMAIL_SMTP_HOST);
        boolean hasPort = requestedProperties.containsKey(JAVAMAIL_SMTP_PORT);
        if (!hasHost && !hasPort) {
            return true;
        }
        boolean hasMailpitContainer = hasMailpitContainerFor(requestedProperties, JAVAMAIL_SMTP_HOST) ||
            hasMailpitContainerFor(requestedProperties, JAVAMAIL_SMTP_PORT);
        if (!hasMailpitContainer) {
            clearEndpointResolution();
        }
        return hasMailpitContainer;
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return findExistingMailpitContainer(properties)
            .flatMap(container -> resolveProperty(propertyName, container));
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    private static String httpUrl(MailpitContainer container) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(container.uiPort);
    }

    private static List<String> requiredEndpointProperty(String expression, String oppositeExpression) {
        Set<String> resolving = RESOLVING_ENDPOINT_PROPERTIES.get();
        if (resolving.contains(oppositeExpression)) {
            clearEndpointResolution();
            return List.of();
        }
        resolving.add(expression);
        return List.of(oppositeExpression);
    }

    private static void clearEndpointResolution() {
        RESOLVING_ENDPOINT_PROPERTIES.remove();
    }

    private static Optional<MailpitContainer> findExistingMailpitContainer(Map<String, Object> properties) {
        return findExistingMailpitContainer(properties, JAVAMAIL_SMTP_HOST)
            .or(() -> findExistingMailpitContainer(properties, JAVAMAIL_SMTP_PORT));
    }

    private static Optional<MailpitContainer> findExistingMailpitContainer(Map<String, Object> properties, String propertyName) {
        return TestContainers.findByRequestedProperty(Scope.from(properties), propertyName)
            .stream()
            .filter(MailpitContainer.class::isInstance)
            .map(MailpitContainer.class::cast)
            .findFirst();
    }

    private static boolean hasMailpitContainerFor(Map<String, Object> properties, String propertyName) {
        return findExistingMailpitContainer(properties, propertyName).isPresent();
    }

    private static int configuredPort(Map<String, Object> testResourcesConfig, String key, int defaultValue) {
        Object value = testResourcesConfig.get(key);
        if (value == null) {
            return defaultValue;
        }
        int port;
        if (value instanceof Number number) {
            port = number.intValue();
        } else {
            try {
                port = Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid Mailpit port configured for '" + key + "': " + value, e);
            }
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid Mailpit port configured for '" + key + "': " + value);
        }
        return port;
    }

    /**
     * A Mailpit container with configured SMTP and UI container ports.
     */
    @SuppressWarnings("java:S2160")
    public static class MailpitContainer extends GenericContainer<MailpitContainer> {
        protected final int smtpPort;
        protected final int uiPort;

        MailpitContainer(DockerImageName dockerImageName, int smtpPort, int uiPort) {
            super(dockerImageName);
            this.smtpPort = smtpPort;
            this.uiPort = uiPort;
        }
    }
}
