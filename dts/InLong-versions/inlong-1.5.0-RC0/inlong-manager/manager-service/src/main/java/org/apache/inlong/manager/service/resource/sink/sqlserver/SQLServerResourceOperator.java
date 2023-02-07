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

package org.apache.inlong.manager.service.resource.sink.sqlserver;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerColumnInfo;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerSinkDTO;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerTableInfo;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperator;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

/**
 * SQLServer's resource operator.
 */
@Service
public class SQLServerResourceOperator implements SinkResourceOperator {

    private static final Logger LOG = LoggerFactory.getLogger(SQLServerResourceOperator.class);

    @Autowired
    private StreamSinkService sinkService;

    @Autowired
    private StreamSinkFieldEntityMapper fieldEntityMapper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.SQLSERVER.equals(sinkType);
    }

    @Override
    public void createSinkResource(SinkInfo sinkInfo) {
        LOG.info("begin to create SQLServer resources sinkId={}", sinkInfo.getId());
        if (SinkStatus.CONFIG_SUCCESSFUL.getCode().equals(sinkInfo.getStatus())) {
            LOG.warn("SQLServer resource [" + sinkInfo.getId() + "] already success, skip to create");
            return;
        } else if (InlongConstants.DISABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource())) {
            LOG.warn("create resource was disabled, skip to create for [" + sinkInfo.getId() + "]");
            return;
        }
        this.createTable(sinkInfo);
    }

    /**
     * Create SQLServer table by SinkInfo.
     *
     * @param sinkInfo {@link SinkInfo}
     */
    private void createTable(SinkInfo sinkInfo) {
        LOG.info("begin to create SQLServer table for sinkId={}", sinkInfo.getId());
        List<StreamSinkFieldEntity> fieldList = fieldEntityMapper.selectBySinkId(sinkInfo.getId());
        if (CollectionUtils.isEmpty(fieldList)) {
            LOG.warn("no SQLServer fields found, skip to create table for sinkId={}", sinkInfo.getId());
        }
        // set columns
        List<SQLServerColumnInfo> columnList = Lists.newArrayList();
        fieldList.forEach(field -> {
            SQLServerColumnInfo columnInfo = new SQLServerColumnInfo(
                    field.getFieldName(),
                    field.getFieldType(),
                    field.getFieldComment());
            columnList.add(columnInfo);
        });

        final SQLServerSinkDTO sink = SQLServerSinkDTO.getFromJson(sinkInfo.getExtParams());
        final SQLServerTableInfo tableInfo = SQLServerSinkDTO.getTableInfo(sink, columnList);

        try (Connection conn = SQLServerJdbcUtils.getConnection(sink.getJdbcUrl(),
                sink.getUsername(), sink.getPassword())) {

            // 1. create schema if not exists
            SQLServerJdbcUtils.createSchema(conn, tableInfo.getSchemaName());
            // 2. if table not exists, create it
            SQLServerJdbcUtils.createTable(conn, tableInfo);
            // 3. if table exists, add columns - skip the exists columns
            SQLServerJdbcUtils.addColumns(conn, tableInfo.getSchemaName(), tableInfo.getTableName(), columnList);
            // 4. update the sink status to success
            final String info = "success to create SQLServer resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOG.info(info + " for sinkInfo={}", sinkInfo);
        } catch (Throwable e) {
            String errMsg = "create SQLServer table failed: " + e.getMessage();
            LOG.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }
        LOG.info("success create SQLServer table for data sink [" + sinkInfo.getId() + "]");
    }
}
