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

package org.apache.inlong.manager.service.resource.sink.iceberg;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.node.iceberg.IcebergDataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergColumnInfo;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergSinkDTO;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergTableInfo;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
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
 * iceberg resource operator
 */
@Service
public class IcebergResourceOperator implements SinkResourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcebergResourceOperator.class);

    private static final String CATALOG_TYPE_HIVE = "HIVE";

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private StreamSinkFieldEntityMapper sinkFieldMapper;
    @Autowired
    private DataNodeOperateHelper dataNodeHelper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.ICEBERG.equals(sinkType);
    }

    /**
     * Create iceberg table according to the sink config
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

        this.createTable(sinkInfo);
    }

    private IcebergSinkDTO getIcebergInfo(SinkInfo sinkInfo) {
        IcebergSinkDTO icebergInfo = IcebergSinkDTO.getFromJson(sinkInfo.getExtParams());

        // read uri from data node if not supplied by user
        if (StringUtils.isBlank(icebergInfo.getCatalogUri())
                && CATALOG_TYPE_HIVE.equals(icebergInfo.getCatalogType())) {
            String dataNodeName = sinkInfo.getDataNodeName();
            Preconditions.checkNotEmpty(dataNodeName, "iceberg catalog uri not specified and data node is empty");
            IcebergDataNodeInfo dataNodeInfo = (IcebergDataNodeInfo) dataNodeHelper.getDataNodeInfo(
                    dataNodeName, sinkInfo.getSinkType());
            CommonBeanUtils.copyProperties(dataNodeInfo, icebergInfo);
            icebergInfo.setCatalogUri(dataNodeInfo.getUrl());
        }
        return icebergInfo;
    }

    private void createTable(SinkInfo sinkInfo) {
        LOGGER.info("begin to create iceberg table for sinkInfo={}", sinkInfo);

        // Get all info from config
        IcebergSinkDTO icebergInfo = getIcebergInfo(sinkInfo);
        List<IcebergColumnInfo> columnInfoList = getColumnList(sinkInfo);
        if (CollectionUtils.isEmpty(columnInfoList)) {
            throw new IllegalArgumentException("no iceberg columns specified");
        }
        IcebergTableInfo tableInfo = IcebergSinkDTO.getIcebergTableInfo(icebergInfo, columnInfoList);

        String metastoreUri = icebergInfo.getCatalogUri();
        String warehouse = icebergInfo.getWarehouse();
        String dbName = icebergInfo.getDbName();
        String tableName = icebergInfo.getTableName();

        try {
            // 1. create database if not exists
            IcebergCatalogUtils.createDb(metastoreUri, warehouse, dbName);

            // 2. check if the table exists
            boolean tableExists = IcebergCatalogUtils.tableExists(metastoreUri, dbName, tableName);

            if (!tableExists) {
                // 3. create table
                IcebergCatalogUtils.createTable(metastoreUri, warehouse, tableInfo);
            } else {
                // 4. or update table columns
                List<IcebergColumnInfo> existColumns = IcebergCatalogUtils.getColumns(metastoreUri, dbName, tableName);
                List<IcebergColumnInfo> needAddColumns = tableInfo.getColumns().stream().skip(existColumns.size())
                        .collect(toList());
                if (CollectionUtils.isNotEmpty(needAddColumns)) {
                    IcebergCatalogUtils.addColumns(metastoreUri, dbName, tableName, needAddColumns);
                    LOGGER.info("{} columns added for table {}", needAddColumns.size(), tableName);
                }
            }
            String info = "success to create iceberg resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOGGER.info(info + " for sinkInfo = {}", info);
        } catch (Throwable e) {
            String errMsg = "create iceberg table failed: " + e.getMessage();
            LOGGER.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }
    }

    private List<IcebergColumnInfo> getColumnList(SinkInfo sinkInfo) {
        List<StreamSinkFieldEntity> fieldList = sinkFieldMapper.selectBySinkId(sinkInfo.getId());

        // set columns
        List<IcebergColumnInfo> columnList = new ArrayList<>();
        for (StreamSinkFieldEntity field : fieldList) {
            IcebergColumnInfo column = IcebergColumnInfo.getFromJson(field.getExtParams());
            column.setName(field.getFieldName());
            column.setType(field.getFieldType());
            column.setDesc(field.getFieldComment());
            column.setRequired(field.getIsRequired() != null && field.getIsRequired() > 0);
            columnList.add(column);
        }

        return columnList;
    }
}
