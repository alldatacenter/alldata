/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.storage.mysql.history;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.debezium.util.DelayStrategy;
import org.apache.kafka.connect.errors.ConnectException;

import io.debezium.annotation.ThreadSafe;
import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.document.DocumentReader;
import io.debezium.document.DocumentWriter;
import io.debezium.relational.history.AbstractSchemaHistory;
import io.debezium.relational.history.HistoryRecord;
import io.debezium.relational.history.HistoryRecordComparator;
import io.debezium.relational.history.SchemaHistory;
import io.debezium.relational.history.SchemaHistoryException;
import io.debezium.relational.history.SchemaHistoryListener;
import io.debezium.util.Collect;
import io.debezium.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.resps.StreamEntry;

/**
 * A {@link SchemaHistory} implementation that stores the schema history into `default_mysql_history_schema_table`.
 *
 * @author AllDataDC
 */
@ThreadSafe
public final class MysqlSchemaHistory extends AbstractSchemaHistory {
    private static final String CONFIGURATION_FIELD_PREFIX_STRING = SchemaHistory.CONFIGURATION_FIELD_PREFIX_STRING + "mysql.";
    private static final String SQL_HISTORY_MYSQL_RECORDS = "Select source,position,databaseName,schemaName,ddl,tableChanges,ts_ms from default_mysql_history_schema_table";

    public static final class MYSQL_FIELDS {
        public static final String SOURCE = "source";
        public static final String POSITION = "position";
        public static final String DATABASE_NAME = "databaseName";
        public static final String SCHEMA_NAME = "schemaName";
        public static final String DDL_STATEMENTS = "ddl";
        public static final String TABLE_CHANGES = "tableChanges";
        public static final String TIMESTAMP = "ts_ms";
    }

