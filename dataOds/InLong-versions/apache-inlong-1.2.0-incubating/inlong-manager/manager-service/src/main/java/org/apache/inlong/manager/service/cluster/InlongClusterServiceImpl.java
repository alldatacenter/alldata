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

package org.apache.inlong.manager.service.cluster;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.pojo.dataproxy.DataProxyCluster;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfig;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfigResponse;
import org.apache.inlong.common.pojo.dataproxy.DataProxyTopicInfo;
import org.apache.inlong.common.pojo.dataproxy.MQClusterInfo;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.common.pojo.cluster.ClusterNodeResponse;
import org.apache.inlong.manager.common.pojo.cluster.InlongClusterInfo;
import org.apache.inlong.manager.common.pojo.cluster.InlongClusterPageRequest;
import org.apache.inlong.manager.common.pojo.cluster.InlongClusterRequest;
import org.apache.inlong.manager.common.pojo.cluster.pulsar.PulsarClusterDTO;
import org.apache.inlong.manager.common.pojo.dataproxy.DataProxyNodeInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.common.settings.InlongGroupSettings;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.entity.InlongClusterNodeEntity;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterNodeEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.service.repository.DataProxyConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Inlong cluster service layer implementation
 */
@Service
public class InlongClusterServiceImpl implements InlongClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongClusterServiceImpl.class);
    private static final Gson GSON = new Gson();

    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongStreamEntityMapper streamMapper;
    @Autowired
    private InlongClusterOperatorFactory clusterOperatorFactory;
    @Autowired
    private InlongClusterEntityMapper clusterMapper;
    @Autowired
    private InlongClusterNodeEntityMapper clusterNodeMapper;
    @Autowired
    private DataProxyConfigRepository proxyRepository;

    @Override
    public Integer save(InlongClusterRequest request, String operator) {
        LOGGER.debug("begin to save inlong cluster={}", request);
        Preconditions.checkNotNull(request, "inlong cluster info cannot be empty");

        // check if the cluster already exist
        String clusterTag = request.getClusterTag();
        String name = request.getName();
        String type = request.getType();
        List<InlongClusterEntity> exist = clusterMapper.selectByKey(clusterTag, name, type);
        if (CollectionUtils.isNotEmpty(exist)) {
            String errMsg = String.format("inlong cluster already exist for cluster tag=%s name=%s type=%s",
                    clusterTag, name, type);
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }

        InlongClusterOperator instance = clusterOperatorFactory.getInstance(request.getType());
        Integer id = instance.saveOpt(request, operator);
        LOGGER.info("success to save inlong cluster={} by user={}", request, operator);
        return id;
    }

    @Override
    public InlongClusterInfo get(Integer id) {
        Preconditions.checkNotNull(id, "inlong cluster id cannot be empty");
        InlongClusterEntity entity = clusterMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("inlong cluster not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }

        InlongClusterOperator instance = clusterOperatorFactory.getInstance(entity.getType());
        InlongClusterInfo clusterInfo = instance.getFromEntity(entity);
        LOGGER.debug("success to get inlong cluster info by id={}", id);
        return clusterInfo;
    }

    @Override
    public PageInfo<InlongClusterInfo> list(InlongClusterPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<InlongClusterEntity> entityPage = (Page<InlongClusterEntity>) clusterMapper.selectByCondition(request);

        List<InlongClusterInfo> list = entityPage.stream()
                .map(entity -> {
                    InlongClusterOperator instance = clusterOperatorFactory.getInstance(entity.getType());
                    return instance.getFromEntity(entity);
                }).collect(Collectors.toList());
        PageInfo<InlongClusterInfo> page = new PageInfo<>(list);
        page.setTotal(list.size());
        LOGGER.debug("success to list inlong cluster by {}", request);
        return page;
    }

    @Override
    public InlongClusterInfo getOne(String clusterTag, String name, String type) {
        List<InlongClusterEntity> entityList = clusterMapper.selectByKey(clusterTag, name, type);
        if (CollectionUtils.isEmpty(entityList)) {
            throw new BusinessException(String.format("cluster not found by tag=%s, name=%s, type=%s",
                    clusterTag, name, type));
        }

        InlongClusterEntity entity = entityList.get(0);
        InlongClusterOperator instance = clusterOperatorFactory.getInstance(entity.getType());
        InlongClusterInfo result = instance.getFromEntity(entity);
        LOGGER.debug("success to get inlong cluster by tag={}, name={}, type={}", clusterTag, name, type);
        return result;
    }

    @Override
    public Boolean update(InlongClusterRequest request, String operator) {
        LOGGER.debug("begin to update inlong cluster={}", request);
        Preconditions.checkNotNull(request, "inlong cluster info cannot be empty");
        Integer id = request.getId();
        Preconditions.checkNotNull(id, "inlong cluster id cannot be empty");

        // check whether the cluster already exists
        String clusterTag = request.getClusterTag();
        String name = request.getName();
        String type = request.getType();
        List<InlongClusterEntity> exist = clusterMapper.selectByKey(clusterTag, name, type);
        if (CollectionUtils.isNotEmpty(exist) && !Objects.equals(id, exist.get(0).getId())) {
            String errMsg = String.format("inlong cluster already exist for cluster tag=%s name=%s type=%s",
                    clusterTag, name, type);
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }

        InlongClusterEntity entity = clusterMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("inlong cluster not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }

        InlongClusterOperator instance = clusterOperatorFactory.getInstance(request.getType());
        instance.updateOpt(request, operator);
        LOGGER.info("success to update inlong cluster={}", request);
        return true;
    }

    @Override
    public Boolean delete(Integer id, String operator) {
        Preconditions.checkNotNull(id, "cluster id cannot be empty");
        InlongClusterEntity entity = clusterMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() > GlobalConstants.UN_DELETED) {
            LOGGER.error("inlong cluster not found by id={}, or was already deleted", id);
            return false;
        }

        entity.setIsDeleted(entity.getId());
        entity.setModifier(operator);
        clusterMapper.updateById(entity);
        LOGGER.info("success to delete inlong cluster for id={} by user={}", id, operator);
        return true;
    }

    @Override
    public Integer saveNode(ClusterNodeRequest request, String operator) {
        LOGGER.debug("begin to insert inlong cluster node={}", request);
        Preconditions.checkNotNull(request, "cluster node info cannot be empty");

        // check cluster node if exist
        InlongClusterNodeEntity exist = clusterNodeMapper.selectByUniqueKey(request);
        if (exist != null) {
            String errMsg = String.format("inlong cluster node already exist for type=%s ip=%s port=%s",
                    request.getType(), request.getIp(), request.getPort());
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }

        InlongClusterNodeEntity entity = CommonBeanUtils.copyProperties(request, InlongClusterNodeEntity::new);
        entity.setCreator(operator);
        entity.setCreateTime(new Date());
        entity.setIsDeleted(GlobalConstants.UN_DELETED);
        clusterNodeMapper.insert(entity);

        LOGGER.info("success to add inlong cluster node={}", request);
        return entity.getId();
    }

    @Override
    public ClusterNodeResponse getNode(Integer id) {
        Preconditions.checkNotNull(id, "cluster node id cannot be empty");
        InlongClusterNodeEntity entity = clusterNodeMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("inlong cluster node not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }
        ClusterNodeResponse clusterNodeResponse = CommonBeanUtils.copyProperties(entity, ClusterNodeResponse::new);
        LOGGER.debug("success to get inlong cluster node by id={}", id);
        return clusterNodeResponse;
    }

    @Override
    public PageInfo<ClusterNodeResponse> listNode(InlongClusterPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<InlongClusterNodeEntity> entityPage = (Page<InlongClusterNodeEntity>)
                clusterNodeMapper.selectByCondition(request);
        List<ClusterNodeResponse> nodeList = CommonBeanUtils.copyListProperties(entityPage, ClusterNodeResponse::new);
        PageInfo<ClusterNodeResponse> page = new PageInfo<>(nodeList);
        page.setTotal(nodeList.size());

        LOGGER.debug("success to list inlong cluster node by {}", request);
        return page;
    }

    @Override
    public List<String> listNodeIpByType(String type) {
        Preconditions.checkNotNull(type, "cluster type cannot be empty");
        InlongClusterPageRequest request = new InlongClusterPageRequest();
        request.setType(type);
        List<InlongClusterNodeEntity> nodeList = clusterNodeMapper.selectByCondition(request);
        if (CollectionUtils.isEmpty(nodeList)) {
            LOGGER.debug("not found any node for type={}", type);
            return Collections.emptyList();
        }

        List<String> ipList = nodeList.stream()
                .map(node -> String.format("%s:%d", node.getIp(), node.getPort()))
                .collect(Collectors.toList());
        LOGGER.debug("success to list node by type={}, result={}", type, ipList);
        return ipList;
    }

    @Override
    public Boolean updateNode(ClusterNodeRequest request, String operator) {
        LOGGER.debug("begin to update inlong cluster node={}", request);
        Preconditions.checkNotNull(request, "inlong cluster node cannot be empty");

        Integer id = request.getId();
        Preconditions.checkNotNull(id, "cluster node id cannot be empty");
        // check cluster node if exist
        InlongClusterNodeEntity exist = clusterNodeMapper.selectByUniqueKey(request);
        if (exist != null && !Objects.equals(id, exist.getId())) {
            String errMsg = String.format("inlong cluster node already exist for type=%s ip=%s port=%s)",
                    request.getType(), request.getIp(), request.getPort());
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }

        InlongClusterNodeEntity entity = clusterNodeMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("cluster node not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }
        CommonBeanUtils.copyProperties(request, entity, true);
        entity.setParentId(request.getParentId());
        entity.setModifier(operator);
        clusterNodeMapper.updateById(entity);

        LOGGER.info("success to update inlong cluster node={}", request);
        return true;
    }

    @Override
    public Boolean deleteNode(Integer id, String operator) {
        Preconditions.checkNotNull(id, "cluster node id cannot be empty");
        InlongClusterNodeEntity entity = clusterNodeMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() > GlobalConstants.UN_DELETED) {
            LOGGER.error("inlong cluster node not found by id={}", id);
            return false;
        }
        entity.setIsDeleted(entity.getId());
        entity.setModifier(operator);
        clusterNodeMapper.updateById(entity);
        LOGGER.info("success to delete inlong cluster node by id={}", id);
        return true;
    }

    @Override
    public List<DataProxyNodeInfo> getDataProxyNodeList(String clusterTag, String clusterName) {
        LOGGER.debug("begin to list data proxy node for tag={} name={}", clusterTag, clusterName);

        InlongClusterPageRequest request = new InlongClusterPageRequest();
        request.setClusterTag(clusterTag);
        request.setName(clusterName);
        request.setType(ClusterType.CLS_DATA_PROXY);
        List<InlongClusterEntity> clusterList = clusterMapper.selectByCondition(request);
        Preconditions.checkNotEmpty(clusterList,
                "data proxy node not found by tag=" + clusterTag + " name=" + clusterName);

        List<DataProxyNodeInfo> responseList = new ArrayList<>();
        for (InlongClusterEntity cluster : clusterList) {
            Integer clusterId = cluster.getId();
            List<InlongClusterNodeEntity> nodeList = clusterNodeMapper.selectByParentId(clusterId);
            for (InlongClusterNodeEntity nodeEntity : nodeList) {
                DataProxyNodeInfo response = new DataProxyNodeInfo();
                response.setId(nodeEntity.getId());
                response.setParentId(clusterId);
                response.setIp(nodeEntity.getIp());
                response.setPort(nodeEntity.getPort());
                responseList.add(response);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("success to list data proxy node for tag={} name={}, result={}",
                    clusterTag, clusterName, responseList);
        }
        return responseList;
    }

    @Override
    public DataProxyConfig getDataProxyConfig(String clusterTag, String clusterName) {
        LOGGER.debug("GetDPConfig: begin to get config by cluster tag={} name={}", clusterTag, clusterName);

        // get all data proxy clusters
        InlongClusterPageRequest request = InlongClusterPageRequest.builder()
                .clusterTag(clusterTag)
                .name(clusterName)
                .type(ClusterType.CLS_DATA_PROXY)
                .build();
        List<InlongClusterEntity> clusterList = clusterMapper.selectByCondition(request);
        DataProxyConfig result = new DataProxyConfig();
        if (CollectionUtils.isEmpty(clusterList)) {
            LOGGER.warn("GetDPConfig: data proxy cluster not found by tag={} name={}", clusterTag, clusterName);
            return result;
        }

        // get all inlong groups which was successful and belongs to this data proxy cluster
        List<String> clusterTagList = clusterList.stream()
                .map(InlongClusterEntity::getClusterTag)
                .collect(Collectors.toList());
        InlongGroupPageRequest groupRequest = InlongGroupPageRequest.builder()
                .status(GroupStatus.CONFIG_SUCCESSFUL.getCode())
                .clusterTagList(clusterTagList)
                .build();

        List<InlongGroupBriefInfo> groupList = groupMapper.selectBriefList(groupRequest);
        if (CollectionUtils.isEmpty(groupList)) {
            LOGGER.warn("GetDPConfig: no inlong group found with success status by cluster tags={}", clusterTagList);
            return result;
        }

        LOGGER.debug("GetDPConfig: begin to get config for cluster tags={}, associated group num={}",
                clusterTagList, groupList.size());
        List<DataProxyTopicInfo> topicList = new ArrayList<>();
        for (InlongGroupBriefInfo groupInfo : groupList) {
            String groupId = groupInfo.getInlongGroupId();
            String mqResource = groupInfo.getMqResource();
            String realClusterTag = groupInfo.getInlongClusterTag();

            MQType type = MQType.forType(groupInfo.getMqType());
            if (type == MQType.PULSAR || type == MQType.TDMQ_PULSAR) {
                List<InlongStreamBriefInfo> streamList = streamMapper.selectBriefList(groupId);
                for (InlongStreamBriefInfo streamInfo : streamList) {
                    List<InlongClusterEntity> pulsarClusters = clusterMapper.selectByKey(realClusterTag, null,
                            ClusterType.CLS_PULSAR);
                    if (CollectionUtils.isEmpty(pulsarClusters)) {
                        LOGGER.error("GetDPConfig: pulsar cluster not found by cluster tag={}", realClusterTag);
                        continue;
                    }

                    // if there are multiple Pulsar clusters, take the first one
                    InlongClusterEntity cluster = pulsarClusters.get(0);
                    PulsarClusterDTO pulsarCluster = PulsarClusterDTO.getFromJson(cluster.getExtParams());
                    String tenant = pulsarCluster.getTenant();
                    if (StringUtils.isBlank(tenant)) {
                        tenant = InlongGroupSettings.DEFAULT_PULSAR_TENANT;
                    }

                    String streamId = streamInfo.getInlongStreamId();
                    String topic = String.format(InlongGroupSettings.PULSAR_TOPIC_FORMAT,
                            tenant, mqResource, streamInfo.getMqResource());
                    DataProxyTopicInfo topicConfig = new DataProxyTopicInfo();
                    topicConfig.setInlongGroupId(groupId + "/" + streamId);
                    topicConfig.setTopic(topic);
                    topicList.add(topicConfig);
                }
            } else if (type == MQType.TUBE) {
                DataProxyTopicInfo topicConfig = new DataProxyTopicInfo();
                topicConfig.setInlongGroupId(groupId);
                topicConfig.setTopic(mqResource);
                topicList.add(topicConfig);
            }
        }

        // get mq cluster info
        LOGGER.debug("GetDPConfig: begin to get mq clusters by tags={}", clusterTagList);
        List<MQClusterInfo> mqSet = new ArrayList<>();
        List<String> typeList = Arrays.asList(ClusterType.CLS_TUBE, ClusterType.CLS_PULSAR);
        InlongClusterPageRequest pageRequest = InlongClusterPageRequest.builder()
                .typeList(typeList)
                .clusterTagList(clusterTagList)
                .build();
        List<InlongClusterEntity> mqClusterList = clusterMapper.selectByCondition(pageRequest);
        for (InlongClusterEntity cluster : mqClusterList) {
            MQClusterInfo clusterInfo = new MQClusterInfo();
            clusterInfo.setUrl(cluster.getUrl());
            clusterInfo.setToken(cluster.getToken());
            Map<String, String> configParams = GSON.fromJson(cluster.getExtParams(), Map.class);
            clusterInfo.setParams(configParams);
            mqSet.add(clusterInfo);
        }

        result.setMqClusterList(mqSet);
        result.setTopicList(topicList);

        return result;
    }

    @Override
    public String getAllConfig(String clusterName, String md5) {
        DataProxyConfigResponse response = new DataProxyConfigResponse();
        String configMd5 = proxyRepository.getProxyMd5(clusterName);
        if (configMd5 == null) {
            response.setResult(false);
            response.setErrCode(DataProxyConfigResponse.REQ_PARAMS_ERROR);
            return GSON.toJson(response);
        }

        // same config
        if (configMd5.equals(md5)) {
            response.setResult(true);
            response.setErrCode(DataProxyConfigResponse.NOUPDATE);
            response.setMd5(configMd5);
            response.setData(new DataProxyCluster());
            return GSON.toJson(response);
        }

        String configJson = proxyRepository.getProxyConfigJson(clusterName);
        if (configJson == null) {
            response.setResult(false);
            response.setErrCode(DataProxyConfigResponse.REQ_PARAMS_ERROR);
            return GSON.toJson(response);
        }

        return configJson;
    }

}
