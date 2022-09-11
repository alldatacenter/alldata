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

package org.apache.inlong.manager.plugin.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.common.settings.InlongGroupSettings;
import org.apache.inlong.manager.plugin.flink.FlinkOperation;
import org.apache.inlong.manager.plugin.flink.FlinkService;
import org.apache.inlong.manager.plugin.flink.dto.FlinkInfo;
import org.apache.inlong.manager.plugin.flink.enums.Constants;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.inlong.manager.plugin.util.FlinkUtils.getExceptionStackMsg;

/**
 * Listener of startup sort.
 */
@Slf4j
public class StartupSortListener implements SortOperateListener {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        ProcessForm processForm = context.getProcessForm();
        String groupId = processForm.getInlongGroupId();
        if (!(processForm instanceof GroupResourceProcessForm)) {
            String message = String.format("process form was not GroupResource for groupId [%s]", groupId);
            log.error(message);
            return ListenerResult.fail(message);
        }

        GroupResourceProcessForm groupResourceForm = (GroupResourceProcessForm) processForm;
        InlongGroupInfo inlongGroupInfo = groupResourceForm.getGroupInfo();
        List<InlongGroupExtInfo> extList = inlongGroupInfo.getExtList();
        log.info("inlong group ext info: {}", extList);

        Map<String, String> kvConf = extList.stream().filter(v -> StringUtils.isNotEmpty(v.getKeyName())
                && StringUtils.isNotEmpty(v.getKeyValue())).collect(Collectors.toMap(
                InlongGroupExtInfo::getKeyName,
                InlongGroupExtInfo::getKeyValue));
        String sortExt = kvConf.get(InlongGroupSettings.SORT_PROPERTIES);
        if (StringUtils.isNotEmpty(sortExt)) {
            Map<String, String> result = OBJECT_MAPPER.convertValue(OBJECT_MAPPER.readTree(sortExt),
                    new TypeReference<Map<String, String>>() {
                    });
            kvConf.putAll(result);
        }

        String dataFlows = kvConf.get(InlongGroupSettings.DATA_FLOW);
        if (StringUtils.isEmpty(dataFlows)) {
            String message = String.format("dataflow is empty for groupId [%s]", groupId);
            log.error(message);
            return ListenerResult.fail(message);
        }

        FlinkInfo flinkInfo = new FlinkInfo();
        String jobName = Constants.INLONG + context.getProcessForm().getInlongGroupId();
        flinkInfo.setJobName(jobName);
        String sortUrl = kvConf.get(InlongGroupSettings.SORT_URL);
        flinkInfo.setEndpoint(sortUrl);
        flinkInfo.setInlongStreamInfoList(groupResourceForm.getStreamInfos());

        FlinkService flinkService = new FlinkService(flinkInfo.getEndpoint());
        FlinkOperation flinkOperation = new FlinkOperation(flinkService);

        try {
            flinkOperation.genPath(flinkInfo, dataFlows);
            flinkOperation.start(flinkInfo);
            log.info("job submit success, jobId is [{}]", flinkInfo.getJobId());
        } catch (Exception e) {
            flinkOperation.pollJobStatus(flinkInfo);
            flinkInfo.setException(true);
            flinkInfo.setExceptionMsg(getExceptionStackMsg(e));
            flinkOperation.pollJobStatus(flinkInfo);

            String message = String.format("startup sort failed for groupId [%s] ", groupId);
            log.error(message, e);
            return ListenerResult.fail(message + e.getMessage());
        }

        saveInfo(groupId, InlongGroupSettings.SORT_JOB_ID, flinkInfo.getJobId(), extList);
        flinkOperation.pollJobStatus(flinkInfo);
        return ListenerResult.success();
    }

    /**
     * Save ext info into list.
     */
    private void saveInfo(String inlongGroupId, String keyName, String keyValue, List<InlongGroupExtInfo> extInfoList) {
        InlongGroupExtInfo extInfo = new InlongGroupExtInfo();
        extInfo.setInlongGroupId(inlongGroupId);
        extInfo.setKeyName(keyName);
        extInfo.setKeyValue(keyValue);
        extInfoList.add(extInfo);
    }

    @Override
    public boolean async() {
        return false;
    }
}
