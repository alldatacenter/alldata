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

package org.apache.inlong.manager.client.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.inlong.manager.client.api.enums.SimpleGroupStatus;
import org.apache.inlong.manager.client.api.enums.SimpleSourceStatus;
import org.apache.inlong.manager.client.api.inner.InnerGroupContext;
import org.apache.inlong.manager.common.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.util.AssertUtils;
import org.apache.inlong.manager.common.util.JsonUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Inlong group context.
 */
@Data
@Slf4j
public class InlongGroupContext implements Serializable {

    private String groupId;

    private String groupName;

    private InlongGroupInfo groupInfo;

    private Map<String, InlongStream> inlongStreamMap;

    /**
     * Extension configuration for Inlong group.
     */
    private Map<String, String> extensions;

    /**
     * Logs for Inlong group, taskName->logs.
     */
    private Map<String, List<String>> groupLogs;

    /**
     * Error message for Inlong group, taskName->exceptionMsg.
     */
    private Map<String, List<String>> groupErrLogs;

    /**
     * Logs for each stream, key: streamName, value: componentName->log
     */
    private Map<String, Map<String, List<String>>> streamErrLogs = Maps.newHashMap();

    private SimpleGroupStatus status;

    public InlongGroupContext(InnerGroupContext groupContext) {
        InlongGroupInfo groupInfo = groupContext.getGroupInfo();
        AssertUtils.notNull(groupInfo);
        this.groupId = groupInfo.getInlongGroupId();
        this.groupName = groupInfo.getName();
        this.groupInfo = groupInfo;
        this.inlongStreamMap = groupContext.getStreamMap();
        this.groupErrLogs = Maps.newHashMap();
        this.groupLogs = Maps.newHashMap();
        this.status = SimpleGroupStatus.parseStatusByCode(groupInfo.getStatus());
        recheckState();
        this.extensions = Maps.newHashMap();
        List<InlongGroupExtInfo> extInfos = groupInfo.getExtList();
        if (CollectionUtils.isNotEmpty(extInfos)) {
            extInfos.forEach(extInfo -> {
                extensions.put(extInfo.getKeyName(), extInfo.getKeyValue());
            });
        }
    }

    private void recheckState() {
        if (MapUtils.isEmpty(this.inlongStreamMap)) {
            return;
        }
        List<StreamSource> sourcesInGroup = Lists.newArrayList();
        List<StreamSource> failedSources = Lists.newArrayList();
        this.inlongStreamMap.values().forEach(inlongStream -> {
            Map<String, StreamSource> sources = inlongStream.getSources();
            if (MapUtils.isNotEmpty(sources)) {
                for (Map.Entry<String, StreamSource> entry : sources.entrySet()) {
                    StreamSource source = entry.getValue();
                    if (source != null) {
                        sourcesInGroup.add(source);
                        if (SimpleSourceStatus.parseByStatus(source.getStatus()) == SimpleSourceStatus.FAILED) {
                            failedSources.add(source);
                        }
                    }
                }
            }
        });
        // check if any stream source is failed
        if (CollectionUtils.isNotEmpty(failedSources)) {
            this.status = SimpleGroupStatus.FAILED;
            for (StreamSource failedSource : failedSources) {
                this.groupErrLogs.computeIfAbsent("failedSources", Lists::newArrayList)
                        .add(JsonUtils.toJsonString(failedSource));
            }
            return;
        }
        // check if any stream source is in indirect state
        switch (this.status) {
            case STARTED:
                for (StreamSource source : sourcesInGroup) {
                    if (SimpleSourceStatus.parseByStatus(source.getStatus()) != SimpleSourceStatus.NORMAL) {
                        log.warn("stream source is not started: {}", source);
                        this.status = SimpleGroupStatus.INITIALIZING;
                        break;
                    }
                }
                return;
            case STOPPED:
                for (StreamSource source : sourcesInGroup) {
                    if (SimpleSourceStatus.parseByStatus(source.getStatus()) != SimpleSourceStatus.FROZEN) {
                        log.warn("stream source is not stopped: {}", source);
                        this.status = SimpleGroupStatus.OPERATING;
                        break;
                    }
                }
                return;
            default:
        }
    }

}
