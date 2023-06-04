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

package org.apache.inlong.manager.service.resource.sink.kudu;

import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.kudu.KuduColumnInfo;
import org.apache.inlong.manager.pojo.sink.kudu.KuduSinkDTO;
import org.apache.inlong.manager.pojo.sink.kudu.KuduTableInfo;
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
 * Kudu resource operator
 */
@Service
public class KuduResourceOperator implements SinkResourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KuduResourceOperator.class);

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private StreamSinkFieldEntityMapper sinkFieldMapper;
    @Autowired
    private DataNodeOperateHelper dataNodeHelper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.KUDU.equals(sinkType);
    }

    /**
     * Create Kudu table according to the sink config
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

        this.createTableIfAbsent(sinkInfo);
    }

    private KuduSinkDTO getKuduInfo(SinkInfo sinkInfo) {
        return KuduSinkDTO.getFromJson(sinkInfo.getExtParams());
    }

    private void createTableIfAbsent(SinkInfo sinkInfo) {
        LOGGER.info("begin to create kudu table for sinkInfo={}", sinkInfo);

        // Get all info from config
        KuduSinkDTO kuduInfo = getKuduInfo(sinkInfo);
        List<KuduColumnInfo> columnInfoList = getColumnList(sinkInfo);
        if (CollectionUtils.isEmpty(columnInfoList)) {
            throw new IllegalArgumentException("no kudu columns specified");
        }
        KuduTableInfo tableInfo = KuduSinkDTO.getKuduTableInfo(kuduInfo, columnInfoList);

        String masters = kuduInfo.getMasters();
        String tableName = kuduInfo.getTableName();

        KuduResourceClient client = null;
        try {
            // 1. create client
            client = new KuduResourceClient(masters);

            // 2. check if the table exists
            boolean tableExists = client.tableExist(tableName);

            if (!tableExists) {
                // 3. create table
                client.createTable(tableName, tableInfo);
            } else {
                // 4. or update table columns
                List<KuduColumnInfo> existColumns = client.getColumns(tableName);
                Set<String> existColumnNameSet = existColumns.stream().map(SinkField::getFieldName)
                        .collect(Collectors.toSet());
                // Get columns need added according to column name.
                List<KuduColumnInfo> needAddColumns = tableInfo.getColumns().stream()
                        .filter(columnInfo -> !existColumnNameSet.contains(columnInfo.getFieldName()))
                        .collect(toList());
                if (CollectionUtils.isNotEmpty(needAddColumns)) {
                    client.addColumns(tableName, needAddColumns);
                    LOGGER.info("{} columns added for kudu table {}", needAddColumns.size(), tableName);
                }
            }
            String info = "success to create Kudu resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOGGER.info(info + " for sinkInfo = {}", info);
        } catch (Throwable e) {
            String errMsg = "create Kudu table failed: " + e.getMessage();
            LOGGER.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private List<KuduColumnInfo> getColumnList(SinkInfo sinkInfo) {
        List<StreamSinkFieldEntity> fieldList = sinkFieldMapper.selectBySinkId(sinkInfo.getId());

        // set columns
        List<KuduColumnInfo> columnList = new ArrayList<>();
        for (StreamSinkFieldEntity field : fieldList) {
            if (StringUtils.isNotBlank(field.getExtParams())) {
                KuduColumnInfo kuduColumnInfo = KuduColumnInfo.getFromJson(field.getExtParams());
                CommonBeanUtils.copyProperties(field, kuduColumnInfo, true);
                columnList.add(kuduColumnInfo);
            } else {
                KuduColumnInfo kuduColumnInfo = new KuduColumnInfo();
                CommonBeanUtils.copyProperties(field, kuduColumnInfo, true);
                columnList.add(kuduColumnInfo);
            }
        }

        return columnList;
    }
}
