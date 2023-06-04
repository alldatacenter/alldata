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

package org.apache.inlong.manager.service.resource.sink.hudi;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.pojo.node.hudi.HudiDataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.hudi.HudiColumnInfo;
import org.apache.inlong.manager.pojo.sink.hudi.HudiSinkDTO;
import org.apache.inlong.manager.pojo.sink.hudi.HudiTableInfo;
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
 * Hudi resource operator
 */
@Service
public class HudiResourceOperator implements SinkResourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HudiResourceOperator.class);

    private static final String CATALOG_TYPE_HIVE = "HIVE";

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private StreamSinkFieldEntityMapper sinkFieldMapper;
    @Autowired
    private DataNodeOperateHelper dataNodeHelper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.HUDI.equals(sinkType);
    }

    /**
     * Create Hudi table according to the sink config
     */
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

        this.createTableIfAbsent(sinkInfo);
    }

    private HudiSinkDTO getHudiInfo(SinkInfo sinkInfo) {
        HudiSinkDTO hudiInfo = HudiSinkDTO.getFromJson(sinkInfo.getExtParams());

        // read uri from data node if not supplied by user
        if (StringUtils.isBlank(hudiInfo.getCatalogUri())
                && CATALOG_TYPE_HIVE.equals(hudiInfo.getCatalogType())) {
            String dataNodeName = sinkInfo.getDataNodeName();
            Preconditions.expectNotBlank(dataNodeName, ErrorCodeEnum.INVALID_PARAMETER,
                    "Hudi catalog uri not specified and data node is empty");
            HudiDataNodeInfo dataNodeInfo = (HudiDataNodeInfo) dataNodeHelper.getDataNodeInfo(
                    dataNodeName, sinkInfo.getSinkType());
            CommonBeanUtils.copyProperties(dataNodeInfo, hudiInfo);
            hudiInfo.setCatalogUri(dataNodeInfo.getUrl());
        }

        hudiInfo.setDataPath(hudiInfo.getWarehouse() + "/" + hudiInfo.getDbName() + ".db/" + hudiInfo.getTableName());
        return hudiInfo;
    }

    private void createTableIfAbsent(SinkInfo sinkInfo) {
        LOGGER.info("begin to create hudi table for sinkInfo={}", sinkInfo);

        // Get all info from config
        HudiSinkDTO hudiInfo = getHudiInfo(sinkInfo);
        List<HudiColumnInfo> columnInfoList = getColumnList(sinkInfo);
        if (CollectionUtils.isEmpty(columnInfoList)) {
            throw new IllegalArgumentException("no hudi columns specified");
        }
        HudiTableInfo tableInfo = HudiSinkDTO.getHudiTableInfo(hudiInfo, columnInfoList);

        String metastoreUri = hudiInfo.getCatalogUri();
        String warehouse = hudiInfo.getWarehouse();
        String dbName = hudiInfo.getDbName();
        String tableName = hudiInfo.getTableName();

        HudiCatalogClient client = null;
        try {
            client = new HudiCatalogClient(metastoreUri, warehouse, dbName);
            client.open();

            // 1. create database if not exists
            client.createDatabase(warehouse, true);
            // 2. check if the table exists
            boolean tableExists = client.tableExist(tableName);

            if (!tableExists) {
                // 3. create table
                client.createTable(tableName, tableInfo, true);
            } else {
                // 4. or update table columns
                List<HudiColumnInfo> existColumns = client.getColumns(dbName, tableName);
                List<HudiColumnInfo> needAddColumns = tableInfo.getColumns().stream().skip(existColumns.size())
                        .collect(toList());
                if (CollectionUtils.isNotEmpty(needAddColumns)) {
                    client.addColumns(tableName, needAddColumns);
                    LOGGER.info("{} columns added for table {}", needAddColumns.size(), tableName);
                }
            }
            String info = "success to create Hudi resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOGGER.info(info + " for sinkInfo = {}", info);
        } catch (Throwable e) {
            String errMsg = "create Hudi table failed: " + e.getMessage();
            LOGGER.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private List<HudiColumnInfo> getColumnList(SinkInfo sinkInfo) {
        List<StreamSinkFieldEntity> fieldList = sinkFieldMapper.selectBySinkId(sinkInfo.getId());

        // set columns
        List<HudiColumnInfo> columnList = new ArrayList<>();
        for (StreamSinkFieldEntity field : fieldList) {
            HudiColumnInfo column = HudiColumnInfo.getFromJson(field.getExtParams());
            column.setName(field.getFieldName());
            column.setType(field.getFieldType());
            column.setDesc(field.getFieldComment());
            column.setRequired(field.getIsRequired() != null && field.getIsRequired() > 0);
            columnList.add(column);
        }

        return columnList;
    }
}
