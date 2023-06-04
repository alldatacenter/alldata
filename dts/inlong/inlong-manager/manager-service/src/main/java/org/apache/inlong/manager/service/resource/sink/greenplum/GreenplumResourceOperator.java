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

package org.apache.inlong.manager.service.resource.sink.greenplum;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumColumnInfo;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumSinkDTO;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumTableInfo;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperator;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.List;

public class GreenplumResourceOperator implements SinkResourceOperator {

    private static final Logger LOG = LoggerFactory.getLogger(GreenplumResourceOperator.class);

    public static final String GREENPLUM_DEFAULT_SCHEMA = "public";

    @Autowired
    private StreamSinkService sinkService;

    @Autowired
    private StreamSinkFieldEntityMapper fieldEntityMapper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.GREENPLUM.equals(sinkType);
    }

    @Override
    public void createSinkResource(SinkInfo sinkInfo) {
        LOG.info("begin to create Greenplum resources sinkId={}", sinkInfo.getId());
        if (SinkStatus.CONFIG_SUCCESSFUL.getCode().equals(sinkInfo.getStatus())) {
            LOG.warn("Greenplum resource [" + sinkInfo.getId() + "] already success, skip to create");
            return;
        } else if (InlongConstants.DISABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource())) {
            LOG.warn("create resource was disabled, skip to create for [" + sinkInfo.getId() + "]");
            return;
        }
        this.createTable(sinkInfo);
    }

    /**
     * Create Greenplum table by SinkInfo.
     *
     * @param sinkInfo {@link SinkInfo}
     */
    private void createTable(SinkInfo sinkInfo) {
        LOG.info("begin to create Greenplum table for sinkId={}", sinkInfo.getId());
        List<StreamSinkFieldEntity> fieldList = fieldEntityMapper.selectBySinkId(sinkInfo.getId());
        if (CollectionUtils.isEmpty(fieldList)) {
            LOG.warn("no Greenplum fields found, skip to create table for sinkId={}", sinkInfo.getId());
        }
        // set columns
        final List<GreenplumColumnInfo> columnList = Lists.newArrayList();
        fieldList.forEach(field -> {
            columnList.add(
                    new GreenplumColumnInfo(field.getFieldName(), field.getFieldType(), field.getFieldComment()));
        });

        GreenplumSinkDTO greenplumSink = GreenplumSinkDTO.getFromJson(sinkInfo.getExtParams());
        GreenplumTableInfo tableInfo = GreenplumSinkDTO.getTableInfo(greenplumSink, columnList);
        if (StringUtils.isEmpty(tableInfo.getSchemaName())) {
            tableInfo.setSchemaName(GREENPLUM_DEFAULT_SCHEMA);
        }
        try (Connection conn = GreenplumJdbcUtils.getConnection(greenplumSink.getJdbcUrl(), greenplumSink.getUsername(),
                greenplumSink.getPassword())) {
            // 1.If schema not exists,create it
            GreenplumJdbcUtils.createSchema(conn, tableInfo.getTableName(), tableInfo.getUserName());
            // 2.If table not exists, create it
            GreenplumJdbcUtils.createTable(conn, tableInfo);
            // 3.Table exists, add columns - skip the exists columns
            GreenplumJdbcUtils.addColumns(conn, tableInfo.getSchemaName(), tableInfo.getTableName(), columnList);
            // 4.Update the sink status to success
            final String info = "success to create Greenplum resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOG.info(info + " for sinkInfo={}", sinkInfo);
            // 4. close connection.
        } catch (Throwable e) {
            String errMsg = "create Greenplum table failed: " + e.getMessage();
            LOG.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }
        LOG.info("success create Greenplum table for data sink [" + sinkInfo.getId() + "]");
    }
}
