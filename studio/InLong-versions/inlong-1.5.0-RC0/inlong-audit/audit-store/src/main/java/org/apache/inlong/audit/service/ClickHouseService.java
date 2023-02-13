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

package org.apache.inlong.audit.service;

import org.apache.inlong.audit.config.ClickHouseConfig;
import org.apache.inlong.audit.db.entities.ClickHouseDataPo;
import org.apache.inlong.audit.protocol.AuditData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ClickHouseService
 */
public class ClickHouseService implements InsertData, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ClickHouseService.class);
    public static final String INSERT_SQL = "insert into audit_data (ip, docker_id, thread_id,\r\n"
            + "      sdk_ts, packet_id, log_ts,\r\n"
            + "      inlong_group_id, inlong_stream_id, audit_id,\r\n"
            + "      count, size, delay, \r\n"
            + "      update_time)\r\n"
            + "    values (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private ClickHouseConfig chConfig;

    private ScheduledExecutorService timerService = Executors.newSingleThreadScheduledExecutor();
    private LinkedBlockingQueue<ClickHouseDataPo> batchQueue;
    private AtomicBoolean needBatchOutput = new AtomicBoolean(false);
    private AtomicInteger batchCounter = new AtomicInteger(0);
    private AtomicLong lastCheckTime = new AtomicLong(System.currentTimeMillis());
    private Connection conn;

    /**
     * Constructor
     * @param chConfig ClickHouse service config, such as jdbc url, jdbc username, jdbc password.
     */
    public ClickHouseService(ClickHouseConfig chConfig) {
        this.chConfig = chConfig;
    }

    /**
     * start
     */
    public void start() {
        // queue
        this.batchQueue = new LinkedBlockingQueue<>(
                chConfig.getBatchThreshold() * chConfig.getBatchIntervalMs() / chConfig.getProcessIntervalMs());
        // connection
        try {
            Class.forName(chConfig.getDriver());
            this.reconnect();
        } catch (Exception e) {
            LOG.error("ClickHouseService start failure!", e);
        }
        // start timer
        timerService.scheduleWithFixedDelay(this::processOutput,
                chConfig.getProcessIntervalMs(),
                chConfig.getProcessIntervalMs(), TimeUnit.MILLISECONDS);
    }

    /**
     * processOutput
     */
    private void processOutput() {
        if (!this.needBatchOutput.get()
                && (System.currentTimeMillis() - lastCheckTime.get() < chConfig.getBatchIntervalMs())) {
            return;
        }
        // output
        try (PreparedStatement pstat = this.conn.prepareStatement(INSERT_SQL)) {
            int counter = 0;
            // output data to clickhouse
            ClickHouseDataPo data = this.batchQueue.poll();
            while (data != null) {
                pstat.setString(1, data.getIp());
                pstat.setString(2, data.getDockerId());
                pstat.setString(3, data.getThreadId());
                pstat.setTimestamp(4, data.getSdkTs());
                pstat.setLong(5, data.getPacketId());
                pstat.setTimestamp(6, data.getLogTs());
                pstat.setString(7, data.getInlongGroupId());
                pstat.setString(8, data.getInlongStreamId());
                pstat.setString(9, data.getAuditId());
                pstat.setLong(10, data.getCount());
                pstat.setLong(11, data.getSize());
                pstat.setLong(12, data.getDelay());
                pstat.setTimestamp(13, data.getUpdateTime());
                pstat.addBatch();
                this.batchCounter.decrementAndGet();
                if (++counter >= chConfig.getBatchThreshold()) {
                    pstat.executeBatch();
                    this.conn.commit();
                    counter = 0;
                }
                data = this.batchQueue.poll();
            }
            if (counter > 0) {
                pstat.executeBatch();
                this.conn.commit();
            }
        } catch (Exception e1) {
            LOG.error("Execute output to clickhouse failure!", e1);
            // re-connect clickhouse
            try {
                this.reconnect();
            } catch (SQLException e2) {
                LOG.error("Re-connect clickhouse failure!", e2);
            }
        }
        // recover flag
        lastCheckTime.set(System.currentTimeMillis());
        this.needBatchOutput.compareAndSet(true, false);
    }

    /**
     * reconnect
     * @throws SQLException Exception when creating connection.
     */
    private void reconnect() throws SQLException {
        if (this.conn != null) {
            try {
                this.conn.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            this.conn = null;
        }
        this.conn = DriverManager.getConnection(chConfig.getUrl(), chConfig.getUsername(),
                chConfig.getPassword());
        this.conn.setAutoCommit(false);
    }

    /**
     * insert
     * @param msgBody audit data reading from Pulsar or other MessageQueue. 
     */
    @Override
    public void insert(AuditData msgBody) {
        ClickHouseDataPo data = new ClickHouseDataPo();
        data.setIp(msgBody.getIp());
        data.setThreadId(msgBody.getThreadId());
        data.setDockerId(msgBody.getDockerId());
        data.setPacketId(msgBody.getPacketId());
        data.setSdkTs(new Timestamp(msgBody.getSdkTs()));
        data.setLogTs(new Timestamp(msgBody.getLogTs()));
        data.setAuditId(msgBody.getAuditId());
        data.setCount(msgBody.getCount());
        data.setDelay(msgBody.getDelay());
        data.setInlongGroupId(msgBody.getInlongGroupId());
        data.setInlongStreamId(msgBody.getInlongStreamId());
        data.setSize(msgBody.getSize());
        data.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        try {
            this.batchQueue.offer(data, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            if (this.batchCounter.incrementAndGet() >= chConfig.getBatchThreshold()) {
                this.needBatchOutput.compareAndSet(false, true);
            }
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * close
     * @throws Exception Exception when closing ClickHouse connection.
     */
    @Override
    public void close() throws Exception {
        this.conn.close();
        this.timerService.shutdown();
    }
}
