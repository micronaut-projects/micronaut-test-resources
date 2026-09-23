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
package io.micronaut.testresources.oracle.free

import org.testcontainers.containers.wait.strategy.WaitStrategy
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

import java.sql.Connection
import java.sql.Statement

class JdbcReadyOracleContainerSpec extends Specification {

    def "waits for a successful JDBC query after the container wait strategy"() {
        given:
        def connection = Mock(Connection)
        def statement = Mock(Statement)
        def waitStrategy = Mock(WaitStrategy)
        def container = new TestOracleContainer(connection)
        container.setWaitStrategy(waitStrategy)

        when:
        container.awaitStarted()

        then:
        1 * waitStrategy.waitUntilReady(container)
        1 * connection.createStatement() >> statement
        1 * statement.execute("SELECT 1 FROM DUAL")
        1 * statement.close()
        1 * connection.close()
    }

    private static final class TestOracleContainer extends JdbcReadyOracleContainer {
        private final Connection connection

        private TestOracleContainer(Connection connection) {
            super(DockerImageName.parse("gvenzl/oracle-free:slim-faststart"))
            this.connection = connection
        }

        @Override
        Connection createConnection(String queryString) {
            connection
        }

        void awaitStarted() {
            waitUntilContainerStarted()
        }
    }
}
