/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.util;

import io.debezium.connector.mongodb.MongoDbConnectorConfig;
import io.debezium.testing.testcontainers.Connector;
import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.debezium.testing.testcontainers.ConnectorResolver;
import io.debezium.testing.testcontainers.DebeziumContainer;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Infrastructure {

    private static final String KAFKA_HOSTNAME = "kafka-dbz-ui";

    public enum DATABASE {
        POSTGRES, MYSQL, SQLSERVER, MONGODB, ORACLE, NONE
    }

    private static final String DEBEZIUM_CONTAINER_VERSION = "1.8";
    private static final Logger LOGGER = LoggerFactory.getLogger(Infrastructure.class);

    private static final Network NETWORK = Network.newNetwork();

    private static final KafkaContainer KAFKA_CONTAINER =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.5"))
                    .withNetworkAliases(KAFKA_HOSTNAME)
                    .withNetwork(NETWORK);

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("debezium/example-postgres:" + DEBEZIUM_CONTAINER_VERSION).asCompatibleSubstituteFor("postgres"))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("postgres");

    private static final MySQLContainer<?> MYSQL_CONTAINER =
            new MySQLContainer<>(DockerImageName.parse("debezium/example-mysql:" + DEBEZIUM_CONTAINER_VERSION).asCompatibleSubstituteFor("mysql"))
                    .withNetwork(NETWORK)
                    .withUsername("mysqluser")
                    .withPassword("mysqlpw")
                    .withEnv("MYSQL_ROOT_PASSWORD", "debezium")
                    .withNetworkAliases("mysql");

    private static final MongoDBContainer MONGODB_CONTAINER =
            new MongoDbContainer(DockerImageName.parse("mongo:3.6"))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("mongodb");

    private static final MSSQLServerContainer<?> SQL_SERVER_CONTAINER =
            new MSSQLServerContainer<>(DockerImageName.parse("mcr.microsoft.com/mssql/server:2019-latest"))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("sqlserver")
                    .withEnv("SA_PASSWORD", "Password!")
                    .withEnv("MSSQL_PID", "Standard")
                    .withEnv("MSSQL_AGENT_ENABLED", "true")
                    .withPassword("Password!")
                    .withStartupCheckStrategy(new MinimumDurationRunningStartupCheckStrategy(Duration.ofSeconds(5)))
                    .withInitScript("/initialize-sqlserver-database.sql")
                    .acceptLicense();

    private static final DebeziumContainer DEBEZIUM_CONTAINER;
    static {
        DEBEZIUM_CONTAINER = new DebeziumContainer("debezium/connect:nightly")
                .withEnv("ENABLE_DEBEZIUM_SCRIPTING", "true")
                .withEnv("CONNECT_REST_EXTENSION_CLASSES", "io.debezium.kcrestextension.DebeziumConnectRestExtension")
                .withNetwork(NETWORK)
                .withKafka(KAFKA_CONTAINER.getNetwork(), KAFKA_HOSTNAME + ":9092")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .dependsOn(KAFKA_CONTAINER);
    }

    public static Network getNetwork() {
        return NETWORK;
    }

    private static Supplier<Stream<GenericContainer<?>>> getContainers(DATABASE database) {
        final GenericContainer<?> dbContainer;
        switch (database) {
            case POSTGRES:
                dbContainer = POSTGRES_CONTAINER;
                break;
            case MYSQL:
                dbContainer = MYSQL_CONTAINER;
                break;
            case MONGODB:
                dbContainer = MONGODB_CONTAINER;
                break;
            case SQLSERVER:
                dbContainer = SQL_SERVER_CONTAINER;
                break;
            case NONE:
            default:
                dbContainer = null;
                break;
        }

        final Supplier<Stream<GenericContainer<?>>> containers;
        if (null != dbContainer) {
            containers = () -> Stream.of(KAFKA_CONTAINER, dbContainer, DEBEZIUM_CONTAINER);
        }
        else {
            containers = () -> Stream.of(KAFKA_CONTAINER, DEBEZIUM_CONTAINER);
        }
        return containers;
    }

    public static void stopContainers(DATABASE database) {
        getContainers(database).get().forEach(GenericContainer::stop);
    }

    public static void startContainers(DATABASE database) {
        final Supplier<Stream<GenericContainer<?>>> containers = getContainers(database);

        if ("true".equals(System.getenv("CI"))) {
            containers.get().forEach(container -> container.withStartupTimeout(Duration.ofSeconds(90)));
        }
        Startables.deepStart(containers.get()).join();
    }

    public static KafkaContainer getKafkaContainer() {
        return KAFKA_CONTAINER;
    }

    public static DebeziumContainer getDebeziumContainer() {
        return DEBEZIUM_CONTAINER;
    }

    public static PostgreSQLContainer<?> getPostgresContainer() {
        return POSTGRES_CONTAINER;
    }

    public static MySQLContainer<?> getMySqlContainer() {
        return MYSQL_CONTAINER;
    }

    public static MongoDBContainer getMongoDbContainer() {
        return MONGODB_CONTAINER;
    }

    public static MSSQLServerContainer<?> getSqlServerContainer() {
        return SQL_SERVER_CONTAINER;
    }

    public static ConnectorConfiguration getPostgresConnectorConfiguration(int id, String... options) {
        final ConnectorConfiguration config = ConnectorConfiguration.forJdbcContainer(POSTGRES_CONTAINER)
                .with("snapshot.mode", "never") // temporarily disable snapshot mode globally until we can check if connectors inside testcontainers are in SNAPSHOT or STREAMING mode (wait for snapshot finished!)
                .with("topic.prefix", "dbserver" + id)
                .with("slot.name", "debezium_" + id);

        if (options != null && options.length > 0) {
            for (int i = 0; i < options.length; i += 2) {
                config.with(options[i], options[i + 1]);
            }
        }
        return config;
    }

    public static ConnectorConfiguration getMySqlConnectorConfiguration(int id, String... options) {
        final ConnectorConfiguration config = ConnectorConfiguration.forJdbcContainer(MYSQL_CONTAINER)
                .with("database.user", "debezium")
                .with("database.password", "dbz")
                .with("snapshot.mode", "never") // temporarily disable snapshot mode globally until we can check if connectors inside testcontainers are in SNAPSHOT or STREAMING mode (wait for snapshot finished!)
                .with("topic.prefix", "dbserver" + id)
                .with("schema.history.internal.kafka.bootstrap.servers", KAFKA_HOSTNAME + ":9092")
                .with("schema.history.internal.kafka.topic", "dbhistory.inventory")
                .with("database.server.id", Long.valueOf(5555 + id - 1));

        if (options != null && options.length > 0) {
            for (int i = 0; i < options.length; i += 2) {
                config.with(options[i], options[i + 1]);
            }
        }
        return config;
    }

    public static ConnectorConfiguration getMongoDbConnectorConfiguration(int id, String... options) {
        final ConnectorConfiguration config = ConnectorConfiguration.forMongoDbContainer(MONGODB_CONTAINER)
                .with("snapshot.mode", "never") // temporarily disable snapshot mode globally until we can check if connectors inside testcontainers are in SNAPSHOT or STREAMING mode (wait for snapshot finished!)
                .with(MongoDbConnectorConfig.USER.name(), "debezium")
                .with(MongoDbConnectorConfig.PASSWORD.name(), "dbz")
                .with(MongoDbConnectorConfig.TOPIC_PREFIX.name(), "mongo" + id);

        if (options != null && options.length > 0) {
            for (int i = 0; i < options.length; i += 2) {
                config.with(options[i], options[i + 1]);
            }
        }
        return config;
    }

    public static void waitForConnectorTaskStatus(String connectorName, int taskNumber, Connector.State state) {
        Awaitility.await()
                // this needs to be set to at least a minimum of ~65-70 seconds because PostgreSQL now
                // retries on certain failure conditions with a 10s between them.
                .atMost(120, TimeUnit.SECONDS)
                // this is necessary until upgrading to Debezium 2.0, see DBZ-5159 changes in main repo
                .ignoreException(NullPointerException.class)
                .until(() -> Infrastructure.getDebeziumContainer().getConnectorTaskState(connectorName, taskNumber) == state);
    }

    public static ConnectorConfiguration getSqlServerConnectorConfiguration(int id, String... options) {
        final ConnectorConfiguration config = forJdbcContainerWithNoDatabaseNameSupport(SQL_SERVER_CONTAINER)
                .with("database.user", "sa")
                .with("database.password", "Password!")
                .with("schema.history.internal.kafka.bootstrap.servers", KAFKA_HOSTNAME + ":9092")
                .with("schema.history.internal.kafka.topic", "dbhistory.inventory")
                .with("snapshot.mode", "initial")
                .with("topic.prefix", "dbserver" + id)
                .with("database.encrypt", false);

        if (options != null && options.length > 0) {
            for (int i = 0; i < options.length; i += 2) {
                config.with(options[i], options[i + 1]);
            }
        }
        return config;
    }

    private static final String HOSTNAME = "database.hostname";
    private static final String PORT = "database.port";
    private static final String USER = "database.user";
    private static final String PASSWORD = "database.password";
    private static final String CONNECTOR = "connector.class";

    /**
     * Creates a {@link ConnectorConfiguration} object for databases where test containers does not support
     * the call to {@link JdbcDatabaseContainer#getDatabaseName()}.
     *
     * todo: this can be replaced with debezium-testing-testcontainers:2.0.0
     *
     * @param jdbcDatabaseContainer the container
     * @return the connector configuration
     */
    private static ConnectorConfiguration forJdbcContainerWithNoDatabaseNameSupport(JdbcDatabaseContainer<?> jdbcDatabaseContainer) {
        final ConnectorConfiguration configuration = ConnectorConfiguration.create();

        configuration.with(HOSTNAME, jdbcDatabaseContainer.getContainerInfo().getConfig().getHostName());

        final List<Integer> exposedPorts = jdbcDatabaseContainer.getExposedPorts();
        configuration.with(PORT, exposedPorts.get(0));

        configuration.with(USER, jdbcDatabaseContainer.getUsername());
        configuration.with(PASSWORD, jdbcDatabaseContainer.getPassword());

        final String driverClassName = jdbcDatabaseContainer.getDriverClassName();
        configuration.with(CONNECTOR, ConnectorResolver.getConnectorByJdbcDriver(driverClassName));

        return configuration;
    }
}
