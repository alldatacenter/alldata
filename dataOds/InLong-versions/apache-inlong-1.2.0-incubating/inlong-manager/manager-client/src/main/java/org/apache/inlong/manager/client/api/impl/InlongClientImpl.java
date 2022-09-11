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

import com.github.pagehelper.PageInfo;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.InlongClient;
import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.api.enums.SimpleGroupStatus;
import org.apache.inlong.manager.client.api.enums.SimpleSourceStatus;
import org.apache.inlong.manager.client.api.inner.InnerInlongManagerClient;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupListResponse;
import org.apache.inlong.manager.common.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.util.HttpUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Inlong client service implementation.
 */
@Slf4j
public class InlongClientImpl implements InlongClient {

    private static final String URL_SPLITTER = ",";
    private static final String HOST_SPLITTER = ":";
    @Getter
    private final ClientConfiguration configuration;

    public InlongClientImpl(String serviceUrl, ClientConfiguration configuration) {
        Map<String, String> hostPorts = Splitter.on(URL_SPLITTER).withKeyValueSeparator(HOST_SPLITTER)
                .split(serviceUrl);
        if (MapUtils.isEmpty(hostPorts)) {
            throw new IllegalArgumentException(String.format("Unsupported serviceUrl: %s", serviceUrl));
        }
        configuration.setServiceUrl(serviceUrl);
        boolean isConnective = false;
        for (Map.Entry<String, String> hostPort : hostPorts.entrySet()) {
            String host = hostPort.getKey();
            int port = Integer.parseInt(hostPort.getValue());
            if (HttpUtils.checkConnectivity(host, port, configuration.getReadTimeout(), configuration.getTimeUnit())) {
                configuration.setBindHost(host);
                configuration.setBindPort(port);
                isConnective = true;
                break;
            }
        }
        if (!isConnective) {
            throw new RuntimeException(String.format("%s is not connective", serviceUrl));
        }
        this.configuration = configuration;
    }

    @Override
    public InlongGroup forGroup(InlongGroupInfo groupInfo) {
        return new InlongGroupImpl(groupInfo, this);
    }

    @Override
    public List<InlongGroup> listGroup(String expr, int status, int pageNum, int pageSize) {
        InnerInlongManagerClient managerClient = new InnerInlongManagerClient(this.configuration);
        PageInfo<InlongGroupListResponse> responsePageInfo = managerClient.listGroups(expr, status, pageNum,
                pageSize);
        if (CollectionUtils.isEmpty(responsePageInfo.getList())) {
            return Lists.newArrayList();
        } else {
            return responsePageInfo.getList().stream().map(response -> {
                String groupId = response.getInlongGroupId();
                InlongGroupInfo groupInfo = managerClient.getGroupInfo(groupId);
                return new InlongGroupImpl(groupInfo, this);
            }).collect(Collectors.toList());
        }
    }

    @Override
    public Map<String, SimpleGroupStatus> listGroupStatus(List<String> groupIds) {
        InnerInlongManagerClient managerClient = new InnerInlongManagerClient(this.configuration);
        InlongGroupPageRequest request = new InlongGroupPageRequest();
        request.setGroupIdList(groupIds);
        request.setListSources(true);

        PageInfo<InlongGroupListResponse> pageInfo = managerClient.listGroups(request);
        List<InlongGroupListResponse> groupListResponses = pageInfo.getList();
        Map<String, SimpleGroupStatus> groupStatusMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(groupListResponses)) {
            groupListResponses.forEach(response -> {
                String groupId = response.getInlongGroupId();
                SimpleGroupStatus groupStatus = SimpleGroupStatus.parseStatusByCode(response.getStatus());
                List<SourceListResponse> sourceListResponses = response.getSourceResponses();
                groupStatus = recheckGroupStatus(groupStatus, sourceListResponses);
                groupStatusMap.put(groupId, groupStatus);
            });
        }
        return groupStatusMap;
    }

    @Override
    public InlongGroup getGroup(String groupId) {
        InnerInlongManagerClient managerClient = new InnerInlongManagerClient(this.configuration);
        InlongGroupInfo groupInfo = managerClient.getGroupInfo(groupId);
        if (groupInfo == null) {
            return new BlankInlongGroup();
        }
        return new InlongGroupImpl(groupInfo, this);
    }

    private SimpleGroupStatus recheckGroupStatus(SimpleGroupStatus groupStatus,
            List<SourceListResponse> sourceListResponses) {
        Map<SimpleSourceStatus, List<SourceListResponse>> statusListMap = Maps.newHashMap();
        sourceListResponses.forEach(source -> {
            SimpleSourceStatus status = SimpleSourceStatus.parseByStatus(source.getStatus());
            statusListMap.computeIfAbsent(status, k -> Lists.newArrayList()).add(source);
        });
        if (CollectionUtils.isNotEmpty(statusListMap.get(SimpleSourceStatus.FAILED))) {
            return SimpleGroupStatus.FAILED;
        }
        switch (groupStatus) {
            case STARTED:
                if (CollectionUtils.isNotEmpty(statusListMap.get(SimpleSourceStatus.INIT))) {
                    return SimpleGroupStatus.INITIALIZING;
                } else {
                    return groupStatus;
                }
            case STOPPED:
                if (CollectionUtils.isNotEmpty(statusListMap.get(SimpleSourceStatus.FREEZING))) {
                    return SimpleGroupStatus.OPERATING;
                } else {
                    return groupStatus;
                }
            case DELETED:
                if (CollectionUtils.isNotEmpty(statusListMap.get(SimpleSourceStatus.DELETING))) {
                    return SimpleGroupStatus.OPERATING;
                } else {
                    return groupStatus;
                }
            default:
                return groupStatus;
        }
    }
}
