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
import org.apache.inlong.sort.tests.utils.PlaceholderResolver;
import org.apache.inlong.sort.tests.utils.TestUtils;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * End-to-end tests for sort-connector-kafka uber jar.
 */
public class KafkaE2ECase extends FlinkContainerTestEnv {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaE2ECase.class);

    private static final Path kafkaJar = TestUtils.getResource("sort-connector-kafka.jar");
    private static final Path jdbcJar = TestUtils.getResource("sort-connector-jdbc.jar");
    private static final Path mysqlJar = TestUtils.getResource("sort-connector-mysql-cdc.jar");
    private static final Path mysqlJdbcJar = TestUtils.getResource("mysql-driver.jar");
    // Can't use getResource("xxx").getPath(), windows will don't know that path

    @ClassRule
    public static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("kafka")
                    .withEmbeddedZookeeper()
                    .withLogConsumer(new Slf4jLogConsumer(LOG));

    @AfterClass
    public static void teardown() {
        if (KAFKA != null) {
            KAFKA.stop();
        }
    }

    private Path getSql(String fileName, Map<String, Object> properties) {
        try {
            Path file = Paths.get(KafkaE2ECase.class.getResource("/flinkSql/" + fileName).toURI());
            return PlaceholderResolver.getDefaultResolver().resolveByMap(file, properties);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getGroupFile(String fileName, Map<String, Object> properties) {
        try {
            Path file = Paths.get(KafkaE2ECase.class.getResource("/groupFile/" + fileName).toURI());
            return PlaceholderResolver.getDefaultResolver().resolveByMap(file, properties);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCreateStatement(String fileName, Map<String, Object> properties) {
        try {
            Path file = Paths.get(KafkaE2ECase.class.getResource("/env/" + fileName).toURI());
            return PlaceholderResolver.getDefaultResolver().resolveByMap(
                    new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
                    properties);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeKafkaTable(String fileName, Map<String, Object> properties) {
        try {
            String createKafkaStatement = getCreateStatement(fileName, properties);
            ExecResult result = KAFKA.execInContainer("bash", "-c", createKafkaStatement);
            LOG.info("Create kafka topic: {}, std: {}", createKafkaStatement, result.getStdout());
            if (result.getExitCode() != 0) {
                throw new RuntimeException("Init kafka topic failed. Exit code:" + result.getExitCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeMysqlTable(String fileName, Map<String, Object> properties) {
        try (Connection conn =
                DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                Statement stat = conn.createStatement()) {
            String createMysqlStatement = getCreateStatement(fileName, properties);
            stat.execute(createMysqlStatement);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test flink sql mysql cdc to hive
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testKafkaWithSqlFile() throws Exception {
        final String topic = "test-topic";
        final String mysqlInputTable = "test_input";
        final String mysqlOutputTable = "test_output";
        initializeMysqlTable("kafka_test_mysql_init.txt", new HashMap() {

            {
                put("MYSQL_INPUT_TABLE", mysqlInputTable);
                put("MYSQL_OUTPUT_TABLE", mysqlOutputTable);
            }
        });
        initializeKafkaTable("kafka_test_kafka_init.txt", new HashMap() {

            {
                put("TOPIC", topic);
                put("ZOOKEEPER_PORT", KafkaContainer.ZOOKEEPER_PORT);
            }
        });
        String sqlFile = getSql("kafka_test.sql", new HashMap<>()).toString();
        submitSQLJob(sqlFile, kafkaJar, jdbcJar, mysqlJar, mysqlJdbcJar);
        waitUntilJobRunning(Duration.ofSeconds(30));

        // generate input
        try (Connection conn =
                DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                Statement stat = conn.createStatement()) {
            stat.execute(
                    "INSERT INTO test_input "
                            + "VALUES (1,'jacket','water resistent white wind breaker',0.2, null, null, null);");
            stat.execute(
                    "INSERT INTO test_input VALUES (2,'scooter','Big 2-wheel scooter ',5.18, null, null, null);");
        } catch (SQLException e) {
            LOG.error("Update table for CDC failed.", e);
            throw e;
        }

        // validate output
        JdbcProxy proxy =
                new JdbcProxy(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword(), MYSQL_DRIVER_CLASS);
        List<String> expectResult =
                Arrays.asList(
                        "1,jacket,water resistent white wind breaker,0.2,,,",
                        "2,scooter,Big 2-wheel scooter ,5.18,,,");
        proxy.checkResultWithTimeout(
                expectResult,
                "test_output",
                7,
                60000L);
    }

    @Test
    public void testKafkaWithGroupFile() throws Exception {
        final String topic = "test_topic_for_group_file";
        final String mysqlInputTable = "test_input_for_group_file";
        final String mysqlOutputTable = "test_output_for_group_file";
        initializeMysqlTable("kafka_test_mysql_init.txt", new HashMap() {

            {
                put("MYSQL_INPUT_TABLE", mysqlInputTable);
                put("MYSQL_OUTPUT_TABLE", mysqlOutputTable);
            }
        });
        initializeKafkaTable("kafka_test_kafka_init.txt", new HashMap() {

            {
                put("TOPIC", topic);
                put("ZOOKEEPER_PORT", KafkaContainer.ZOOKEEPER_PORT);
            }
        });
        String groupFile = getGroupFile("kafka_test.json", new HashMap() {

            {
                put("MYSQL_INPUT_TABLE", mysqlInputTable);
                put("MYSQL_OUTPUT_TABLE", mysqlOutputTable);
                put("TOPIC", topic);
                put("ZOOKEEPER_PORT", KafkaContainer.ZOOKEEPER_PORT);
            }
        }).toString();
        submitGroupFileJob(groupFile, kafkaJar, jdbcJar, mysqlJar, mysqlJdbcJar);
        waitUntilJobRunning(Duration.ofSeconds(30));

        // generate input
        try (Connection conn =
                DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                Statement stat = conn.createStatement()) {
            stat.execute(
                    "INSERT INTO test_input_for_group_file "
                            + "VALUES (1,'jacket','water resistent white wind breaker',0.2, null, null, null);");
            stat.execute(
                    "INSERT INTO test_input_for_group_file "
                            + "VALUES (2,'scooter','Big 2-wheel scooter ',5.18, null, null, null);");
        } catch (SQLException e) {
            LOG.error("Update table for CDC failed.", e);
            throw e;
        }

        // validate output
        JdbcProxy proxy =
                new JdbcProxy(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword(), MYSQL_DRIVER_CLASS);
        List<String> expectResult =
                Arrays.asList(
                        "1,jacket,water resistent white wind breaker,0.2,null,null,null",
                        "2,scooter,Big 2-wheel scooter ,5.18,null,null,null");
        proxy.checkResultWithTimeout(
                expectResult,
                mysqlOutputTable,
                7,
                60000L);
    }
}
