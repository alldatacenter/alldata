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

package org.apache.inlong.manager.service.resource.sink.hive;

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
import org.apache.inlong.manager.pojo.node.hive.HiveDataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.hive.HiveColumnInfo;
import org.apache.inlong.manager.pojo.sink.hive.HiveSinkDTO;
import org.apache.inlong.manager.pojo.sink.hive.HiveTableInfo;
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
 * Hive resource operator
 */
@Service
public class HiveResourceOperator implements SinkResourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HiveResourceOperator.class);
    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private StreamSinkFieldEntityMapper sinkFieldMapper;
    @Autowired
    private DataNodeOperateHelper dataNodeHelper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.HIVE.equals(sinkType);
    }

    /**
     * Create hive table according to the groupId and hive config
     */
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

    private HiveSinkDTO getHiveInfo(SinkInfo sinkInfo) {
        HiveSinkDTO hiveInfo = HiveSinkDTO.getFromJson(sinkInfo.getExtParams());

        // read from data node if not supplied by user
        if (StringUtils.isBlank(hiveInfo.getJdbcUrl())) {
            String dataNodeName = sinkInfo.getDataNodeName();
            Preconditions.checkNotEmpty(dataNodeName, "hive jdbc url not specified and data node is empty");
            HiveDataNodeInfo dataNodeInfo = (HiveDataNodeInfo) dataNodeHelper.getDataNodeInfo(
                    dataNodeName, sinkInfo.getSinkType());
            CommonBeanUtils.copyProperties(dataNodeInfo, hiveInfo);
            hiveInfo.setJdbcUrl(dataNodeInfo.getUrl());
            hiveInfo.setPassword(dataNodeInfo.getToken());
        }
        return hiveInfo;
    }

    private void createTable(SinkInfo sinkInfo) {
        LOGGER.info("begin to create hive table for sinkId={}", sinkInfo.getId());

        List<StreamSinkFieldEntity> fieldList = sinkFieldMapper.selectBySinkId(sinkInfo.getId());
        if (CollectionUtils.isEmpty(fieldList)) {
            LOGGER.warn("no hive fields found, skip to create table for sinkId={}", sinkInfo.getId());
        }

        // set columns
        List<HiveColumnInfo> columnList = new ArrayList<>();
        for (StreamSinkFieldEntity field : fieldList) {
            HiveColumnInfo columnInfo = new HiveColumnInfo();
            columnInfo.setName(field.getFieldName());
            columnInfo.setType(field.getFieldType());
            columnInfo.setDesc(field.getFieldComment());
            columnList.add(columnInfo);
        }

        try {
            HiveSinkDTO hiveInfo = this.getHiveInfo(sinkInfo);
            HiveTableInfo tableInfo = HiveSinkDTO.getHiveTableInfo(hiveInfo, columnList);
            String url = hiveInfo.getJdbcUrl();
            String user = hiveInfo.getUsername();
            String password = hiveInfo.getPassword();

            String dbName = tableInfo.getDbName();
            String tableName = tableInfo.getTableName();

            // 1. create database if not exists
            HiveJdbcUtils.createDb(url, user, password, dbName);

            // 2. check if the table exists
            List<String> tables = HiveJdbcUtils.getTables(url, user, password, dbName);
            boolean tableExists = tables.contains(tableName);

            // 3. table not exists, create it
            if (!tableExists) {
                HiveJdbcUtils.createTable(url, user, password, tableInfo);
            } else {
                // 4. table exists, add columns - skip the exists columns
                List<HiveColumnInfo> existColumns = HiveJdbcUtils.getColumns(url, user, password, dbName, tableName);
                List<HiveColumnInfo> needAddColumns = tableInfo.getColumns().stream()
                        .skip(existColumns.size()).collect(toList());
                if (CollectionUtils.isNotEmpty(needAddColumns)) {
                    HiveJdbcUtils.addColumns(url, user, password, dbName, tableName, needAddColumns);
                }
            }

            // 5. update the sink status to success
            String info = "success to create hive resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOGGER.info(info + " for sinkInfo={}", sinkInfo);
        } catch (Throwable e) {
            String errMsg = "create hive table failed: " + e.getMessage();
            LOGGER.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }
    }

}
