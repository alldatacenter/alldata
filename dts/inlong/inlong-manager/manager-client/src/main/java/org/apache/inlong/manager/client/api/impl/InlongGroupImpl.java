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

package org.apache.inlong.manager.client.api.impl;

import com.google.common.base.Objects;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.api.InlongGroupContext;
import org.apache.inlong.manager.client.api.InlongStream;
import org.apache.inlong.manager.client.api.InlongStreamBuilder;
import org.apache.inlong.manager.client.api.inner.InnerGroupContext;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.inner.client.InlongGroupClient;
import org.apache.inlong.manager.client.api.inner.client.InlongStreamClient;
import org.apache.inlong.manager.client.api.inner.client.WorkflowClient;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.client.api.util.InlongGroupTransfer;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.enums.SimpleGroupStatus;
import org.apache.inlong.manager.common.enums.SortStatus;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.group.InlongGroupCountResponse;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupResetRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.sort.BaseSortConf;
import org.apache.inlong.manager.pojo.sort.SortStatusInfo;
import org.apache.inlong.manager.pojo.sort.SortStatusRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Inlong group service implementation.
 */
public class InlongGroupImpl implements InlongGroup {

    public static final String GROUP_FIELD = "groupInfo";
    public static final String MQ_FIELD_OLD = "middlewareType";
    public static final String MQ_FIELD = "mqType";

    private final InnerGroupContext groupContext;
    private final InlongGroupClient groupClient;
    private final WorkflowClient workflowClient;
    private final InlongStreamClient streamClient;
    private final ClientConfiguration configuration;
    private InlongGroupInfo groupInfo;

    public InlongGroupImpl(InlongGroupInfo groupInfo, ClientConfiguration configuration) {
        this.groupInfo = groupInfo;
        this.groupContext = new InnerGroupContext();
        this.configuration = configuration;

        ClientFactory clientFactory = ClientUtils.getClientFactory(configuration);
        this.streamClient = clientFactory.getStreamClient();
        this.groupClient = clientFactory.getGroupClient();
        this.workflowClient = clientFactory.getWorkflowClient();

        String groupId = groupInfo.getInlongGroupId();
        InlongGroupInfo existGroupInfo = groupClient.getGroupInfo(groupId);
        if (existGroupInfo == null) {
            InlongGroupTransfer.createGroupInfo(groupInfo, groupInfo.getSortConf());
            groupId = groupClient.createGroup(groupInfo.genRequest());
            groupInfo.setInlongGroupId(groupId);
        }

        // after creating, change the group version
        InlongGroupInfo newGroupInfo = groupClient.getGroupInfo(groupId);
        groupInfo.setVersion(newGroupInfo.getVersion());
        this.groupContext.setGroupInfo(groupInfo);
    }

    @Override
    public InlongStreamBuilder createStream(InlongStreamInfo streamInfo) {
        return new DefaultInlongStreamBuilder(streamInfo, this.groupContext, configuration);
    }

    @Override
    public InlongGroupContext context() throws Exception {
        return generateSnapshot();
    }

    public InlongGroupContext context(String credentials) throws Exception {
        return generateSnapshot(credentials);
    }

