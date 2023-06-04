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

package org.apache.inlong.manager.service.resource.sink.starrocks;

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
import org.apache.inlong.manager.pojo.node.starrocks.StarRocksDataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksColumnInfo;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksSinkDTO;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksTableInfo;
import org.apache.inlong.manager.service.node.DataNodeOperateHelper;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperator;
import org.apache.inlong.manager.service.resource.sink.mysql.MySQLResourceOperator;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * StarRocks resource operator.
 */
@Service
public class StarRocksResourceOperator implements SinkResourceOperator {

    private static final Logger LOG = LoggerFactory.getLogger(MySQLResourceOperator.class);

    @Autowired
    private StreamSinkService sinkService;

    @Autowired
    private StreamSinkFieldEntityMapper fieldEntityMapper;

    @Autowired
    private DataNodeOperateHelper dataNodeHelper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.STARROCKS.equals(sinkType);
    }

    @Override
    public void createSinkResource(SinkInfo sinkInfo) {
        LOG.info("begin to create sink resources sinkId={}", sinkInfo.getId());
        if (SinkStatus.CONFIG_SUCCESSFUL.getCode().equals(sinkInfo.getStatus())) {
            LOG.warn("sink resource [" + sinkInfo.getId() + "] already success, skip to create");
            return;
        } else if (InlongConstants.DISABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource())) {
            LOG.warn("create resource was disabled, skip to create for [" + sinkInfo.getId() + "]");
            return;
        }
        this.createTable(sinkInfo);
    }

    /**
     * Create starRocks table by SinkInfo.
     *
     * @param sinkInfo {@link SinkInfo}
     */
    private void createTable(SinkInfo sinkInfo) {
        LOG.info("begin to create starRocks table for sinkId={}", sinkInfo.getId());
        List<StreamSinkFieldEntity> fieldList = fieldEntityMapper.selectBySinkId(sinkInfo.getId());
        if (CollectionUtils.isEmpty(fieldList)) {
            LOG.warn("no starRocks fields found, skip to create table for sinkId={}", sinkInfo.getId());
        }
        // get columns
        List<StarRocksColumnInfo> columnList = getStarRocksColumnInfoFromSink(fieldList);

        StarRocksSinkDTO sinkDTO = getStarRocksInfo(sinkInfo);
        StarRocksTableInfo tableInfo = StarRocksSinkDTO.getTableInfo(sinkDTO, columnList);
        String url = sinkDTO.getJdbcUrl();
        String username = sinkDTO.getUsername();
        String password = sinkDTO.getPassword();
        String dbName = sinkDTO.getDatabaseName();
        String tableName = sinkDTO.getTableName();
        try {
            // 1. create database if not exists
            StarRocksJdbcUtils.createDb(url, username, password, dbName);
            String dbUrl = url + "/" + dbName;
            // 2. table not exists, create it
            StarRocksJdbcUtils.createTable(dbUrl, username, password, tableInfo);
            // 3. table exists, add columns - skip the exists columns
            StarRocksJdbcUtils.addColumns(dbUrl, username, password, dbName, tableName, columnList);
            // 4. update the sink status to success
            String info = "success to create StarRocks resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOG.info(info + " for sinkInfo={}", sinkInfo);
        } catch (Throwable e) {
            String errMsg = "create StarRocks table failed: " + e.getMessage();
            LOG.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }
        LOG.info("success create StarRocks table for data sink [" + sinkInfo.getId() + "]");
    }

    public List<StarRocksColumnInfo> getStarRocksColumnInfoFromSink(List<StreamSinkFieldEntity> sinkList) {
        List<StarRocksColumnInfo> columnInfoList = new ArrayList<>();
        for (StreamSinkFieldEntity fieldEntity : sinkList) {
            if (StringUtils.isNotBlank(fieldEntity.getExtParams())) {
                StarRocksColumnInfo starRocksColumnInfo = StarRocksColumnInfo.getFromJson(
                        fieldEntity.getExtParams());
                CommonBeanUtils.copyProperties(fieldEntity, starRocksColumnInfo, true);
                columnInfoList.add(starRocksColumnInfo);
            } else {
                StarRocksColumnInfo starRocksColumnInfo = new StarRocksColumnInfo();
                CommonBeanUtils.copyProperties(fieldEntity, starRocksColumnInfo, true);
                columnInfoList.add(starRocksColumnInfo);
            }
        }
        return columnInfoList;
    }

    private StarRocksSinkDTO getStarRocksInfo(SinkInfo sinkInfo) {
        StarRocksSinkDTO starRocksInfo = StarRocksSinkDTO.getFromJson(sinkInfo.getExtParams());

        // read from data node if not supplied by user
        if (StringUtils.isBlank(starRocksInfo.getJdbcUrl())) {
            String dataNodeName = sinkInfo.getDataNodeName();
            Preconditions.expectNotBlank(dataNodeName, ErrorCodeEnum.INVALID_PARAMETER,
                    "starRocks jdbc url not specified and data node is empty");
            StarRocksDataNodeInfo dataNodeInfo = (StarRocksDataNodeInfo) dataNodeHelper.getDataNodeInfo(
                    dataNodeName, sinkInfo.getSinkType());
            CommonBeanUtils.copyProperties(dataNodeInfo, starRocksInfo);
            starRocksInfo.setJdbcUrl(dataNodeInfo.getUrl());
            starRocksInfo.setPassword(dataNodeInfo.getToken());
        }
        return starRocksInfo;
    }
}
