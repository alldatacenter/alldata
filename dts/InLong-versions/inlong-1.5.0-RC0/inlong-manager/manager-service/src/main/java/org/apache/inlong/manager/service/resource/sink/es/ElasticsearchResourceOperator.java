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

package org.apache.inlong.manager.service.resource.sink.es;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchFieldInfo;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSinkDTO;
import org.apache.inlong.manager.service.node.DataNodeOperateHelper;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperator;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch's resource operator
 */
@Service
public class ElasticsearchResourceOperator implements SinkResourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchResourceOperator.class);
    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private StreamSinkFieldEntityMapper sinkFieldMapper;
    @Autowired
    private DataNodeOperateHelper dataNodeHelper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.ELASTICSEARCH.equals(sinkType);
    }

    @Override
    public void createSinkResource(SinkInfo sinkInfo) {
        if (sinkInfo == null) {
            LOGGER.warn("sink info was null, skip to create es resource");
            return;
        }

        if (SinkStatus.CONFIG_SUCCESSFUL.getCode().equals(sinkInfo.getStatus())) {
            LOGGER.warn("sink resource [" + sinkInfo.getId() + "] already success, skip to create");
            return;
        } else if (InlongConstants.DISABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource())) {
            LOGGER.warn("create resource was disabled, skip to create for [" + sinkInfo.getId() + "]");
            return;
        }

        this.createIndex(sinkInfo);
    }

    private void createIndex(SinkInfo sinkInfo) {
        LOGGER.info("begin to create es index for sinkId={}", sinkInfo.getId());

        List<StreamSinkFieldEntity> sinkList = sinkFieldMapper.selectBySinkId(sinkInfo.getId());
        if (CollectionUtils.isEmpty(sinkList)) {
            LOGGER.warn("no es fields found, skip to create es index for sinkId={}", sinkInfo.getId());
        }

        // set fields
        List<ElasticsearchFieldInfo> fieldList = getElasticsearchFieldFromSink(sinkList);

        try {
            ElasticsearchApi client = new ElasticsearchApi();
            ElasticsearchSinkDTO esInfo = ElasticsearchSinkDTO.getFromJson(sinkInfo.getExtParams());
            client.setEsConfig(getElasticsearchConfig(sinkInfo, esInfo));
            String indexName = esInfo.getIndexName();
            boolean indexExists = client.indexExists(indexName);

            // 3. index not exists, create it
            if (!indexExists) {
                client.createIndexAndMapping(indexName, fieldList);
            } else {
                // 4. index exists, add fields - skip the exists fields
                client.addNotExistFields(indexName, fieldList);
            }

            // 5. update the sink status to success
            String info = "success to create es resource";
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_SUCCESSFUL.getCode(), info);
            LOGGER.info(info + " for sinkInfo={}", sinkInfo);
        } catch (Throwable e) {
            String errMsg = "Create Elasticsearch index failed: " + e.getMessage();
            LOGGER.error(errMsg, e);
            sinkService.updateStatus(sinkInfo.getId(), SinkStatus.CONFIG_FAILED.getCode(), errMsg);
            throw new WorkflowException(errMsg);
        }
    }

    public List<ElasticsearchFieldInfo> getElasticsearchFieldFromSink(List<StreamSinkFieldEntity> sinkList) {
        List<ElasticsearchFieldInfo> esFieldList = new ArrayList<>();
        for (StreamSinkFieldEntity fieldEntity : sinkList) {
            if (StringUtils.isNotBlank(fieldEntity.getExtParams())) {
                ElasticsearchFieldInfo elasticsearchFieldInfo = ElasticsearchFieldInfo.getFromJson(
                        fieldEntity.getExtParams());
                CommonBeanUtils.copyProperties(fieldEntity, elasticsearchFieldInfo, true);
                esFieldList.add(elasticsearchFieldInfo);
            } else {
                ElasticsearchFieldInfo esFieldInfo = new ElasticsearchFieldInfo();
                CommonBeanUtils.copyProperties(fieldEntity, esFieldInfo, true);
                esFieldList.add(esFieldInfo);
            }
        }
        return esFieldList;
    }

    private ElasticsearchConfig getElasticsearchConfig(SinkInfo sinkInfo, ElasticsearchSinkDTO esInfo) {
        ElasticsearchConfig config = new ElasticsearchConfig();
        if (StringUtils.isNotEmpty(esInfo.getUsername())) {
            config.setAuthEnable(true);
            config.setUsername(esInfo.getUsername());
            config.setPassword(esInfo.getPassword());
        }
        config.setHosts(esInfo.getHosts());
        return config;
    }

}
