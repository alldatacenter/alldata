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

package org.apache.inlong.agent.plugin.sources;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.CommonConstants;
import org.apache.inlong.agent.metrics.AgentMetricItem;
import org.apache.inlong.agent.metrics.AgentMetricItemSet;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.sources.reader.SQLServerReader;
import org.apache.inlong.agent.utils.AgentDbUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.field;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Test cases for {@link SQLServerReader}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AgentDbUtils.class, AuditUtils.class, SQLServerReader.class})
@PowerMockIgnore({"javax.management.*"})
public class TestSQLServerReader {

    private SQLServerReader reader;

    @Mock
    private JobProfile jobProfile;

    @Mock
    private Connection conn;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData metaData;

    @Mock
    private AgentMetricItemSet agentMetricItemSet;

    @Mock
    private AgentMetricItem agentMetricItem;

    private AtomicLong atomicLong;

    private String sql;

    @Before
    public void setUp() throws Exception {
        final String username = "sa";
        final String password = "123456";
        final String hostname = "127.0.0.1";
        final String port = "1434";
        final String dbname = "inlong";
        final String typeName1 = "int";
        final String typeName2 = "varchar";
        final String groupId = "group01";
        final String streamId = "stream01";
        atomicLong = new AtomicLong(0L);

        sql = "select * from dbo.test01";

        when(jobProfile.get(eq(CommonConstants.PROXY_INLONG_GROUP_ID), anyString())).thenReturn(groupId);
        when(jobProfile.get(eq(CommonConstants.PROXY_INLONG_STREAM_ID), anyString())).thenReturn(streamId);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_USER))).thenReturn(username);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_PASSWORD))).thenReturn(password);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_HOSTNAME))).thenReturn(hostname);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_PORT))).thenReturn(port);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_DBNAME))).thenReturn(dbname);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_DRIVER_CLASS), anyString())).thenReturn(
                SQLServerReader.DEFAULT_JOB_DATABASE_DRIVER_CLASS);
        when(jobProfile.getInt(eq(SQLServerReader.JOB_DATABASE_BATCH_SIZE), anyInt())).thenReturn(
                SQLServerReader.DEFAULT_JOB_DATABASE_BATCH_SIZE);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_TYPE), anyString())).thenReturn(
                SQLServerReader.SQLSERVER);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_SEPARATOR), anyString())).thenReturn(
                SQLServerReader.STD_FIELD_SEPARATOR_SHORT);
        mockStatic(AgentDbUtils.class);
        when(AgentDbUtils.getConnectionFailover(eq(SQLServerReader.DEFAULT_JOB_DATABASE_DRIVER_CLASS), anyString(),
                eq(username), eq(password))).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnName(2)).thenReturn("cell");
        when(metaData.getColumnType(1)).thenReturn(Types.INTEGER);
        when(metaData.getColumnType(2)).thenReturn(Types.VARCHAR);
        when(metaData.getColumnTypeName(1)).thenReturn(typeName1);
        when(metaData.getColumnTypeName(2)).thenReturn(typeName2);

        //mock metrics
        whenNew(AgentMetricItemSet.class).withArguments(anyString()).thenReturn(agentMetricItemSet);
        when(agentMetricItemSet.findMetricItem(any())).thenReturn(agentMetricItem);
        field(AgentMetricItem.class, "pluginReadCount").set(agentMetricItem, atomicLong);

        //init method
        (reader = new SQLServerReader(sql)).init(jobProfile);
    }

    /**
     * Test cases for {@link SQLServerReader#read()}.
     */
    @Test
    public void testRead() throws Exception {
        final String v11 = "11";
        final String v12 = "12";
        final String v21 = "aa";
        final String v22 = "bb";

        final String msg1 = String.join(SQLServerReader.STD_FIELD_SEPARATOR_SHORT, v11, v12);
        final String msg2 = String.join(SQLServerReader.STD_FIELD_SEPARATOR_SHORT, v21, v22);

        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString(1)).thenReturn(v11, v21);
        when(resultSet.getString(2)).thenReturn(v12, v22);
        Message message1 = reader.read();
        assertEquals(msg1, message1.toString());
        verify(preparedStatement, times(1)).setFetchSize(SQLServerReader.DEFAULT_JOB_DATABASE_BATCH_SIZE);
        Message message2 = reader.read();
        assertEquals(msg2, message2.toString());
        assertEquals(2L, atomicLong.get());
    }

    /**
     * Test cases for {@link SQLServerReader#destroy()}.
     */
    @Test
    public void testDestroy() throws Exception {
        assertFalse(reader.isFinished());
        reader.destroy();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(conn).close();
        assertTrue(reader.isFinished());
    }

    /**
     * Test cases for {@link SQLServerReader#finishRead()}.
     */
    @Test
    public void testFinishRead() throws Exception {
        assertFalse(reader.isFinished());
        reader.destroy();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(conn).close();
        assertTrue(reader.isFinished());
    }

    /**
     * Test cases for {@link SQLServerReader#isSourceExist()}.
     */
    @Test
    public void testIsSourceExist() {
        assertTrue(reader.isSourceExist());
    }

    /**
     * Test cases for {@link SQLServerReader#getSnapshot()}.
     */
    @Test
    public void testGetSnapshot() {
        assertEquals(StringUtils.EMPTY, reader.getSnapshot());
    }

    /**
     * Test cases for {@link SQLServerReader#getReadSource()}.
     */
    @Test
    public void testGetReadSource() {
        assertEquals(sql, reader.getReadSource());
    }

    /**
     * Just using in local test.
     */
    @Ignore
    public void testSQLServerReader() {
        JobProfile jobProfile = JobProfile.parseJsonStr("{}");
        jobProfile.set(SQLServerReader.JOB_DATABASE_USER, "sa");
        jobProfile.set(SQLServerReader.JOB_DATABASE_PASSWORD, "123456");
        jobProfile.set(SQLServerReader.JOB_DATABASE_HOSTNAME, "127.0.0.1");
        jobProfile.set(SQLServerReader.JOB_DATABASE_PORT, "1434");
        jobProfile.set(SQLServerReader.JOB_DATABASE_DBNAME, "inlong");
        final String sql = "select * from dbo.test01";
        jobProfile.set(SQLServerSource.JOB_DATABASE_SQL, sql);
        final SQLServerSource source = new SQLServerSource();
        List<Reader> readers = source.split(jobProfile);
        for (Reader reader : readers) {
            reader.init(jobProfile);
            while (!reader.isFinished()) {
                Message message = reader.read();
                if (Objects.nonNull(message)) {
                    assertNotNull(message.getBody());
                }
            }
        }
    }
}
