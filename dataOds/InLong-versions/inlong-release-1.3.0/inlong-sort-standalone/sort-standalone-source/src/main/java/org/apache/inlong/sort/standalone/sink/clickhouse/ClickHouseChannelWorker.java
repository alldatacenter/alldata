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

package org.apache.inlong.sort.standalone.sink.clickhouse;

import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.dispatch.DispatchProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * ClickHouseChannelWorker
 */
public class ClickHouseChannelWorker extends Thread {

    public static final Logger LOG = LoggerFactory.getLogger(ClickHouseChannelWorker.class);

    private final ClickHouseSinkContext context;
    private final int workerIndex;
    private LifecycleState status;
    private IEventHandler handler;
    private Connection conn;

    /**
     * Constructor
     * 
     * @param context
     * @param workerIndex
     */
    public ClickHouseChannelWorker(ClickHouseSinkContext context, int workerIndex) {
        this.context = context;
        this.workerIndex = workerIndex;
        this.status = LifecycleState.IDLE;
        this.handler = this.context.createEventHandler();
    }

    /**
     * run
     */
    @Override
    public void run() {
        status = LifecycleState.START;
        LOG.info("start to ClickHouseChannelWorker:{},status:{},index:{}", context.getTaskName(), status, workerIndex);
        while (status == LifecycleState.START) {
            try {
                this.doRun();
            } catch (Throwable t) {
                LOG.error(t.getMessage(), t);
            }
        }
    }

    /**
     * doRun
     */
    public void doRun() {
        DispatchProfile currentRecord = context.getDispatchQueue().poll();
        try {
            // prepare
            if (currentRecord == null) {
                this.sleepOneInterval();
                return;
            }
            // check config
            ClickHouseIdConfig idConfig = context.getIdConfig(currentRecord.getUid());
            if (idConfig == null) {
                context.addSendFailMetric("idConfig is null", currentRecord);
                currentRecord.ack();
                return;
            }
            // check sql
            String insertSql = idConfig.getInsertSql();
            if (insertSql == null) {
                context.addSendFailMetric("sql is null", currentRecord);
                currentRecord.ack();
                return;
            }
            // execute sql
            if (this.conn == null) {
                this.reconnect();
            }
            try (PreparedStatement pstat = this.conn.prepareStatement(insertSql)) {
                for (ProfileEvent event : currentRecord.getEvents()) {
                    Map<String, String> columnValueMap = this.handler.parse(idConfig, event);
                    this.handler.setValue(idConfig, columnValueMap, pstat);
                    pstat.addBatch();
                }
                pstat.executeBatch();
                this.conn.commit();
            } catch (Exception e) {
                this.reconnect();
            }
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            if (currentRecord != null) {
                context.getDispatchQueue().add(currentRecord);
            }
            this.sleepOneInterval();
        }
    }

    /**
     * close
     */
    public void close() {
        this.status = LifecycleState.STOP;
    }

    /**
     * sleepOneInterval
     */
    private void sleepOneInterval() {
        try {
            Thread.sleep(context.getProcessInterval());
        } catch (InterruptedException e1) {
            LOG.error(e1.getMessage(), e1);
        }
    }

    /**
     * reconnect
     * @throws SQLException
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
        this.conn = DriverManager.getConnection(context.getJdbcUrl(), context.getJdbcUsername(),
                context.getJdbcPassword());
        this.conn.setAutoCommit(false);
    }
}
