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

package org.apache.inlong.manager.service.resource.sink.postgresql;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLColumnInfo;
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLSinkDTO;
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLTableInfo;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperator;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

/**
 * PostgreSQL's resource operator
 */
@Service
public class PostgreSQLResourceOperator implements SinkResourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSQLResourceOperator.class);

    private static final String POSTGRESQL_DEFAULT_SCHEMA = "public";

    @Autowired
    private StreamSinkService sinkService;

    @Autowired
    private StreamSinkFieldEntityMapper postgresFieldMapper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.POSTGRESQL.equals(sinkType);
    }

    @Override
    public void createSinkResource(SinkInfo sinkInfo) {
        LOGGER.info("begin to create postgresql resources sinkId={}", sinkInfo.getId());
        if (SinkStatus.CONFIG_SUCCESSFUL.getCode().equals(sinkInfo.getStatus())) {
            LOGGER.warn("postgresql resource [" + sinkInfo.getId() + "] already success, skip to create");
            return;
        } else if (InlongConstants.DISABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource())) {
            LOGGER.warn("create resource was disabled, skip to create for [" + sinkInfo.getId() + "]");
            return;
        }
        this.createTable(sinkInfo);
    }

    /**
     * Create PostgreSQL table
     */
    private void createTable(SinkInfo sinkInfo) {
        LOGGER.info("begin to create postgresql table for sinkId={}", sinkInfo.getId());
        List<StreamSinkFieldEntity> fieldList = postgresFieldMapper.selectBySinkId(sinkInfo.getId());
        if (CollectionUtils.isEmpty(fieldList)) {
            LOGGER.warn("no postgresql fields found, skip to create table for sinkId={}", sinkInfo.getId());
        }

        // set columns
        final List<PostgreSQLColumnInfo> columnList = Lists.newArrayList();
        fieldList.forEach(field -> {
            columnList.add(
                    new PostgreSQLColumnInfo(field.getFieldName(), field.getFieldType(), field.getFieldComment()));
        });

        PostgreSQLSinkDTO postgreSQLSink = PostgreSQLSinkDTO.getFromJson(sinkInfo.getExtParams());
        PostgreSQLTableInfo tableInfo = PostgreSQLSinkDTO.getTableInfo(postgreSQLSink, columnList);
        if (StringUtils.isEmpty(tableInfo.getSchemaName())) {
            tableInfo.setSchemaName(POSTGRESQL_DEFAULT_SCHEMA);
        }
        try (Connection conn = PostgreSQLJdbcUtils.getConnection(postgreSQLSink.getJdbcUrl(),
                postgreSQLSink.getUsername(), postgreSQLSink.getPassword())) {
            // 1.If schema not exists,create it
            PostgreSQLJdbcUtils.createSchema(conn, tableInfo.getTableName(), tableInfo.getUserName());
            // 2.If table not exists, create it
            PostgreSQLJdbcUtils.createTable(conn, tableInfo);
            // 3.Table exists, add columns - skip the exists columns
            PostgreSQLJdbcUtils.addColumns(conn, tableInfo.getSchemaName(), tableInfo.getTableName(), columnList);
            // 4.Update the sink status to success
            final String info = "success to create PostgreSQL resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOGGER.info(info + " for sinkInfo={}", sinkInfo);
            // 4. close connection.
        } catch (Throwable e) {
            String errMsg = "create PostgreSQL table failed: " + e.getMessage();
            LOGGER.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }
        LOGGER.info("success create PostgreSQL table for data sink [" + sinkInfo.getId() + "]");
    }

}
