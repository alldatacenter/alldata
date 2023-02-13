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

package org.apache.inlong.agent.plugin.sources.reader;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.utils.AgentDbUtils;
import org.apache.inlong.agent.utils.AgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static java.sql.Types.BINARY;
import static java.sql.Types.BLOB;
import static java.sql.Types.LONGVARBINARY;
import static java.sql.Types.VARBINARY;

/**
 * Read data from database by SQL
 */
public class SqlReader extends AbstractReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlReader.class);

    private static final String SQL_READER_TAG_NAME = "AgentSqlMetric";

    private static final String JOB_DATABASE_USER = "job.sql.user";
    private static final String JOB_DATABASE_PASSWORD = "job.sql.password";
    private static final String JOB_DATABASE_HOSTNAME = "job.sql.hostname";
    private static final String JOB_DATABASE_PORT = "job.sql.port";

    private static final String JOB_DATABASE_BATCH_SIZE = "job.sql.batchSize";
    private static final int DEFAULT_JOB_DATABASE_BATCH_SIZE = 1000;

    private static final String JOB_DATABASE_DRIVER_CLASS = "job.database.driverClass";
    private static final String DEFAULT_JOB_DATABASE_DRIVER_CLASS = "com.mysql.jdbc.Driver";

    // pre-set sql lines, commands like "set xxx=xx;"
    private static final String JOB_DATABASE_TYPE = "job.database.type";
    private static final String MYSQL = "mysql";

    /* Standard short field separator */
    private static final String STD_FIELD_SEPARATOR_SHORT = "\001";
    private static final String JOB_DATABASE_SEPARATOR = "job.sql.separator";
    private static final String[] NEW_LINE_CHARS = new String[]{String.valueOf(CharUtils.CR),
            String.valueOf(CharUtils.LF)};
    private static final String[] EMPTY_CHARS = new String[]{StringUtils.EMPTY, StringUtils.EMPTY};

    private final String sql;

    // use statement for mysql due to compatibility
    private Statement statement;
    private PreparedStatement preparedStatement;
    private Connection conn;
    private ResultSet resultSet;
    private int columnCount;

    // column types
    private String[] columnTypeNames;
    private int[] columnTypeCodes;
    private boolean finished = false;
    private String separator;

    public SqlReader(String sql) {
        this.sql = sql;
    }

    @Override
    public Message read() {
        try {
            if (resultSet.next()) {
                List<String> lineColumns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String dataValue = null;
                    /* handle special blob value, encode with base64, BLOB=2004 */
                    int typeCode = columnTypeCodes[i - 1];
                    String typeName = columnTypeNames[i - 1];

                    // binary type
                    if (typeCode == BLOB || typeCode == BINARY || typeCode == VARBINARY
                            || typeCode == LONGVARBINARY || typeName.contains("BLOB")) {
                        byte[] data = resultSet.getBytes(i);
                        dataValue = new String(Base64.encodeBase64(data, false), StandardCharsets.UTF_8);
                    } else {
                        // non-binary type
                        dataValue = StringUtils.replaceEachRepeatedly(resultSet.getString(i),
                                NEW_LINE_CHARS, EMPTY_CHARS);
                    }
                    lineColumns.add(dataValue);
                }
                long dataSize = lineColumns.stream().mapToLong(column -> column.length()).sum();
                AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_SUCCESS,
                        inlongGroupId, inlongStreamId, System.currentTimeMillis(), 1, dataSize);
                readerMetric.pluginReadSuccessCount.incrementAndGet();
                readerMetric.pluginReadCount.incrementAndGet();
                return generateMessage(lineColumns);
            } else {
                finished = true;
            }
        } catch (Exception ex) {
            LOGGER.error("error while reading data", ex);
            readerMetric.pluginReadFailCount.incrementAndGet();
            readerMetric.pluginReadCount.incrementAndGet();
            throw new RuntimeException(ex);
        }
        return null;
    }

    private Message generateMessage(List<String> lineColumns) {
        return new DefaultMessage(StringUtils.join(lineColumns, separator).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getReadSource() {
        return sql;
    }

    @Override
    public void setReadTimeout(long mill) {

    }

    @Override
    public void setWaitMillisecond(long millis) {

    }

    @Override
    public String getSnapshot() {
        return StringUtils.EMPTY;
    }

    @Override
    public void finishRead() {
        destroy();
    }

    @Override
    public boolean isSourceExist() {
        return true;
    }

    /**
     * Init column meta data.
     *
     * @throws Exception - sql exception
     */
    private void initColumnMeta() throws Exception {
        columnCount = resultSet.getMetaData().getColumnCount();
        columnTypeNames = new String[columnCount];
        columnTypeCodes = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnTypeCodes[i] = resultSet.getMetaData().getColumnType(i + 1);
            String t = resultSet.getMetaData().getColumnTypeName(i + 1);
            if (t != null) {
                columnTypeNames[i] = t.toUpperCase();
            }
        }
    }

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        int batchSize = jobConf.getInt(JOB_DATABASE_BATCH_SIZE, DEFAULT_JOB_DATABASE_BATCH_SIZE);
        String userName = jobConf.get(JOB_DATABASE_USER);
        String password = jobConf.get(JOB_DATABASE_PASSWORD);
        String hostName = jobConf.get(JOB_DATABASE_HOSTNAME);
        int port = jobConf.getInt(JOB_DATABASE_PORT);

        String driverClass = jobConf.get(JOB_DATABASE_DRIVER_CLASS,
                DEFAULT_JOB_DATABASE_DRIVER_CLASS);
        separator = jobConf.get(JOB_DATABASE_SEPARATOR, STD_FIELD_SEPARATOR_SHORT);
        finished = false;
        try {
            String databaseType = jobConf.get(JOB_DATABASE_TYPE, MYSQL);
            String url = String.format("jdbc:%s://%s:%d", databaseType, hostName, port);
            conn = AgentDbUtils.getConnectionFailover(
                    driverClass, url, userName, password);
            if (databaseType.equals(MYSQL)) {
                statement = conn.createStatement(
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                statement.setFetchSize(Integer.MIN_VALUE);
                resultSet = statement.executeQuery(sql);
            } else {
                // better performance in non-mysql server
                preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setFetchSize(batchSize);
                resultSet = preparedStatement.executeQuery();
            }
            initColumnMeta();
        } catch (Exception ex) {
            LOGGER.error("error create statement", ex);
            destroy();
            throw new RuntimeException(ex);
        }

    }

    @Override
    public void destroy() {
        finished = true;
        AgentUtils.finallyClose(resultSet);
        AgentUtils.finallyClose(statement);
        AgentUtils.finallyClose(preparedStatement);
        AgentUtils.finallyClose(conn);
    }
}
