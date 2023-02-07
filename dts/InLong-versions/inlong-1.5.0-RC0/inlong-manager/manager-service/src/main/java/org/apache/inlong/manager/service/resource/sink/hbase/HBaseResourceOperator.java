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

package org.apache.inlong.manager.service.resource.sink.hbase;

import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.hbase.HBaseColumnFamilyInfo;
import org.apache.inlong.manager.pojo.sink.hbase.HBaseSinkDTO;
import org.apache.inlong.manager.pojo.sink.hbase.HBaseTableInfo;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperator;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * HBase's resource operator
 */
@Service
public class HBaseResourceOperator implements SinkResourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HBaseResourceOperator.class);

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private StreamSinkFieldEntityMapper sinkFieldMapper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.HBASE.equals(sinkType);
    }

    /**
     * Create hbase table according to the sink config
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

    private void createTable(SinkInfo sinkInfo) {
        LOGGER.info("begin to create hbase table for sinkInfo={}", sinkInfo);

        // Get all info from config
        HBaseSinkDTO hbaseInfo = HBaseSinkDTO.getFromJson(sinkInfo.getExtParams());
        List<HBaseColumnFamilyInfo> columnFamilies = getColumnFamilies(sinkInfo);
        if (CollectionUtils.isEmpty(columnFamilies)) {
            throw new IllegalArgumentException("no hbase column families specified");
        }
        HBaseTableInfo tableInfo = HBaseSinkDTO.getHbaseTableInfo(hbaseInfo, columnFamilies);

        String zkAddress = hbaseInfo.getZkQuorum();
        String zkNode = hbaseInfo.getZkNodeParent();
        String namespace = hbaseInfo.getNamespace();
        String tableName = hbaseInfo.getTableName();

        try {
            // 1. create database if not exists
            HBaseApiUtils.createNamespace(zkAddress, zkNode, namespace);

            // 2. check if the table exists
            boolean tableExists = HBaseApiUtils.tableExists(zkAddress, zkNode, namespace, tableName);

            if (!tableExists) {
                // 3. create table
                HBaseApiUtils.createTable(zkAddress, zkNode, tableInfo);
            } else {
                // 4. or update table columns
                List<HBaseColumnFamilyInfo> existColumnFamilies = HBaseApiUtils.getColumnFamilies(zkAddress, zkNode,
                        namespace, tableName).stream()
                        .sorted(Comparator.comparing(HBaseColumnFamilyInfo::getCfName)).collect(toList());
                List<HBaseColumnFamilyInfo> requestColumnFamilies = tableInfo.getColumnFamilies().stream()
                        .sorted(Comparator.comparing(HBaseColumnFamilyInfo::getCfName)).collect(toList());
                List<HBaseColumnFamilyInfo> newColumnFamilies = requestColumnFamilies.stream()
                        .skip(existColumnFamilies.size()).collect(toList());

                if (CollectionUtils.isNotEmpty(newColumnFamilies)) {
                    HBaseApiUtils.addColumnFamilies(zkAddress, zkNode, namespace, tableName, newColumnFamilies);
                    LOGGER.info("{} column families added for table {}", newColumnFamilies.size(), tableName);
                }
            }
            String info = "success to create hbase resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOGGER.info(info + " for sinkInfo = {}", info);
        } catch (Throwable e) {
            String errMsg = "create hbase table failed: " + e.getMessage();
            LOGGER.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }
    }

    private List<HBaseColumnFamilyInfo> getColumnFamilies(SinkInfo sinkInfo) {
        List<StreamSinkFieldEntity> fieldList = sinkFieldMapper.selectBySinkId(sinkInfo.getId());
        Set<String> seen = new HashSet<>();

        List<HBaseColumnFamilyInfo> columnFamilies = new ArrayList<>();
        for (StreamSinkFieldEntity field : fieldList) {
            HBaseColumnFamilyInfo columnFamily = HBaseColumnFamilyInfo.getFromJson(field.getExtParams());
            if (seen.contains(columnFamily.getCfName())) {
                continue;
            }
            seen.add(columnFamily.getCfName());
            columnFamilies.add(columnFamily);
        }

        return columnFamilies;
    }
}