    @Override
    public InlongGroupContext init() throws Exception {
        InlongGroupInfo groupInfo = this.groupContext.getGroupInfo();
        WorkflowResult initWorkflowResult = groupClient.initInlongGroup(groupInfo.genRequest());
        List<TaskResponse> taskViews = initWorkflowResult.getNewTasks();
        Preconditions.expectNotEmpty(taskViews, "init inlong group info failed");
        TaskResponse taskView = taskViews.get(0);
        final int taskId = taskView.getId();
        ProcessResponse processView = initWorkflowResult.getProcessInfo();
        Preconditions.expectTrue(ProcessStatus.PROCESSING == processView.getStatus(),
                String.format("process status %s is not corrected, should be PROCESSING", processView.getStatus()));

        // init must be ApplyGroupProcessForm
        // compile with old cluster
        JSONObject formDataJson = JsonUtils.parseObject(
                JsonUtils.toJsonString(JsonUtils.toJsonString(processView.getFormData())),
                JSONObject.class);
        assert formDataJson != null;
        if (formDataJson.has(GROUP_FIELD)) {
            JSONObject groupInfoJson = formDataJson.getJSONObject(GROUP_FIELD);
            if (groupInfoJson.has(MQ_FIELD_OLD) && !groupInfoJson.has(MQ_FIELD)) {
                groupInfoJson.put(MQ_FIELD, groupInfoJson.get(MQ_FIELD_OLD));
            }
        }
        String formDataNew = formDataJson.toString();
        ApplyGroupProcessForm groupProcessForm = JsonUtils.parseObject(
                formDataNew, ApplyGroupProcessForm.class);
        Preconditions.expectNotNull(groupProcessForm, "ApplyGroupProcessForm cannot be null");
        groupContext.setInitMsg(groupProcessForm);
        assert groupProcessForm != null;
        WorkflowResult startWorkflowResult = workflowClient.startInlongGroup(taskId, groupProcessForm);
        processView = startWorkflowResult.getProcessInfo();
        Preconditions.expectTrue(ProcessStatus.COMPLETED == processView.getStatus(),
                String.format("inlong group status %s is incorrect, should be COMPLETED", processView.getStatus()));
        return generateSnapshot();
    }

    @Override
    public void update(InlongGroupInfo originGroupInfo, BaseSortConf sortConf) throws Exception {
        if (originGroupInfo == null) {
            originGroupInfo = this.groupInfo;
        }

        Preconditions.expectTrue(Objects.equal(originGroupInfo.getInlongGroupId(), this.groupInfo.getInlongGroupId()),
                "groupId must be same");

        InlongGroupInfo groupInfo = InlongGroupTransfer.createGroupInfo(originGroupInfo, sortConf);
        this.updateOpt(groupInfo);
        this.groupInfo = this.groupContext.getGroupInfo();
    }

    @Override
    public void update(BaseSortConf sortConf) throws Exception {
        Preconditions.expectNotNull(sortConf, "sort conf cannot be null");
        this.updateOpt(InlongGroupTransfer.createGroupInfo(this.groupInfo, sortConf));
    }

    private void updateOpt(InlongGroupInfo groupInfo) {
        InlongGroupInfo existGroupInfo = groupClient.getGroupInfo(groupInfo.getInlongGroupId());
        Preconditions.expectNotNull(existGroupInfo, "inlong group does not exist, cannot be updated");
        SimpleGroupStatus status = SimpleGroupStatus.parseStatusByCode(existGroupInfo.getStatus());
        Preconditions.expectTrue(status != SimpleGroupStatus.INITIALIZING,
                "inlong group is in init status, cannot be updated");

        groupInfo.setVersion(existGroupInfo.getVersion());
        Pair<String, String> idAndErr = groupClient.updateGroup(groupInfo.genRequest());
        String errMsg = idAndErr.getValue();
        Preconditions.expectNull(errMsg, errMsg);

        this.groupContext.setGroupInfo(groupInfo);
    }

    @Override
    public InlongGroupContext reInitOnUpdate(InlongGroupInfo originGroupInfo, BaseSortConf sortConf) throws Exception {
        this.update(originGroupInfo, sortConf);
        String groupId = this.groupContext.getGroupInfo().getInlongGroupId();
        InlongGroupInfo newGroupInfo = groupClient.getGroupInfo(groupId);
        if (newGroupInfo == null) {
            throw new RuntimeException(String.format("Group not found by inlongGroupId=%s", groupId));
        }

        this.groupContext.setGroupInfo(newGroupInfo);
        return init();
    }

    @Override
    public InlongGroupContext suspend() {
        return suspend(false);
    }

    @Override
    public InlongGroupContext suspend(boolean async) {
        InlongGroupInfo groupInfo = groupContext.getGroupInfo();
        this.updateOpt(groupInfo);

        groupClient.operateInlongGroup(groupInfo.getInlongGroupId(), SimpleGroupStatus.STOPPED, async);
        return generateSnapshot();
    }

    @Override
    public InlongGroupContext restart() {
        return restart(false);
    }

    @Override
    public InlongGroupContext restart(boolean async) {
        InlongGroupInfo groupInfo = groupContext.getGroupInfo();
        this.updateOpt(groupInfo);

        groupClient.operateInlongGroup(groupInfo.getInlongGroupId(), SimpleGroupStatus.STARTED, async);
        return generateSnapshot();
    }

