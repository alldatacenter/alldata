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

package org.apache.inlong.manager.service.resource.postgres;

import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.pojo.sink.SinkInfo;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresColumnInfo;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresSinkDTO;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresTableInfo;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.service.resource.SinkResourceOperator;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Postgres resource operator
 */
@Service
public class PostgresResourceOperator implements SinkResourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresResourceOperator.class);

    @Autowired
    private StreamSinkService sinkService;

    @Autowired
    private StreamSinkFieldEntityMapper postgresFieldMapper;

    @Override
    public Boolean accept(SinkType sinkType) {
        return SinkType.POSTGRES == sinkType;
    }

    /**
     * Create Postgres sink resource
     *
     * @param sinkInfo Postgres sink info
     */
    @Override
    public void createSinkResource(SinkInfo sinkInfo) {
        LOGGER.info("begin to create postgres res sinkId={}", sinkInfo.getId());
        if (SinkStatus.CONFIG_SUCCESSFUL.getCode().equals(sinkInfo.getStatus())) {
            LOGGER.warn("postgres resource [" + sinkInfo.getId() + "] already success, skip to create");
            return;
        } else if (GlobalConstants.DISABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource())) {
            LOGGER.warn("create resource was disabled, skip to create for [" + sinkInfo.getId() + "]");
            return;
        }
        this.createTable(sinkInfo);
    }

    /**
     * Create Postgres table
     */
    private void createTable(SinkInfo sinkInfo) {
        LOGGER.info("begin to create postgres table for sinkId={}", sinkInfo.getId());
        List<StreamSinkFieldEntity> fieldList = postgresFieldMapper.selectBySinkId(sinkInfo.getId());
        if (CollectionUtils.isEmpty(fieldList)) {
            LOGGER.warn("no postgres fields found, skip to create table for sinkId={}", sinkInfo.getId());
        }

        // set columns
        List<PostgresColumnInfo> columnList = new ArrayList<>();
        for (StreamSinkFieldEntity field : fieldList) {
            PostgresColumnInfo columnInfo = new PostgresColumnInfo();
            columnInfo.setName(field.getFieldName());
            columnInfo.setType(field.getFieldType());
            columnInfo.setDesc(field.getFieldComment());
            columnList.add(columnInfo);
        }

        try {
            PostgresSinkDTO pgInfo = PostgresSinkDTO.getFromJson(sinkInfo.getExtParams());
            PostgresTableInfo tableInfo = PostgresSinkDTO.getPostgresTableInfo(pgInfo, columnList);
            String url = pgInfo.getJdbcUrl();
            String user = pgInfo.getUsername();
            String password = pgInfo.getPassword();

            String dbName = tableInfo.getDbName();
            String tableName = tableInfo.getTableName();

            // 1. create database if not exists
            PostgresJdbcUtils.createDb(url, user, password, dbName);

            // 2. check if the table exists
            boolean tableExists = PostgresJdbcUtils.checkTablesExist(url, user, password, dbName, tableName);

            // 3. table not exists, create it
            if (!tableExists) {
                PostgresJdbcUtils.createTable(url, user, password, tableInfo);
            } else {
                // 4. table exists, add columns - skip the exists columns
                List<PostgresColumnInfo> existColumns = PostgresJdbcUtils.getColumns(url, user, password, tableName);
                List<String> columnNameList = new ArrayList<>();
                existColumns.forEach(e -> columnNameList.add(e.getName()));
                List<PostgresColumnInfo> needAddColumns = tableInfo.getColumns().stream()
                        .filter((pgcInfo) -> !columnNameList.contains(pgcInfo.getName())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(needAddColumns)) {
                    PostgresJdbcUtils.addColumns(url, user, password, tableName, needAddColumns);
                }
            }

            // 5. update the sink status to success
            String info = "success to create postgres resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOGGER.info(info + " for sinkInfo={}", sinkInfo);
        } catch (Throwable e) {
            String errMsg = "create postgres table failed: " + e.getMessage();
            LOGGER.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }

        LOGGER.info("success create postgres table for data sink [" + sinkInfo.getId() + "]");
    }

}
