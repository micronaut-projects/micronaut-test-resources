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
package io.micronaut.testresources.oracle.free;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * An Oracle container which waits until it accepts JDBC connections.
 *
 * <p>{@link OracleContainer} replaces the JDBC container startup check with a
 * log-based wait strategy. The log message can be emitted shortly before the
 * database accepts authenticated connections, so verify readiness using the
 * JDBC container's retrying connection method.</p>
 *
 * <p>The JDBC check requires the Oracle JDBC driver on the test resources
 * server classpath. If it is missing, only the log-based wait strategy is used.</p>
 */
class JdbcReadyOracleContainer extends OracleContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcReadyOracleContainer.class);

    JdbcReadyOracleContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void waitUntilContainerStarted() {
        super.waitUntilContainerStarted();
        try (Connection connection = createConnection("");
             Statement statement = connection.createStatement()) {
            statement.execute(getTestQueryString());
        } catch (NoDriverFoundException e) {
            LOGGER.warn("Oracle JDBC driver {} is not available on the test resources server classpath, skipping the JDBC readiness check. "
                + "Add the driver to the test resources classpath (for example the testResourcesRuntimeOnly configuration in Gradle) to enable it.",
                getDriverClassName());
            LOGGER.debug("Unable to load the Oracle JDBC driver", e);
        } catch (SQLException e) {
            throw new ContainerLaunchException("Oracle did not become ready to accept JDBC connections", e);
        }
    }
}
