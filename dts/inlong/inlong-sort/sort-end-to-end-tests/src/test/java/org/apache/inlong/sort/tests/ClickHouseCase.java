/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.tests;

import org.apache.inlong.sort.tests.utils.FlinkContainerTestEnv;
import org.apache.inlong.sort.tests.utils.JdbcProxy;
import org.apache.inlong.sort.tests.utils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * End-to-end tests
 * Test flink sql mysql cdc to clickHouse
 */
public class ClickHouseCase extends FlinkContainerTestEnv {

    private static final Logger LOG = LoggerFactory.getLogger(ClickHouseCase.class);

    private static final Path jdbcJar = TestUtils.getResource("sort-connector-jdbc.jar");
    private static final Path mysqlJar = TestUtils.getResource("sort-connector-mysql-cdc.jar");
    private static final Path mysqlJdbcJar = TestUtils.getResource("mysql-driver.jar");
    // Can't use getResource("xxx").getPath(), windows will don't know that path
    private static final String sqlFile;

    static {
        try {
            sqlFile = Paths.get(ClickHouseCase.class.getResource("/flinkSql/clickhouse_test.sql").toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @ClassRule
    public static final ClickHouseContainer CLICK_HOUSE_CONTAINER = (ClickHouseContainer) new ClickHouseContainer(
            "yandex/clickhouse-server:20.1.8.41")
                    .withNetwork(NETWORK)
                    .withNetworkAliases("clickhouse")
                    .withLogConsumer(new Slf4jLogConsumer(LOG));

    @Before
    public void setup() {
        initializeMysqlTable();
        initializeClickHouseTable();
    }

    @After
    public void teardown() {
        if (CLICK_HOUSE_CONTAINER != null) {
            CLICK_HOUSE_CONTAINER.stop();
        }
    }

    private void initializeClickHouseTable() {
        try {
            Class.forName(CLICK_HOUSE_CONTAINER.getDriverClassName());
            Connection conn = DriverManager
                    .getConnection(CLICK_HOUSE_CONTAINER.getJdbcUrl(), CLICK_HOUSE_CONTAINER.getUsername(),
                            CLICK_HOUSE_CONTAINER.getPassword());
            Statement stat = conn.createStatement();
            stat.execute("create table test_output1 (\n"
                    + "       id Int32,\n"
                    + "       name Nullable(String),\n"
                    + "       description Nullable(String)\n"
                    + ")\n"
                    + "engine=MergeTree ORDER BY id;");
            stat.close();
            conn.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeMysqlTable() {
        try (Connection conn =
                DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                Statement stat = conn.createStatement()) {
            stat.execute(
                    "CREATE TABLE test_input1 (\n"
                            + "  id INTEGER primary key,\n"
                            + "  name VARCHAR(255) NOT NULL DEFAULT 'flink',\n"
                            + "  description VARCHAR(512)\n"
                            + ");");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test flink sql mysql cdc to clickHouse
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testClickHouseUpdateAndDelete() throws Exception {
        submitSQLJob(sqlFile, jdbcJar, mysqlJar, mysqlJdbcJar);
        waitUntilJobRunning(Duration.ofSeconds(30));

        // generate input
        try (Connection conn =
                DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                Statement stat = conn.createStatement()) {
            stat.execute(
                    "INSERT INTO test_input1 "
                            + "VALUES (1,'jacket','water resistent white wind breaker');");
            stat.execute(
                    "INSERT INTO test_input1 VALUES (2,'scooter','Big 2-wheel scooter ');");
            stat.execute(
                    "update test_input1 set name = 'tom' where id = 2;");
            stat.execute(
                    "delete from test_input1 where id = 1;");
        } catch (SQLException e) {
            LOG.error("Update table for CDC failed.", e);
            throw e;
        }

        JdbcProxy proxy =
                new JdbcProxy(CLICK_HOUSE_CONTAINER.getJdbcUrl(), CLICK_HOUSE_CONTAINER.getUsername(),
                        CLICK_HOUSE_CONTAINER.getPassword(),
                        CLICK_HOUSE_CONTAINER.getDriverClassName());
        List<String> expectResult =
                Arrays.asList("2,tom,Big 2-wheel scooter ");
        proxy.checkResultWithTimeout(
                expectResult,
                "test_output1",
                3,
                60000L);
    }

}
