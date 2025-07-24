package io.micronaut.testresources.oracle.testpilot;

import io.micronaut.test.extensions.spock.annotation.MicronautTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class TestOracleTestPilotTestResourceProvider {

    private static final String TESTPILOT_CONNECTION_STRING_SUFFIX = "TESTPILOT_CONNECTION_STRING_SUFFIX";
    private static final String TESTPILOT_USERNAME = "TESTPILOT_USERNAME";
    private static final String TESTPILOT_PASSWORD = "TESTPILOT_PASSWORD";

    @BeforeAll
    static void setup() {
        try {
            // configure environment
            final Class<?> classOfMap = Class.forName("java.lang.ProcessEnvironment");
            final Field field = classOfMap.getDeclaredField("theCaseInsensitiveEnvironment");
            field.setAccessible(true);
            final Map<String, String> writeableEnvironmentVariables = (Map<String, String>) field.get(System.getenv());
            writeableEnvironmentVariables.put(TESTPILOT_CONNECTION_STRING_SUFFIX, "localhost:1521/freepdb1");
            writeableEnvironmentVariables.put(TESTPILOT_USERNAME, "loic");
            writeableEnvironmentVariables.put(TESTPILOT_PASSWORD, "password");
        }
        catch(Exception e) {
            Assertions.fail(e);
        }
    }

    @AfterAll
    static void cleanup() {
        try {
            // configure environment
            final Class<?> classOfMap = Class.forName("java.lang.ProcessEnvironment");
            final Field field = classOfMap.getDeclaredField("theCaseInsensitiveEnvironment");
            field.setAccessible(true);
            final Map<String, String> writeableEnvironmentVariables = (Map<String, String>) field.get(System.getenv());
            writeableEnvironmentVariables.remove(TESTPILOT_CONNECTION_STRING_SUFFIX);
            writeableEnvironmentVariables.remove(TESTPILOT_USERNAME);
            writeableEnvironmentVariables.remove(TESTPILOT_PASSWORD);
        }
        catch(Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    void ensureTestResourceWorking() {
        final OracleTestPilotTestResourceProvider provider = new OracleTestPilotTestResourceProvider();

        assertEquals( "jdbc:oracle:thin:@localhost:1521/freepdb1", provider.resolve("url",null,null).get() );
        assertEquals( "loic", provider.resolve("username",null,null).get() );
        assertEquals( "password", provider.resolve("password",null,null).get() );
    }
}
