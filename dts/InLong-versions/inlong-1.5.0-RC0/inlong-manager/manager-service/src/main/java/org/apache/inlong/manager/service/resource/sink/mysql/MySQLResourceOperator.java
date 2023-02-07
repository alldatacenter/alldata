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

package org.apache.inlong.manager.service.resource.sink.mysql;

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
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLColumnInfo;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSinkDTO;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLTableInfo;
import org.apache.inlong.manager.service.node.DataNodeOperateHelper;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperator;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL's resource operator.
 */
@Service
public class MySQLResourceOperator implements SinkResourceOperator {

    private static final Logger LOG = LoggerFactory.getLogger(MySQLResourceOperator.class);

    @Autowired
    private StreamSinkService sinkService;

    @Autowired
    private StreamSinkFieldEntityMapper fieldEntityMapper;

    @Autowired
    private DataNodeOperateHelper dataNodeHelper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.MYSQL.equals(sinkType);
    }

    @Override
    public void createSinkResource(SinkInfo sinkInfo) {
        LOG.info("begin to create MySQL resources sinkId={}", sinkInfo.getId());
        if (SinkStatus.CONFIG_SUCCESSFUL.getCode().equals(sinkInfo.getStatus())) {
            LOG.warn("MySQL resource [" + sinkInfo.getId() + "] already success, skip to create");
            return;
        } else if (InlongConstants.DISABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource())) {
            LOG.warn("create resource was disabled, skip to create for [" + sinkInfo.getId() + "]");
            return;
        }
        this.createTable(sinkInfo);
    }

    /**
     * Create MySQL table by SinkInfo.
     *
     * @param sinkInfo {@link SinkInfo}
     */
    private void createTable(SinkInfo sinkInfo) {
        LOG.info("begin to create MySQL table for sinkId={}", sinkInfo.getId());
        List<StreamSinkFieldEntity> fieldList = fieldEntityMapper.selectBySinkId(sinkInfo.getId());
        if (CollectionUtils.isEmpty(fieldList)) {
            LOG.warn("no MySQL fields found, skip to create table for sinkId={}", sinkInfo.getId());
        }
        // set columns
        List<MySQLColumnInfo> columnList = new ArrayList<>();
        for (StreamSinkFieldEntity field : fieldList) {
            MySQLColumnInfo columnInfo = new MySQLColumnInfo(field.getFieldName(), field.getFieldType(),
                    field.getFieldComment());
            columnList.add(columnInfo);
        }

        MySQLSinkDTO sinkDTO = this.getMysqlInfo(sinkInfo);
        MySQLTableInfo tableInfo = MySQLSinkDTO.getTableInfo(sinkDTO, columnList);
        try (Connection conn = MySQLJdbcUtils.getConnection(sinkDTO.getJdbcUrl(), sinkDTO.getUsername(),
                sinkDTO.getPassword())) {
            // 1. create database if not exists
            MySQLJdbcUtils.createDb(conn, tableInfo.getDbName());
            // 2. table not exists, create it
            MySQLJdbcUtils.createTable(conn, tableInfo);
            // 3. table exists, add columns - skip the exists columns
            MySQLJdbcUtils.addColumns(conn, tableInfo.getDbName(), tableInfo.getTableName(), columnList);

            // 4. update the sink status to success
            String info = "success to create MySQL resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOG.info(info + " for sinkInfo={}", sinkInfo);
        } catch (Throwable e) {
            String errMsg = "create MySQL table failed: " + e.getMessage();
            LOG.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }
        LOG.info("success create MySQL table for data sink [" + sinkInfo.getId() + "]");
    }

    private MySQLSinkDTO getMysqlInfo(SinkInfo sinkInfo) {
        MySQLSinkDTO mysqlInfo = MySQLSinkDTO.getFromJson(sinkInfo.getExtParams());

        if (StringUtils.isBlank(mysqlInfo.getJdbcUrl())) {
            String dataNodeName = sinkInfo.getDataNodeName();
            Preconditions.checkNotEmpty(dataNodeName, "mysql jdbc url not specified and data node is empty");
            DataNodeInfo dataNodeInfo = dataNodeHelper.getDataNodeInfo(dataNodeName, sinkInfo.getSinkType());
            CommonBeanUtils.copyProperties(dataNodeInfo, mysqlInfo);
            mysqlInfo.setJdbcUrl(dataNodeInfo.getUrl());
            mysqlInfo.setPassword(dataNodeInfo.getToken());
        }
        return mysqlInfo;
    }

}