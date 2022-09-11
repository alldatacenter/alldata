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

package org.apache.inlong.manager.service.source.listener;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.source.SourceRequest;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.source.kafka.KafkaSource;
import org.apache.inlong.manager.common.pojo.source.kafka.KafkaSourceRequest;
import org.apache.inlong.manager.common.pojo.source.mysql.MySQLBinlogSource;
import org.apache.inlong.manager.common.pojo.source.mysql.MySQLBinlogSourceRequest;
import org.apache.inlong.manager.common.pojo.stream.StreamBriefResponse;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.DataSourceOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Event listener of operate resources, such as delete, stop, restart sources.
 */
@Slf4j
@Component
public abstract class AbstractSourceOperateListener implements DataSourceOperateListener {

    @Autowired
    protected InlongStreamService streamService;

    @Autowired
    protected StreamSourceService streamSourceService;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        log.info("operate stream source for context={}", context);
        InlongGroupInfo groupInfo = getGroupInfo(context.getProcessForm());
        final String groupId = groupInfo.getInlongGroupId();
        List<StreamBriefResponse> streamResponses = streamService.getBriefList(groupId);
        List<StreamSource> unOperatedSources = Lists.newArrayList();
        streamResponses.forEach(stream ->
                operateStreamSources(groupId, stream.getInlongStreamId(), context.getOperator(), unOperatedSources));
        if (CollectionUtils.isNotEmpty(unOperatedSources)) {
            GroupOperateType groupOperateType = getOperateType(context.getProcessForm());
            StringBuilder builder = new StringBuilder("Unsupported operate ").append(groupOperateType).append(" for (");
            unOperatedSources.forEach(source -> builder.append(" ").append(source.getSourceName()).append(" "));
            String errMsg = builder.append(")").toString();
            throw new WorkflowListenerException(errMsg);
        } else {
            return ListenerResult.success();
        }
    }

    /**
     * Operate stream sources, such as delete, stop, restart.
     */
    protected void operateStreamSources(String groupId, String streamId, String operator,
            List<StreamSource> unOperatedSources) {
        List<StreamSource> sources = streamSourceService.listSource(groupId, streamId);
        sources.forEach(source -> {
            boolean checkIfOp = checkIfOp(source, unOperatedSources);
            if (checkIfOp) {
                SourceRequest sourceRequest = createSourceRequest(source);
                operateStreamSource(sourceRequest, operator);
            }
        });
    }

    /**
     * Check source status.
     */
    @SneakyThrows
    public boolean checkIfOp(StreamSource streamSource, List<StreamSource> unOperatedSources) {
        for (int retry = 0; retry < 60; retry++) {
            int status = streamSource.getStatus();
            SourceStatus sourceStatus = SourceStatus.forCode(status);
            if (sourceStatus == SourceStatus.SOURCE_NORMAL || sourceStatus == SourceStatus.SOURCE_FROZEN) {
                return true;
            } else if (sourceStatus == SourceStatus.SOURCE_FAILED || sourceStatus == SourceStatus.SOURCE_DISABLE) {
                return false;
            } else {
                log.warn("stream source={} cannot be operated for status={}", streamSource, sourceStatus);
                TimeUnit.SECONDS.sleep(5);
                streamSource = streamSourceService.get(streamSource.getId());
            }
        }
        SourceStatus sourceStatus = SourceStatus.forCode(streamSource.getStatus());
        if (sourceStatus != SourceStatus.SOURCE_NORMAL
                && sourceStatus != SourceStatus.SOURCE_FROZEN
                && sourceStatus != SourceStatus.SOURCE_DISABLE
                && sourceStatus != SourceStatus.SOURCE_FAILED) {
            log.error("stream source ={} cannot be operated for status={}", streamSource, sourceStatus);
            unOperatedSources.add(streamSource);
        }
        return false;
    }

    /**
     * Creat source request by source type.
     *
     * @param streamSource source information
     * @return source request
     */
    public SourceRequest createSourceRequest(StreamSource streamSource) {
        String sourceType = streamSource.getSourceType();
        SourceType type = SourceType.valueOf(sourceType);
        switch (type) {
            case BINLOG:
                return CommonBeanUtils.copyProperties((MySQLBinlogSource) streamSource, MySQLBinlogSourceRequest::new);
            case KAFKA:
                return CommonBeanUtils.copyProperties((KafkaSource) streamSource, KafkaSourceRequest::new);
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported type=%s for DataSourceOperateListener", type));
        }
    }

    /**
     * Operate stream sources ,such as delete, stop, restart.
     */
    public abstract void operateStreamSource(SourceRequest sourceRequest, String operator);

    private GroupOperateType getOperateType(ProcessForm processForm) {
        if (processForm instanceof GroupResourceProcessForm) {
            return ((GroupResourceProcessForm) processForm).getGroupOperateType();
        } else {
            log.error("illegal process form {} to get inlong group info", processForm.getFormName());
            throw new RuntimeException(String.format("Unsupported ProcessForm {%s} in CreateSortConfigListener",
                    processForm.getFormName()));
        }
    }

    private InlongGroupInfo getGroupInfo(ProcessForm processForm) {
        if (processForm instanceof GroupResourceProcessForm) {
            GroupResourceProcessForm groupResourceProcessForm = (GroupResourceProcessForm) processForm;
            return groupResourceProcessForm.getGroupInfo();
        } else {
            log.error("illegal process form {} to get inlong group info", processForm.getFormName());
            throw new RuntimeException(String.format("Unsupported ProcessForm {%s} in CreateSortConfigListener",
                    processForm.getFormName()));
        }
    }

    @Override
    public boolean async() {
        return false;
    }

}