    private static final String DEFAULT_MYSQL_HISTORY_SCHEMA_TABLE = "default_mysql_history_schema_table";
    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlSchemaHistory.class);

    public static final Field PROP_ADDRESS = Field.create(CONFIGURATION_FIELD_PREFIX_STRING + "url")
            .withDescription("The mysql url that will be used to access the database schema history");

    public static final Field PROP_USER = Field.create(CONFIGURATION_FIELD_PREFIX_STRING + "user")
            .withDescription("The mysql url that will be used to access the database schema history");

    public static final Field PROP_PASSWORD = Field.create(CONFIGURATION_FIELD_PREFIX_STRING + "password")
            .withDescription("The mysql url that will be used to access the database schema history");

    public static final Integer DEFAULT_RETRY_INITIAL_DELAY = 300;
    public static final Field PROP_RETRY_INITIAL_DELAY = Field.create(CONFIGURATION_FIELD_PREFIX_STRING + "retry.initial.delay.ms")
            .withDescription("Initial retry delay (in ms)")
            .withDefault(DEFAULT_RETRY_INITIAL_DELAY);

    public static final Integer DEFAULT_RETRY_MAX_DELAY = 10000;
    public static final Field PROP_RETRY_MAX_DELAY = Field.create(CONFIGURATION_FIELD_PREFIX_STRING + "retry.max.delay.ms")
            .withDescription("Maximum retry delay (in ms)")
            .withDefault(DEFAULT_RETRY_MAX_DELAY);

    public static final Integer DEFAULT_CONNECTION_TIMEOUT = 2000;
    public static final Field PROP_CONNECTION_TIMEOUT = Field.create(CONFIGURATION_FIELD_PREFIX_STRING + "connection.timeout.ms")
            .withDescription("Connection timeout (in ms)")
            .withDefault(DEFAULT_CONNECTION_TIMEOUT);

    public static final Integer DEFAULT_SOCKET_TIMEOUT = 2000;

    Duration initialRetryDelay;
    Duration maxRetryDelay;

    public static Collection<Field> ALL_FIELDS = Collect.arrayListOf(PROP_ADDRESS, PROP_USER, PROP_PASSWORD);

    private final DocumentWriter writer = DocumentWriter.defaultWriter();
    private final DocumentReader reader = DocumentReader.defaultReader();
    private final AtomicBoolean running = new AtomicBoolean();
    private Configuration config;
    private String mysqlKeyName;
    private String url;
    private String user;
    private String password;
    private boolean sslEnabled;
    private Integer connectionTimeout;

    private Connection connection = null;

    void connect() {
        //获取源表DDL语句, jdbc查询数据库show create table xxx.xxx
        try {
            if (connection == null || connection.isClosed()) {
                try {
                    connection = JDBCUtils.getConnection(url, user, password);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configure(Configuration config, HistoryRecordComparator comparator, SchemaHistoryListener listener, boolean useCatalogBeforeSchema) {
        if (!config.validateAndRecord(ALL_FIELDS, LOGGER::error)) {
            throw new ConnectException(
                    "Error configuring an instance of " + getClass().getSimpleName() + "; check the logs for details");
        }
        this.config = config;
        // fetch the properties
        this.url = this.config.getString(PROP_ADDRESS.name());
        this.user = this.config.getString(PROP_USER.name());
        this.password = this.config.getString(PROP_PASSWORD.name());
        // load retry settings
        this.initialRetryDelay = Duration.ofMillis(this.config.getInteger(PROP_RETRY_INITIAL_DELAY));
        this.maxRetryDelay = Duration.ofMillis(this.config.getInteger(PROP_RETRY_MAX_DELAY));
        // load connection timeout settings
        this.connectionTimeout = this.config.getInteger(PROP_CONNECTION_TIMEOUT);
        super.configure(config, comparator, listener, useCatalogBeforeSchema);
    }

    @Override
    public synchronized void start() {
        super.start();
        LOGGER.info("Starting MysqlSchemaHistory");
        this.connect();
    }

    @Override
    protected void storeRecord(HistoryRecord record) throws SchemaHistoryException {
        if (record == null) {
            return;
        }
        String line;
        try {
            line = writer.write(record.document());
        } catch (IOException e) {
            Throwables.logErrorAndTraceRecord(LOGGER, record, "Failed to convert record to string", e);
            throw new SchemaHistoryException("Unable to write database schema history record");
        }

        DelayStrategy delayStrategy = DelayStrategy.exponential(initialRetryDelay, maxRetryDelay);
        boolean completedSuccessfully = false;

        // loop and retry until successful
        while (!completedSuccessfully) {
            try {
                if (connection == null) {
                    this.connect();
                }

                // write the entry to Mysql
                insert((StreamEntryID) null, Collections.singletonMap("schema", line));
                LOGGER.trace("Record written to database schema history in mysql: " + line);
                completedSuccessfully = true;
            } catch (Exception e) {
                LOGGER.warn("Writing to database schema history stream failed", e);
                LOGGER.warn("Will retry");
            }
            if (!completedSuccessfully) {
                // Failed to execute the transaction, retry...
                delayStrategy.sleepWhen(!completedSuccessfully);
            }

        }
    }

    /**
     * 插入保存HistroyRecord
     * public static final class MYSQL_FIELDS {
     * public static final String SOURCE = "source";
     * public static final String POSITION = "position";
     * public static final String DATABASE_NAME = "databaseName";
     * public static final String SCHEMA_NAME = "schemaName";
     * public static final String DDL_STATEMENTS = "ddl";
     * public static final String TABLE_CHANGES = "tableChanges";
     * public static final String TIMESTAMP = "ts_ms";
     * }
     *
     * @param streamEntryID
     * @param schema
     */
    private void insert(StreamEntryID streamEntryID, Map<String, String> schema) {
        PreparedStatement stat = null;
        try {
            if (connection == null) {
                this.connect();
            }
            connection.setAutoCommit(false);
            List<String> records = (List<String>) schema.values();
            PreparedStatement preparedStatement = null;
            String sql = String.format("insert into %s(source,position,databaseName,schemaName,ddl,tableChanges,ts_ms)" +
                    " values(?,?,?,?,?,?,?)", DEFAULT_MYSQL_HISTORY_SCHEMA_TABLE);
            preparedStatement = connection.prepareStatement(sql);
            for (int i = 0; i < 10000; i++) {
                String record = records.get(i);
                //todo fetch field from record
                preparedStatement.setString(1, record + "test_Source");
                preparedStatement.setString(2, record + "test_position");
                preparedStatement.setString(3, record + "test_databaseName");
                preparedStatement.setString(4, record + "test_schemaName");
                preparedStatement.setString(5, record + "test_ddl");
                preparedStatement.setString(6, record + "test_tableChanges");
                preparedStatement.setString(7, record + "test_ts_ms");
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            preparedStatement.clearBatch();
            connection.commit();
            System.out.println("成功插入Mysql History Records 数据");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                JDBCUtils.close(null, null, connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * 读取HistoryRecords From Mysql
     * source,position,databaseName,schemaName,ddl,tableChanges,ts_ms
     *
     * @param streamEntryID
     * @param streamEntryID1
     * @return
     */
    private List<StreamEntry> readMysqlHistoryRecords(StreamEntryID streamEntryID, StreamEntryID streamEntryID1) {
        List<StreamEntry> streamEntries = new ArrayList<>();
        try {
            //获取源表DDL语句, jdbc查询数据库show create table xxx.xxx
            if (connection == null || connection.isClosed()) {
                this.connect();
            }
            PreparedStatement preparedStatement = connection.prepareStatement(String.format(SQL_HISTORY_MYSQL_RECORDS, streamEntryID, streamEntryID));
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Map<String, String> recordMap = new HashMap<>();
                recordMap.put("source", resultSet.getString("source"));
                recordMap.put("position", resultSet.getString("position"));
                recordMap.put("databaseName", resultSet.getString("databaseName"));
                recordMap.put("schemaName", resultSet.getString("schemaName"));
                recordMap.put("ddl", resultSet.getString("ddl"));
                recordMap.put("tableChanges", resultSet.getString("tableChanges"));
                recordMap.put("ts_ms", resultSet.getString("ts_ms"));
                StreamEntry streamEntry = new StreamEntry(streamEntryID, recordMap);
                streamEntries.add(streamEntry);
                System.out.println("读取Mysql History Records成功--streamEntry:\n" + streamEntry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


    @Override
    public void stop() {
        running.set(false);
        if (connection != null) {
            try {
                JDBCUtils.close(null, null, connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        super.stop();
    }

    @Override
    protected synchronized void recoverRecords(Consumer<HistoryRecord> records) {
        DelayStrategy delayStrategy = DelayStrategy.exponential(initialRetryDelay, maxRetryDelay);
        boolean completedSuccessfully = false;
        List<StreamEntry> entries = new ArrayList<StreamEntry>();

        // loop and retry until successful
        while (!completedSuccessfully) {
            try {
                if (connection == null) {
                    this.connect();
                }

                //todo read the entries from Mysql
                entries = readMysqlHistoryRecords((StreamEntryID) null, (StreamEntryID) null);
                completedSuccessfully = true;
            } catch (Exception e) {
                LOGGER.warn("Reading from database schema history stream failed with " + e);
                LOGGER.warn("Will retry");
            }
            if (!completedSuccessfully) {
                // Failed to execute the transaction, retry...
                delayStrategy.sleepWhen(!completedSuccessfully);
            }

        }

        for (StreamEntry item : entries) {
            try {
                records.accept(new HistoryRecord(reader.read(item.getFields().get("schema"))));
            } catch (IOException e) {
                LOGGER.error("Failed to convert record to string: {}", item, e);
                return;
            }
        }

    }

    @Override
    public boolean storageExists() {
        return true;
    }

    @Override
    public boolean exists() {
        //todo mysql judge if exist in default_mysql_history_schema_talbe
        return true;
    }
}
