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
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which will spawn a Mailpit test container.
 */
public class MailpitTestResourceProvider extends AbstractTestContainersProvider<MailpitTestResourceProvider.MailpitContainer> {

    public static final String JAVAMAIL_SMTP_PREFIX = "javamail.properties.mail.smtp";
    public static final String JAVAMAIL_SMTP_HOST = JAVAMAIL_SMTP_PREFIX + ".host";
    public static final String JAVAMAIL_SMTP_PORT = JAVAMAIL_SMTP_PREFIX + ".port";
    public static final String JAVAMAIL_SMTP_AUTH = JAVAMAIL_SMTP_PREFIX + ".auth";
    public static final String JAVAMAIL_SMTP_STARTTLS = JAVAMAIL_SMTP_PREFIX + ".starttls.enable";
    public static final String MAILPIT_PREFIX = "mailpit";
    public static final String MAILPIT_UI_URL = MAILPIT_PREFIX + ".ui.url";
    public static final String MAILPIT_API_URL = MAILPIT_PREFIX + ".api.url";
    public static final String DEFAULT_IMAGE = DefaultTestResourceImages.DEFAULT_MAILPIT_IMAGE;
    public static final String DISPLAY_NAME = "Mailpit";
    public static final String SIMPLE_NAME = "mailpit";

    private static final int DEFAULT_SMTP_PORT = 1025;
    private static final int DEFAULT_UI_PORT = 8025;
    private static final String SMTP_PORT_CONFIG = "containers.mailpit.smtp-port";
    private static final String UI_PORT_CONFIG = "containers.mailpit.ui-port";
    private static final String HOST_ENTRY = "host";
    private static final String PORT_ENTRY = "port";
    private static final String UI_ENTRY = "ui";
    private static final String API_ENTRY = "api";
    private static final List<String> SUPPORTED_PROPERTIES = List.of(
        JAVAMAIL_SMTP_HOST,
        JAVAMAIL_SMTP_PORT,
        JAVAMAIL_SMTP_AUTH,
        JAVAMAIL_SMTP_STARTTLS,
        MAILPIT_UI_URL,
        MAILPIT_API_URL
    );
    private static final Set<String> SUPPORTED_PROPERTY_SET = Set.copyOf(SUPPORTED_PROPERTIES);

    /**
     * {@inheritDoc}
     *
     * <p>This provider offers all of its properties or none of them: if the application
     * already configures an SMTP endpoint, nothing is offered, so no Mailpit container is
     * ever started and the JavaMail configuration is left alone. The decision belongs here
     * rather than in {@link #shouldAnswer} because this is the only hook which sees the
     * configuration before a value is asked for; deciding later would mean requiring the
     * endpoint properties through {@link #getRequiredProperties}, which this provider
     * resolves itself, and resolution would never terminate.</p>
     */
    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        if (hasExternalSmtpEndpoint(propertyEntries)) {
            return List.of();
        }
        return SUPPORTED_PROPERTIES;
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return List.of(JAVAMAIL_SMTP_PREFIX, MAILPIT_PREFIX);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This provider must not declare any required property. Required properties are
     * resolved through the very same property resolver which asked for the expression
     * (see {@code PropertyResolverSupport.resolveRequiredProperties}), so a provider which
     * requires a property it also resolves asks itself for that property, forever. The
     * SMTP host and port are the only context this provider could want, and both are
     * properties it resolves, so the list stays empty.</p>
     */
    @Override
    public List<String> getRequiredProperties(String expression) {
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
        return SUPPORTED_PROPERTY_SET.contains(propertyName);
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    private static String httpUrl(MailpitContainer container) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(container.uiPort);
    }

    /**
     * Determines whether the SMTP endpoint visible in the property entries belongs to the
     * application rather than to this provider.
     *
     * <p>The entries are read from the environment, which means that once the test resources
     * property source has been loaded they also contain the keys this provider itself offered,
     * and those are indistinguishable from user configuration by name alone. The {@code mailpit}
     * entries are the discriminator: no application configures them, so their presence means
     * the SMTP entries being looked at are this provider's own offer.</p>
     *
     * @param propertyEntries the property entries, keyed by the prefixes returned from
     * {@link #getRequiredPropertyEntries()}
     * @return whether the application configures its own SMTP endpoint
     */
    private static boolean hasExternalSmtpEndpoint(Map<String, Collection<String>> propertyEntries) {
        if (isOfferedByThisProvider(propertyEntries)) {
            return false;
        }
        Collection<String> smtpEntries = propertyEntries.get(JAVAMAIL_SMTP_PREFIX);
        if (smtpEntries == null) {
            return false;
        }
        return smtpEntries.contains(HOST_ENTRY) || smtpEntries.contains(PORT_ENTRY);
    }

    private static boolean isOfferedByThisProvider(Map<String, Collection<String>> propertyEntries) {
        Collection<String> mailpitEntries = propertyEntries.get(MAILPIT_PREFIX);
        return mailpitEntries != null && mailpitEntries.contains(UI_ENTRY) && mailpitEntries.contains(API_ENTRY);
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
