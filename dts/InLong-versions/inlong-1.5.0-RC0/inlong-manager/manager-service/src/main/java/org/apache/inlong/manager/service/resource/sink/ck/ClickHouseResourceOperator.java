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

package org.apache.inlong.manager.service.resource.sink.ck;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.pojo.node.ck.ClickHouseDataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseColumnInfo;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseSinkDTO;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseTableInfo;
import org.apache.inlong.manager.service.node.DataNodeOperateHelper;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperator;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Operator for clickHouse resource, such as create sink resource, clickHouse tables, etc.
 */
@Service
public class ClickHouseResourceOperator implements SinkResourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseResourceOperator.class);
    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private StreamSinkFieldEntityMapper clickHouseFieldMapper;
    @Autowired
    private DataNodeOperateHelper dataNodeHelper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.CLICKHOUSE.equals(sinkType);
    }

    @Override
    public void createSinkResource(SinkInfo sinkInfo) {
        if (sinkInfo == null) {
            LOGGER.warn("sink info was null, skip to create resource");
            return;
        }

        if (SinkStatus.CONFIG_SUCCESSFUL.getCode().equals(sinkInfo.getStatus())) {
            LOGGER.warn("sink resource [" + sinkInfo.getId() + "] already success, skip to create");
            return;
        } else if (InlongConstants.DISABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource())) {
            LOGGER.warn("create resource was disabled, skip to create for [" + sinkInfo.getId() + "]");
            return;
        }

        this.createTable(sinkInfo);
    }

    private ClickHouseSinkDTO getClickHouseInfo(SinkInfo sinkInfo) {
        ClickHouseSinkDTO ckInfo = ClickHouseSinkDTO.getFromJson(sinkInfo.getExtParams());

        // read from data node if not supplied by user
        if (StringUtils.isBlank(ckInfo.getJdbcUrl())) {
            String dataNodeName = sinkInfo.getDataNodeName();
            Preconditions.checkNotEmpty(dataNodeName, "clickhouse jdbc url not specified and data node is empty");
            ClickHouseDataNodeInfo dataNodeInfo = (ClickHouseDataNodeInfo) dataNodeHelper.getDataNodeInfo(
                    dataNodeName, sinkInfo.getSinkType());
            CommonBeanUtils.copyProperties(dataNodeInfo, ckInfo);
            ckInfo.setJdbcUrl(dataNodeInfo.getUrl());
            ckInfo.setPassword(dataNodeInfo.getToken());
        }
        return ckInfo;
    }

    private void createTable(SinkInfo sinkInfo) {
        LOGGER.info("begin to create clickhouse table for sinkId={}", sinkInfo.getId());

        List<StreamSinkFieldEntity> fieldList = clickHouseFieldMapper.selectBySinkId(sinkInfo.getId());
        if (CollectionUtils.isEmpty(fieldList)) {
            LOGGER.warn("no clickhouse fields found, skip to create table for sinkId={}", sinkInfo.getId());
        }

        // set columns
        List<ClickHouseColumnInfo> columnList = new ArrayList<>();
        for (StreamSinkFieldEntity field : fieldList) {
            ClickHouseColumnInfo columnInfo = new ClickHouseColumnInfo();
            columnInfo.setName(field.getFieldName());
            columnInfo.setType(field.getFieldType());
            columnInfo.setDesc(field.getFieldComment());
            columnList.add(columnInfo);
        }

        try {
            ClickHouseSinkDTO ckInfo = getClickHouseInfo(sinkInfo);
            ClickHouseTableInfo tableInfo = ClickHouseSinkDTO.getClickHouseTableInfo(ckInfo, columnList);
            String url = ckInfo.getJdbcUrl();
            String user = ckInfo.getUsername();
            String password = ckInfo.getPassword();

            String dbName = tableInfo.getDbName();
            String tableName = tableInfo.getTableName();

            // 1. create database if not exists
            ClickHouseJdbcUtils.createDb(url, user, password, dbName);

            // 2. check if the table exists
            List<String> tables = ClickHouseJdbcUtils.getTables(url, user, password, dbName);
            boolean tableExists = tables.contains(tableName);

            // 3. table not exists, create it
            if (!tableExists) {
                ClickHouseJdbcUtils.createTable(url, user, password, tableInfo);
            } else {
                // 4. table exists, add columns - skip the exists columns
                List<ClickHouseColumnInfo> existColumns = ClickHouseJdbcUtils.getColumns(url,
                        user, password, dbName, tableName);
                List<ClickHouseColumnInfo> needAddColumns = tableInfo.getColumns().stream()
                        .skip(existColumns.size()).collect(toList());
                if (CollectionUtils.isNotEmpty(needAddColumns)) {
                    ClickHouseJdbcUtils.addColumns(url, user, password, dbName, tableName, needAddColumns);
                }
            }

            // 5. update the sink status to success
            String info = "success to create clickhouse resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOGGER.info(info + " for sinkInfo={}", sinkInfo);
        } catch (Throwable e) {
            String errMsg = "create clickhouse table failed: " + e.getMessage();
            LOGGER.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }

        LOGGER.info("success create ClickHouse table for sink id [" + sinkInfo.getId() + "]");
    }

}
