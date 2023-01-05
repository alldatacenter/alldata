/*
 * Copyright 2022 Ververica Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.cdc.connectors.postgres;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.realtime.utils.NetUtils;
import org.apache.commons.io.IOUtils;
import org.apache.flink.test.util.AbstractTestBase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;

/**
 * Basic class for testing PostgresSQL source, this contains a PostgreSQL container which enables
 * binlog.
 */
public abstract class PostgresTestBase extends AbstractTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresTestBase.class);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^(.*)--.*$");

    private static final DockerImageName PG_IMAGE =
            DockerImageName.parse("debezium/postgres:9.6").asCompatibleSubstituteFor("postgres");

    protected static final PostgreSQLContainer<?> POSTGERS_CONTAINER =
            new PostgreSQLContainer<>(PG_IMAGE)
                    .withDatabaseName("postgres")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withLogConsumer(new Slf4jLogConsumer(LOG));

    @BeforeClass
    public static void startContainers() {
        LOG.info("Starting containers...");
        Startables.deepStart(Stream.of(POSTGERS_CONTAINER)).join();
        LOG.info("Containers are started.");
    }

    protected Connection getJdbcConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGERS_CONTAINER.getJdbcUrl(),
                POSTGERS_CONTAINER.getUsername(),
                POSTGERS_CONTAINER.getPassword());
    }

    public static BasicDataSourceFactory createPgSourceFactory(TargetResName dataxName) {
        PostgreSQLContainer<?> postgersContainer = POSTGERS_CONTAINER;
        Descriptor pgDataSourceFactory = TIS.get().getDescriptor("PGDataSourceFactory");
        Assert.assertNotNull(pgDataSourceFactory);
        Descriptor.FormData formData = new Descriptor.FormData();
        formData.addProp("tabSchema", "public");
        formData.addProp("name", "postgreSql");
        formData.addProp("dbName", postgersContainer.getDatabaseName());
        // formData.addProp("nodeDesc", mySqlContainer.getHost());

        formData.addProp("nodeDesc", NetUtils.getHost());

        formData.addProp("password", postgersContainer.getPassword());
        formData.addProp("userName", postgersContainer.getUsername());
        formData.addProp("port", String.valueOf(postgersContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
        formData.addProp("encode", "utf8");
        formData.addProp("useCompression", "true");

        Descriptor.ParseDescribable<BasicDataSourceFactory> parseDescribable
                = pgDataSourceFactory.newInstance(dataxName.getName(), formData);
        Assert.assertNotNull(parseDescribable.getInstance());
        return parseDescribable.getInstance();
    }


    /**
     * Executes a JDBC statement using the default jdbc config without autocommitting the
     * connection.
     */
    protected void initializePostgresTable(String sqlFile) {
        final String ddlFile = String.format("ddl/%s.sql", sqlFile);
        final URL ddlTestFile = PostgresTestBase.class.getClassLoader().getResource(ddlFile);
        assertNotNull("Cannot locate " + ddlFile, ddlTestFile);
        try (Connection connection = getJdbcConnection();
             Statement statement = connection.createStatement()) {
            try (InputStream reader = ddlTestFile.openStream()) {


                final List<String> statements
                        = Arrays.stream(IOUtils.readLines(reader, TisUTF8.get()).stream().map(String::trim)
                        .filter(x -> !x.startsWith("--") && !x.isEmpty())
                        .map(
                                x -> {
                                    final Matcher m =
                                            COMMENT_PATTERN.matcher(x);
                                    return m.matches() ? m.group(1) : x;
                                })
                        .collect(Collectors.joining("\n"))
                        .split(";"))
                        .collect(Collectors.toList());
                for (String stmt : statements) {
                    statement.execute(stmt);
                }
            }

//            final List<String> statements =
//                    Arrays.stream(
//                            Files.readAllLines(Paths.get(ddlTestFile.toURI())).stream()
//                                    .map(String::trim)
//                                    .filter(x -> !x.startsWith("--") && !x.isEmpty())
//                                    .map(
//                                            x -> {
//                                                final Matcher m =
//                                                        COMMENT_PATTERN.matcher(x);
//                                                return m.matches() ? m.group(1) : x;
//                                            })
//                                    .collect(Collectors.joining("\n"))
//                                    .split(";"))
//                            .collect(Collectors.toList());
//            for (String stmt : statements) {
//                statement.execute(stmt);
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
