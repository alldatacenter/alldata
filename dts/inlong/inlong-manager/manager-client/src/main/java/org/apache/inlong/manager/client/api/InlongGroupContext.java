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
import org.apache.inlong.manager.common.enums.SimpleGroupStatus;
import org.apache.inlong.manager.common.enums.SimpleSourceStatus;
import org.apache.inlong.manager.client.api.inner.InnerGroupContext;
import org.apache.inlong.manager.common.enums.SortStatus;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupStatusInfo;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.common.util.Preconditions;

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

    private SimpleGroupStatus status;

    private SortStatus sortStatus = SortStatus.UNKNOWN;

    private InlongGroupStatusInfo statusInfo;

    public InlongGroupContext(InnerGroupContext groupContext) {
        InlongGroupInfo groupInfo = groupContext.getGroupInfo();
        Preconditions.expectNotNull(groupInfo, "inlong group info cannot be null");
        this.groupId = groupInfo.getInlongGroupId();
        this.groupName = groupInfo.getName();
        this.groupInfo = groupInfo;
        this.inlongStreamMap = groupContext.getStreamMap();
        this.status = SimpleGroupStatus.parseStatusByCode(groupInfo.getStatus());
        recheckState();
        this.statusInfo = InlongGroupStatusInfo.builder()
                .inlongGroupId(groupInfo.getInlongGroupId())
                .originalStatus(groupInfo.getStatus())
                .simpleGroupStatus(this.status)
                .sortStatus(this.sortStatus)
                .streamSources(getGroupSources()).build();
        this.extensions = Maps.newHashMap();
        List<InlongGroupExtInfo> extInfos = groupInfo.getExtList();
        if (CollectionUtils.isNotEmpty(extInfos)) {
            extInfos.forEach(extInfo -> {
                extensions.put(extInfo.getKeyName(), extInfo.getKeyValue());
            });
        }
    }

    public void updateSortStatus(SortStatus sortStatus) {
        this.sortStatus = sortStatus;
        this.statusInfo.setSortStatus(sortStatus);
    }

    private List<StreamSource> getGroupSources() {
        List<StreamSource> groupSources = Lists.newArrayList();
        this.inlongStreamMap.values().forEach(inlongStream -> {
            Map<String, StreamSource> sources = inlongStream.getSources();
            if (MapUtils.isNotEmpty(sources)) {
                for (Map.Entry<String, StreamSource> entry : sources.entrySet()) {
                    StreamSource source = entry.getValue();
                    // when template id is null it is considered as normal source other than template source
                    // sub sources are filtered because they are already collected in template source's sub source list
                    if (source != null && source.getTemplateId() == null) {
                        groupSources.add(source);
                    }
                }
            }
        });
        return groupSources;
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
