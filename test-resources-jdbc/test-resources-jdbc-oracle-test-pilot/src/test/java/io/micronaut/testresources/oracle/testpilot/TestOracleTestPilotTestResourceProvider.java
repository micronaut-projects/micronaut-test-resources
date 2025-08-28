package io.micronaut.testresources.oracle.testpilot;

import io.micronaut.test.extensions.spock.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class TestOracleTestPilotTestResourceProvider {

    private static final String TESTPILOT_CONNECTION_STRING_SUFFIX = "TESTPILOT_CONNECTION_STRING_SUFFIX";
    private static final String TESTPILOT_USERNAME = "TESTPILOT_USERNAME";
    private static final String TESTPILOT_PASSWORD = "TESTPILOT_PASSWORD";

    @Test
    void ensureTestResourceWorking() {
        final OracleTestPilotTestResourceProvider provider = new OracleTestPilotTestResourceProvider(this::getenv);

        assertEquals("jdbc:oracle:thin:@localhost:1521/freepdb1", provider.resolve("url", null, null).get());
        assertEquals("loic", provider.resolve("username", null, null).get());
        assertEquals("password", provider.resolve("password", null, null).get());
    }

    private String getenv(final String environmentVariable) {
        return switch (environmentVariable) {
            case TESTPILOT_CONNECTION_STRING_SUFFIX -> "\"localhost:1521/freepdb1\"";
            case TESTPILOT_USERNAME -> "loic";
            case TESTPILOT_PASSWORD -> "password";
            default -> null;
        };
    }
}
