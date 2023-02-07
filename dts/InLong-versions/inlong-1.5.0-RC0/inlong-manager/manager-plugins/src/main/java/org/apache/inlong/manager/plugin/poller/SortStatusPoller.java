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

package org.apache.inlong.manager.plugin.poller;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobStatus;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.SortStatus;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.plugin.flink.FlinkService;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.sort.SortStatusInfo;
import org.apache.inlong.manager.workflow.plugin.sort.SortPoller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flink sort task status poller for inlong groups
 */
@Slf4j
public class SortStatusPoller implements SortPoller {

    /**
     * Flink job status to InLong sort status mapping.
     */
    private static final Map<JobStatus, SortStatus> JOB_SORT_STATUS_MAP = new HashMap<>(16);

    static {
        JOB_SORT_STATUS_MAP.put(JobStatus.CREATED, SortStatus.NEW);
        JOB_SORT_STATUS_MAP.put(JobStatus.INITIALIZING, SortStatus.NEW);

        JOB_SORT_STATUS_MAP.put(JobStatus.RUNNING, SortStatus.RUNNING);
        JOB_SORT_STATUS_MAP.put(JobStatus.FAILED, SortStatus.FAILED);
        JOB_SORT_STATUS_MAP.put(JobStatus.CANCELED, SortStatus.STOPPED);
        JOB_SORT_STATUS_MAP.put(JobStatus.SUSPENDED, SortStatus.PAUSED);
        JOB_SORT_STATUS_MAP.put(JobStatus.FINISHED, SortStatus.FINISHED);

        JOB_SORT_STATUS_MAP.put(JobStatus.FAILING, SortStatus.OPERATING);
        JOB_SORT_STATUS_MAP.put(JobStatus.CANCELLING, SortStatus.OPERATING);
        JOB_SORT_STATUS_MAP.put(JobStatus.RESTARTING, SortStatus.OPERATING);
        JOB_SORT_STATUS_MAP.put(JobStatus.RECONCILING, SortStatus.OPERATING);
    }

    @Override
    public List<SortStatusInfo> pollSortStatus(List<InlongGroupInfo> groupInfos, String credentials) {
        log.debug("begin to poll sort status for inlong groups");
        if (CollectionUtils.isEmpty(groupInfos)) {
            log.debug("end to poll sort status, as the inlong groups is empty");
            return Collections.emptyList();
        }

        List<SortStatusInfo> statusInfos = new ArrayList<>(groupInfos.size());
        for (InlongGroupInfo groupInfo : groupInfos) {
            String groupId = groupInfo.getInlongGroupId();
            try {
                List<InlongGroupExtInfo> extList = groupInfo.getExtList();
                log.debug("inlong group {} ext info: {}", groupId, extList);

                Map<String, String> kvConf = new HashMap<>();
                extList.forEach(groupExtInfo -> kvConf.put(groupExtInfo.getKeyName(), groupExtInfo.getKeyValue()));
                String sortExt = kvConf.get(InlongConstants.SORT_PROPERTIES);
                if (StringUtils.isNotEmpty(sortExt)) {
                    Map<String, String> result = JsonUtils.OBJECT_MAPPER.convertValue(
                            JsonUtils.OBJECT_MAPPER.readTree(sortExt), new TypeReference<Map<String, String>>() {
                            });
                    kvConf.putAll(result);
                }

                String jobId = kvConf.get(InlongConstants.SORT_JOB_ID);
                SortStatusInfo statusInfo = SortStatusInfo.builder().inlongGroupId(groupId).build();
                if (StringUtils.isBlank(jobId)) {
                    statusInfo.setSortStatus(SortStatus.NOT_EXISTS);
                    statusInfos.add(statusInfo);
                    continue;
                }

                String sortUrl = kvConf.get(InlongConstants.SORT_URL);
                FlinkService flinkService = new FlinkService(sortUrl);
                statusInfo.setSortStatus(
                        JOB_SORT_STATUS_MAP.getOrDefault(flinkService.getJobStatus(jobId), SortStatus.UNKNOWN));
                statusInfos.add(statusInfo);
            } catch (Exception e) {
                log.error("polling sort status failed for groupId=" + groupId, e);
            }
        }

        log.debug("success to get sort status: {}", statusInfos);
        return statusInfos;
    }

}
