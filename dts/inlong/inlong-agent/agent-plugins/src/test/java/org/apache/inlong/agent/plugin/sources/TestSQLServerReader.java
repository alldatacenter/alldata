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

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.CommonConstants;
import org.apache.inlong.agent.constant.SqlServerConstants;
import org.apache.inlong.agent.metrics.AgentMetricItem;
import org.apache.inlong.agent.metrics.AgentMetricItemSet;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.sources.reader.SQLServerReader;
import org.apache.inlong.agent.plugin.sources.snapshot.SqlServerSnapshotBase;
import org.apache.inlong.common.metric.MetricRegister;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.field;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Test cases for {@link SQLServerReader}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DebeziumEngine.class, Executors.class, MetricRegister.class, SQLServerReader.class})
@PowerMockIgnore({"javax.management.*"})
public class TestSQLServerReader {

    private SQLServerReader reader;

    @Mock
    private JobProfile jobProfile;

    @Mock
    private AgentMetricItemSet agentMetricItemSet;

    @Mock
    private AgentMetricItem agentMetricItem;

    @Mock
    private SqlServerSnapshotBase sqlServerSnapshot;

    @Mock
    private DebeziumEngine.Builder builder;

    @Mock
    private ExecutorService executorService;

    @Mock
    private LinkedBlockingQueue<Pair<String, String>> sqlServerMessageQueue;

    @Mock
    private DebeziumEngine<ChangeEvent<String, String>> engine;

    private AtomicLong atomicLong;

    private AtomicLong atomicCountLong;

    private final String instanceId = "s4bc475560b4444dbd4e9812ab1fd64d";

    @Before
    public void setUp() throws Exception {
        final String username = "sa";
        final String password = "123456";
        final String hostname = "127.0.0.1";
        final String port = "1434";
        final String groupId = "group01";
        final String streamId = "stream01";
        final String dbName = "inlong";
        final String serverName = "server1";
        final String offsetFlushIntervalMs = "1000";
        final String offsetStoreFileName = "/opt/offset.dat";
        final String snapshotMode = SqlServerConstants.INITIAL;
        final int queueSize = 1000;
        final String databaseStoreHistoryName = "/opt/history.dat";
        final String offset = "111";
        final String specificOffsetFile = "";
        final String specificOffsetPos = "-1";

        atomicLong = new AtomicLong(0L);
        atomicCountLong = new AtomicLong(0L);

        when(jobProfile.getInstanceId()).thenReturn(instanceId);
        when(jobProfile.get(eq(CommonConstants.PROXY_INLONG_GROUP_ID), anyString())).thenReturn(groupId);
        when(jobProfile.get(eq(CommonConstants.PROXY_INLONG_STREAM_ID), anyString())).thenReturn(streamId);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_USER))).thenReturn(username);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_PASSWORD))).thenReturn(password);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_HOSTNAME))).thenReturn(hostname);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_PORT))).thenReturn(port);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_DBNAME))).thenReturn(dbName);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_SERVER_NAME))).thenReturn(serverName);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_STORE_OFFSET_INTERVAL_MS), anyString())).thenReturn(
                offsetFlushIntervalMs);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_STORE_HISTORY_FILENAME), anyString())).thenReturn(
                offsetStoreFileName);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_SNAPSHOT_MODE), anyString())).thenReturn(snapshotMode);
        when(jobProfile.getInt(eq(SQLServerReader.JOB_DATABASE_QUEUE_SIZE), anyInt())).thenReturn(queueSize);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_STORE_HISTORY_FILENAME))).thenReturn(
                databaseStoreHistoryName);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_OFFSETS), anyString())).thenReturn(offset);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_FILE), anyString())).thenReturn(
                specificOffsetFile);
        when(jobProfile.get(eq(SQLServerReader.JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_POS), anyString())).thenReturn(
                specificOffsetPos);
        whenNew(SqlServerSnapshotBase.class).withAnyArguments().thenReturn(sqlServerSnapshot);

        // mock sqlServerMessageQueue
        whenNew(LinkedBlockingQueue.class).withAnyArguments().thenReturn(sqlServerMessageQueue);

        // mock DebeziumEngine
        mockStatic(DebeziumEngine.class);
        when(DebeziumEngine.create(io.debezium.engine.format.Json.class)).thenReturn(builder);
        when(builder.using(any(Properties.class))).thenReturn(builder);
        when(builder.notifying(any(DebeziumEngine.ChangeConsumer.class))).thenReturn(builder);
        when(builder.using(any(DebeziumEngine.CompletionCallback.class))).thenReturn(builder);
        when(builder.build()).thenReturn(engine);

        // mock executorService
        mockStatic(Executors.class);
        when(Executors.newSingleThreadExecutor()).thenReturn(executorService);

        // mock metrics
        whenNew(AgentMetricItemSet.class).withArguments(anyString()).thenReturn(agentMetricItemSet);
        when(agentMetricItemSet.findMetricItem(any())).thenReturn(agentMetricItem);
        field(AgentMetricItem.class, "pluginReadCount").set(agentMetricItem, atomicLong);
        field(AgentMetricItem.class, "pluginReadSuccessCount").set(agentMetricItem, atomicCountLong);

        // init method
        mockStatic(MetricRegister.class);
        (reader = new SQLServerReader()).init(jobProfile);
    }

    /**
     * Test cases for {@link SQLServerReader#read()}.
     */
    @Test
    public void testRead() throws Exception {
        final String right = "value";
        final String left = "key";
        final String dataKey = "dataKey";
        when(sqlServerMessageQueue.isEmpty()).thenReturn(true);
        assertEquals(null, reader.read());
        when(sqlServerMessageQueue.isEmpty()).thenReturn(false);
        when(sqlServerMessageQueue.poll()).thenReturn(Pair.of(left, right));
        Message result = reader.read();
        assertEquals(String.join(right, "\"", "\""), result.toString());
        assertEquals(left, result.getHeader().get(dataKey));
    }

    /**
     * Test cases for {@link SQLServerReader#destroy()}.
     */
    @Test
    public void testDestroy() throws Exception {
        assertFalse(reader.isDestroyed());
        reader.destroy();
        verify(executorService).shutdownNow();
        verify(sqlServerSnapshot).close();
        assertTrue(reader.isDestroyed());
    }

    /**
     * Test cases for {@link SQLServerReader#finishRead()}.
     */
    @Test
    public void testFinishRead() throws Exception {
        assertFalse(reader.isFinished());
        reader.finishRead();
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
        final String snapShort = "snapShort";
        when(sqlServerSnapshot.getSnapshot()).thenReturn(snapShort);
        assertEquals(snapShort, reader.getSnapshot());
    }

    /**
     * Test cases for {@link SQLServerReader#getReadSource()}.
     */
    @Test
    public void testGetReadSource() {
        assertEquals(instanceId, reader.getReadSource());
    }
}