    @Override
    public InlongGroupContext delete() throws Exception {
        return delete(false);
    }

    @Override
    public InlongGroupContext delete(boolean async) throws Exception {
        InlongGroupInfo groupInfo = groupClient.getGroupInfo(groupContext.getGroupId());
        GroupStatus status = GroupStatus.forCode(groupInfo.getStatus());
        if (status == GroupStatus.FINISH) {
            groupClient.deleteInlongGroup(groupInfo.getInlongGroupId());
            return generateSnapshot();
        }
        boolean isDeleted = groupClient.deleteInlongGroup(groupInfo.getInlongGroupId(), async);
        if (isDeleted) {
            groupInfo.setStatus(GroupStatus.DELETED.getCode());
        }
        return generateSnapshot();
    }

    @Override
    public List<InlongStream> listStreams() {
        String inlongGroupId = this.groupContext.getGroupId();
        return fetchInlongStreams(inlongGroupId);
    }

    @Override
    public InlongGroupContext reset(int rerun, int resetFinalStatus) {
        InlongGroupInfo groupInfo = groupContext.getGroupInfo();
        InlongGroupResetRequest request = new InlongGroupResetRequest(groupInfo.getInlongGroupId(),
                rerun, resetFinalStatus);
        groupClient.resetGroup(request);
        return generateSnapshot();
    }

    @Override
    public InlongGroupCountResponse countGroupByUser() {
        return groupClient.countGroupByUser();
    }

    @Override
    public InlongGroupTopicInfo getTopic(String id) {
        return groupClient.getTopic(id);
    }

    private InlongGroupContext generateSnapshot() {
        InlongGroupInfo groupInfo = groupClient.getGroupInfo(groupContext.getGroupId());
        // if current group is not exists, set its status to deleted
        if (groupInfo == null) {
            groupInfo = groupContext.getGroupInfo();
            groupInfo.setStatus(GroupStatus.DELETED.getCode());
            return new InlongGroupContext(groupContext);
        }

        groupContext.setGroupInfo(groupInfo);
        String inlongGroupId = groupInfo.getInlongGroupId();
        // fetch stream in group
        List<InlongStream> dataStreams = fetchInlongStreams(inlongGroupId);
        if (CollectionUtils.isNotEmpty(dataStreams)) {
            dataStreams.forEach(groupContext::setStream);
        }

        return new InlongGroupContext(groupContext);
    }

    private InlongGroupContext generateSnapshot(String credentials) {
        InlongGroupContext groupContext = generateSnapshot();
        InlongGroupInfo groupInfo = groupContext.getGroupInfo();

        SortStatus sortStatus = SortStatus.NOT_EXISTS;
        if (groupInfo.getExtList().stream().anyMatch(ext -> InlongConstants.SORT_JOB_ID.equals(ext.getKeyName())
                && StringUtils.isNotEmpty(ext.getKeyValue()))) {
            SortStatusRequest request = new SortStatusRequest();
            final String groupId = groupInfo.getInlongGroupId();
            request.setInlongGroupIds(Collections.singletonList(groupId));
            request.setCredentials(credentials);
            List<SortStatusInfo> statusInfos = groupClient.listSortStatus(request);
            if (CollectionUtils.isNotEmpty(statusInfos)) {
                Optional<SortStatusInfo> optional = statusInfos.stream()
                        .filter(statusInfo -> groupId.equals(statusInfo.getInlongGroupId()))
                        .findFirst();
                if (optional.isPresent()) {
                    sortStatus = optional.get().getSortStatus();
                }
            }
        }
        groupContext.updateSortStatus(sortStatus);

        return groupContext;
    }

    private List<InlongStream> fetchInlongStreams(String groupId) {
        List<InlongStreamInfo> streamInfos = streamClient.listStreamInfo(groupId);
        if (CollectionUtils.isEmpty(streamInfos)) {
            return null;
        }
        return streamInfos.stream()
                .map(streamInfo -> new InlongStreamImpl(streamInfo, configuration))
                .collect(Collectors.toList());
    }
}
