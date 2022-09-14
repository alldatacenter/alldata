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
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.pojo.dataproxy.DataProxyCluster;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfig;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfigResponse;
import org.apache.inlong.common.pojo.dataproxy.DataProxyNodeInfo;
import org.apache.inlong.common.pojo.dataproxy.DataProxyNodeResponse;
import org.apache.inlong.common.pojo.dataproxy.DataProxyTopicInfo;
import org.apache.inlong.common.pojo.dataproxy.MQClusterInfo;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.MQType;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.entity.InlongClusterNodeEntity;
import org.apache.inlong.manager.dao.entity.InlongClusterTagEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.UserEntity;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterNodeEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterTagEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.UserEntityMapper;
import org.apache.inlong.manager.pojo.cluster.BindTagRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeResponse;
import org.apache.inlong.manager.pojo.cluster.ClusterPageRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagPageRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagResponse;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterDTO;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.service.repository.DataProxyConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Inlong cluster service layer implementation
 */
@Service
public class InlongClusterServiceImpl implements InlongClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongClusterServiceImpl.class);
    private static final Gson GSON = new Gson();

    @Autowired
    private UserEntityMapper userMapper;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongStreamEntityMapper streamMapper;
    @Autowired
    private InlongClusterOperatorFactory clusterOperatorFactory;
    @Autowired
    private InlongClusterTagEntityMapper clusterTagMapper;
    @Autowired
    private InlongClusterEntityMapper clusterMapper;
    @Autowired
    private InlongClusterNodeEntityMapper clusterNodeMapper;
    @Lazy
    @Autowired
    private DataProxyConfigRepository proxyRepository;

    @Override
    public Integer saveTag(ClusterTagRequest request, String operator) {
        LOGGER.debug("begin to save cluster tag {}", request);
        Preconditions.checkNotNull(request, "inlong cluster request cannot be empty");
        Preconditions.checkNotNull(request.getClusterTag(), "cluster tag cannot be empty");

        // check if the cluster tag already exist
        String clusterTag = request.getClusterTag();
        InlongClusterTagEntity exist = clusterTagMapper.selectByTag(clusterTag);
        if (exist != null) {
            String errMsg = String.format("inlong cluster tag [%s] already exist", clusterTag);
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }

        InlongClusterTagEntity entity = CommonBeanUtils.copyProperties(request, InlongClusterTagEntity::new);
        entity.setCreator(operator);
        entity.setModifier(operator);
        clusterTagMapper.insert(entity);
        LOGGER.info("success to save cluster tag={} by user={}", request, operator);
        return entity.getId();
    }

    @Override
    public ClusterTagResponse getTag(Integer id, String currentUser) {
        Preconditions.checkNotNull(id, "inlong cluster tag id cannot be empty");
        InlongClusterTagEntity entity = clusterTagMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("inlong cluster tag not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }
        UserEntity userEntity = userMapper.selectByName(currentUser);
        boolean isInCharge = Preconditions.inSeparatedString(currentUser, entity.getInCharges(), InlongConstants.COMMA);
        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to get cluster tag");

        ClusterTagResponse response = CommonBeanUtils.copyProperties(entity, ClusterTagResponse::new);
        LOGGER.debug("success to get cluster tag info by id={}", id);
        return response;
    }

    @Override
    public PageResult<ClusterTagResponse> listTag(ClusterTagPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<InlongClusterTagEntity> entityPage = (Page<InlongClusterTagEntity>) clusterTagMapper
                .selectByCondition(request);

        List<ClusterTagResponse> tagList = CommonBeanUtils.copyListProperties(entityPage, ClusterTagResponse::new);

        PageResult<ClusterTagResponse> pageResult = new PageResult<>(tagList,
                entityPage.getTotal(), entityPage.getPageNum(), entityPage.getPageSize());
        LOGGER.debug("success to list cluster tag by {}", request);
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public Boolean updateTag(ClusterTagRequest request, String operator) {
        LOGGER.debug("begin to update cluster tag={}", request);
        Preconditions.checkNotNull(request, "inlong cluster request cannot be empty");
        String newClusterTag = request.getClusterTag();
        Preconditions.checkNotNull(newClusterTag, "inlong cluster tag cannot be empty");

        Integer id = request.getId();
        Preconditions.checkNotNull(id, "cluster tag id cannot be empty");
        InlongClusterTagEntity exist = clusterTagMapper.selectById(id);
        if (exist == null) {
            LOGGER.warn("inlong cluster tag was not exist for id={}", id);
            return true;
        }
        String errMsg = String.format("cluster tag has already updated with name=%s, curVersion=%s",
                exist.getClusterTag(), request.getVersion());
        if (!Objects.equals(exist.getVersion(), request.getVersion())) {
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }

        UserEntity userEntity = userMapper.selectByName(operator);
        boolean isInCharge = Preconditions.inSeparatedString(operator, exist.getInCharges(), InlongConstants.COMMA);
        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to update cluster tag");

        // if the cluster tag was changed, need to check whether the new tag already exists
        String oldClusterTag = exist.getClusterTag();
        if (!newClusterTag.equals(oldClusterTag)) {
            InlongClusterTagEntity tagConflict = clusterTagMapper.selectByTag(newClusterTag);
            if (tagConflict != null) {
                String tagErrMsg = String.format("inlong cluster tag [%s] already exist", newClusterTag);
                LOGGER.error(tagErrMsg);
                throw new BusinessException(tagErrMsg);
            }

            // check if there are some InlongGroups that uses this tag
            this.assertNoInlongGroupExists(oldClusterTag);

            // update the associated cluster tag in inlong_cluster
            List<InlongClusterEntity> clusterEntities = clusterMapper.selectByKey(oldClusterTag, null, null);
            if (CollectionUtils.isNotEmpty(clusterEntities)) {
                clusterEntities.forEach(entity -> {
                    Set<String> tagSet = Sets.newHashSet(entity.getClusterTags().split(InlongConstants.COMMA));
                    tagSet.remove(oldClusterTag);
                    tagSet.add(newClusterTag);
                    String updateTags = Joiner.on(",").join(tagSet);
                    entity.setClusterTags(updateTags);
                    entity.setModifier(operator);
                    if (InlongConstants.AFFECTED_ONE_ROW != clusterMapper.updateById(entity)) {
                        LOGGER.error("cluster has already updated with name={}, type={}, curVersion={}",
                                entity.getName(), entity.getType(), entity.getVersion());
                        throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
                    }
                });
            }
        }

        CommonBeanUtils.copyProperties(request, exist, true);
        exist.setModifier(operator);
        if (InlongConstants.AFFECTED_ONE_ROW != clusterTagMapper.updateById(exist)) {
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        LOGGER.info("success to update cluster tag={}", request);
        return true;
    }

    @Override
    public Boolean deleteTag(Integer id, String operator) {
        Preconditions.checkNotNull(id, "cluster tag id cannot be empty");
        InlongClusterTagEntity exist = clusterTagMapper.selectById(id);
        if (exist == null || exist.getIsDeleted() > InlongConstants.UN_DELETED) {
            LOGGER.error("inlong cluster tag not found by id={}", id);
            return false;
        }
        UserEntity userEntity = userMapper.selectByName(operator);
        boolean isInCharge = Preconditions.inSeparatedString(operator, exist.getInCharges(), InlongConstants.COMMA);
        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to delete cluster tag");

        // check if there are some InlongGroups that uses this tag
        String clusterTag = exist.getClusterTag();
        this.assertNoInlongGroupExists(clusterTag);

        // update the associated cluster tag in inlong_cluster
        List<InlongClusterEntity> clusterEntities = clusterMapper.selectByKey(clusterTag, null, null);
        if (CollectionUtils.isNotEmpty(clusterEntities)) {
            clusterEntities.forEach(entity -> {
                this.removeClusterTag(entity, clusterTag, operator);
            });
        }

        exist.setIsDeleted(exist.getId());
        exist.setModifier(operator);
        if (InlongConstants.AFFECTED_ONE_ROW != clusterTagMapper.updateById(exist)) {
            LOGGER.error("cluster tag has already updated with name={}, curVersion={}",
                    exist.getClusterTag(), exist.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        LOGGER.info("success to delete cluster tag by id={}", id);
        return true;
    }

    @Override
    public Integer save(ClusterRequest request, String operator) {
        LOGGER.debug("begin to save inlong cluster={}", request);
        Preconditions.checkNotNull(request, "inlong cluster request cannot be empty");

        // check if the cluster already exist
        String clusterTag = request.getClusterTags();
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
    public ClusterInfo get(Integer id, String currentUser) {
        Preconditions.checkNotNull(id, "inlong cluster id cannot be empty");
        InlongClusterEntity entity = clusterMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("inlong cluster not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }
        UserEntity userEntity = userMapper.selectByName(currentUser);
        boolean isInCharge = Preconditions.inSeparatedString(currentUser, entity.getInCharges(), InlongConstants.COMMA);
        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to get cluster info");

        InlongClusterOperator instance = clusterOperatorFactory.getInstance(entity.getType());
        ClusterInfo clusterInfo = instance.getFromEntity(entity);
        LOGGER.debug("success to get inlong cluster info by id={}", id);
        return clusterInfo;
    }

    @Override
    public PageResult<ClusterInfo> list(ClusterPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<InlongClusterEntity> entityPage = (Page<InlongClusterEntity>) clusterMapper.selectByCondition(request);
        List<ClusterInfo> list = entityPage.stream()
                .map(entity -> {
                    InlongClusterOperator instance = clusterOperatorFactory.getInstance(entity.getType());
                    return instance.getFromEntity(entity);
                })
                .collect(Collectors.toList());

        PageResult<ClusterInfo> pageResult = new PageResult<>(
                list, entityPage.getTotal(),
                entityPage.getPageNum(), entityPage.getPageSize()
        );

        LOGGER.debug("success to list inlong cluster by {}", request);
        return pageResult;
    }

    @Override
    public ClusterInfo getOne(String clusterTag, String name, String type) {
        List<InlongClusterEntity> entityList = clusterMapper.selectByKey(clusterTag, name, type);
        if (CollectionUtils.isEmpty(entityList)) {
            throw new BusinessException(String.format("cluster not found by tag=%s, name=%s, type=%s",
                    clusterTag, name, type));
        }

        InlongClusterEntity entity = entityList.get(0);
        InlongClusterOperator instance = clusterOperatorFactory.getInstance(entity.getType());
        ClusterInfo result = instance.getFromEntity(entity);
        LOGGER.debug("success to get inlong cluster by tag={}, name={}, type={}", clusterTag, name, type);
        return result;
    }

    @Override
    public Boolean update(ClusterRequest request, String operator) {
        LOGGER.debug("begin to update inlong cluster: {}", request);
        Preconditions.checkNotNull(request, "inlong cluster info cannot be empty");
        Integer id = request.getId();
        Preconditions.checkNotNull(id, "inlong cluster id cannot be empty");

        // check whether the cluster already exists
        String clusterTag = request.getClusterTags();
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
        if (!Objects.equals(entity.getVersion(), request.getVersion())) {
            LOGGER.error("cluster has already updated with name={}, type={}, curVersion={}",
                    request.getName(), request.getType(), request.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        UserEntity userEntity = userMapper.selectByName(operator);
        boolean isInCharge = Preconditions.inSeparatedString(operator, entity.getInCharges(), InlongConstants.COMMA);
        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to update cluster info");

        InlongClusterOperator instance = clusterOperatorFactory.getInstance(request.getType());
        instance.updateOpt(request, operator);
        LOGGER.info("success to update inlong cluster: {} by {}", request, operator);
        return true;
    }

    @Override
    public Boolean bindTag(BindTagRequest request, String operator) {
        LOGGER.info("begin to bind or unbind cluster tag: {}", request);
        Preconditions.checkNotNull(request, "inlong cluster info cannot be empty");
        String clusterTag = request.getClusterTag();
        Preconditions.checkNotNull(clusterTag, "cluster tag cannot be empty");
        InlongClusterTagEntity exist = clusterTagMapper.selectByTag(clusterTag);
        UserEntity userEntity = userMapper.selectByName(operator);
        boolean isInCharge = Preconditions.inSeparatedString(operator, exist.getInCharges(), InlongConstants.COMMA);
        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to bind or unbind cluster tag");
        if (CollectionUtils.isNotEmpty(request.getBindClusters())) {
            request.getBindClusters().forEach(id -> {
                InlongClusterEntity entity = clusterMapper.selectById(id);
                Set<String> tagSet = Sets.newHashSet(entity.getClusterTags().split(InlongConstants.COMMA));
                tagSet.add(clusterTag);
                String updateTags = Joiner.on(",").join(tagSet);
                InlongClusterEntity updateEntity = clusterMapper.selectById(id);
                updateEntity.setClusterTags(updateTags);
                updateEntity.setModifier(operator);
                if (InlongConstants.AFFECTED_ONE_ROW != clusterMapper.updateById(updateEntity)) {
                    LOGGER.error("cluster has already updated with name={}, type={}, curVersion={}",
                            updateEntity.getName(), updateEntity.getType(), updateEntity.getVersion());
                    throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
                }
            });
        }

        if (CollectionUtils.isNotEmpty(request.getUnbindClusters())) {
            request.getUnbindClusters().forEach(id -> {
                InlongClusterEntity entity = clusterMapper.selectById(id);
                this.removeClusterTag(entity, clusterTag, operator);
            });
        }
        LOGGER.info("success to bind or unbind cluster tag {} by {}", request, operator);
        return true;
    }

    @Override
    public Boolean delete(Integer id, String operator) {
        Preconditions.checkNotNull(id, "cluster id cannot be empty");
        InlongClusterEntity entity = clusterMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() > InlongConstants.UN_DELETED) {
            LOGGER.error("inlong cluster not found by id={}, or was already deleted", id);
            return false;
        }
        UserEntity userEntity = userMapper.selectByName(operator);
        boolean isInCharge = Preconditions.inSeparatedString(operator, entity.getInCharges(), InlongConstants.COMMA);
        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to delete cluster info");

        List<InlongClusterNodeEntity> nodeEntities = clusterNodeMapper.selectByParentId(id);
        if (CollectionUtils.isNotEmpty(nodeEntities)) {
            String errMsg = String.format("there are undeleted nodes under the cluster [%s], "
                    + "please delete the node first", entity.getName());
            throw new BusinessException(errMsg);
        }

        entity.setIsDeleted(entity.getId());
        entity.setModifier(operator);
        if (InlongConstants.AFFECTED_ONE_ROW != clusterMapper.updateById(entity)) {
            LOGGER.error("cluster has already updated with name={}, type={}, curVersion={}", entity.getName(),
                    entity.getType(), entity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
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
        entity.setModifier(operator);
        clusterNodeMapper.insert(entity);

        LOGGER.info("success to add inlong cluster node={}", request);
        return entity.getId();
    }

    @Override
    public ClusterNodeResponse getNode(Integer id, String currentUser) {
        Preconditions.checkNotNull(id, "cluster node id cannot be empty");
        InlongClusterNodeEntity entity = clusterNodeMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("inlong cluster node not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }
        InlongClusterEntity cluster = clusterMapper.selectById(entity.getParentId());
        UserEntity userEntity = userMapper.selectByName(currentUser);
        boolean isInCharge = Preconditions.inSeparatedString(currentUser, cluster.getInCharges(),
                InlongConstants.COMMA);
        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to get cluster node");
        ClusterNodeResponse clusterNodeResponse = CommonBeanUtils.copyProperties(entity, ClusterNodeResponse::new);
        LOGGER.debug("success to get inlong cluster node by id={}", id);
        return clusterNodeResponse;
    }

    @Override
    public PageResult<ClusterNodeResponse> listNode(ClusterPageRequest request, String currentUser) {
        if (StringUtils.isNotBlank(request.getClusterTag())) {
            List<ClusterNodeResponse> nodeList = listNodeByClusterTag(request);

            return new PageResult<>(nodeList, (long) nodeList.size());
        }
        Integer parentId = request.getParentId();
        Preconditions.checkNotNull(parentId, "Cluster id cannot be empty");
        InlongClusterEntity cluster = clusterMapper.selectById(parentId);
        UserEntity userEntity = userMapper.selectByName(currentUser);
        boolean isInCharge = Preconditions.inSeparatedString(currentUser, cluster.getInCharges(),
                InlongConstants.COMMA);

        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to get cluster node list");
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<InlongClusterNodeEntity> entityPage = (Page<InlongClusterNodeEntity>)
                clusterNodeMapper.selectByCondition(request);
        List<ClusterNodeResponse> nodeList = CommonBeanUtils.copyListProperties(entityPage, ClusterNodeResponse::new);

        PageResult<ClusterNodeResponse> pageResult = new PageResult<>(nodeList, entityPage.getTotal(),
                entityPage.getPageNum(), entityPage.getPageSize());

        LOGGER.debug("success to list inlong cluster node by {}", request);
        return pageResult;
    }

    public List<ClusterNodeResponse> listNodeByClusterTag(ClusterPageRequest request) {
        List<InlongClusterEntity> clusterList = clusterMapper.selectByKey(request.getClusterTag(), request.getName(),
                request.getType());
        List<InlongClusterNodeEntity> allNodeList = new ArrayList<>();
        for (InlongClusterEntity cluster : clusterList) {
            List<InlongClusterNodeEntity> nodeList = clusterNodeMapper.selectByParentId(cluster.getId());
            allNodeList.addAll(nodeList);
        }
        return CommonBeanUtils.copyListProperties(allNodeList, ClusterNodeResponse::new);
    }

    @Override
    public List<String> listNodeIpByType(String type) {
        Preconditions.checkNotNull(type, "cluster type cannot be empty");
        ClusterPageRequest request = new ClusterPageRequest();
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
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public Boolean updateNode(ClusterNodeRequest request, String operator) {
        LOGGER.debug("begin to update inlong cluster node={}", request);
        Preconditions.checkNotNull(request, "inlong cluster node cannot be empty");

        Integer id = request.getId();
        Preconditions.checkNotNull(id, "cluster node id cannot be empty");
        // check cluster node if exist
        InlongClusterNodeEntity exist = clusterNodeMapper.selectByUniqueKey(request);
        if (exist != null && !Objects.equals(id, exist.getId())) {
            String errMsg = String.format("inlong cluster node already exist for type=%s ip=%s port=%s",
                    request.getType(), request.getIp(), request.getPort());
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }

        InlongClusterNodeEntity entity = clusterNodeMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("cluster node not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }
        String errMsg = String.format("cluster node has already updated with parentId=%s, type=%s, ip=%s, port=%s",
                request.getParentId(), request.getType(), request.getIp(), request.getPort());
        if (!Objects.equals(entity.getVersion(), request.getVersion())) {
            LOGGER.warn(errMsg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        InlongClusterEntity cluster = clusterMapper.selectById(entity.getParentId());
        UserEntity userEntity = userMapper.selectByName(operator);
        boolean isInCharge = Preconditions.inSeparatedString(operator, cluster.getInCharges(), InlongConstants.COMMA);
        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to update cluster node");
        CommonBeanUtils.copyProperties(request, entity, true);
        entity.setParentId(request.getParentId());
        entity.setModifier(operator);
        if (InlongConstants.AFFECTED_ONE_ROW != clusterNodeMapper.updateById(entity)) {
            LOGGER.warn(errMsg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        LOGGER.info("success to update inlong cluster node={}", request);
        return true;
    }

    @Override
    public Boolean deleteNode(Integer id, String operator) {
        Preconditions.checkNotNull(id, "cluster node id cannot be empty");
        InlongClusterNodeEntity entity = clusterNodeMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() > InlongConstants.UN_DELETED) {
            LOGGER.error("inlong cluster node not found by id={}", id);
            return false;
        }
        InlongClusterEntity cluster = clusterMapper.selectById(entity.getParentId());
        UserEntity userEntity = userMapper.selectByName(operator);
        boolean isInCharge = Preconditions.inSeparatedString(operator, cluster.getInCharges(), InlongConstants.COMMA);
        Preconditions.checkTrue(isInCharge || userEntity.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user does not have permission to delete cluster node");
        entity.setIsDeleted(entity.getId());
        entity.setModifier(operator);
        if (InlongConstants.AFFECTED_ONE_ROW != clusterNodeMapper.updateById(entity)) {
            LOGGER.error("cluster node has already updated with parentId={}, type={}, ip={}, port={}",
                    entity.getParentId(), entity.getType(), entity.getIp(), entity.getPort());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        LOGGER.info("success to delete inlong cluster node by id={}", id);
        return true;
    }

    @Override
    public DataProxyNodeResponse getDataProxyNodes(String inlongGroupId) {
        LOGGER.debug("begin to get data proxy nodes for inlongGroupId={}", inlongGroupId);
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(inlongGroupId);
        if (groupEntity == null) {
            String msg = "inlong group not exists for inlongGroupId=" + inlongGroupId;
            LOGGER.debug(msg);
            throw new BusinessException(msg);
        }

        String clusterTag = groupEntity.getInlongClusterTag();
        if (StringUtils.isBlank(clusterTag)) {
            String msg = "not found any cluster tag for inlongGroupId=" + inlongGroupId;
            LOGGER.debug(msg);
            throw new BusinessException(msg);
        }

        List<InlongClusterEntity> clusterList = clusterMapper.selectByKey(clusterTag, null, ClusterType.DATAPROXY);
        if (CollectionUtils.isEmpty(clusterList)) {
            String msg = "not found any data proxy cluster for inlongGroupId=" + inlongGroupId
                    + " and clusterTag=" + clusterTag;
            LOGGER.debug(msg);
            throw new BusinessException(msg);
        }

        // if more than one data proxy cluster, currently takes first
        // TODO consider the data proxy load and re-balance
        List<DataProxyNodeInfo> nodeInfos = new ArrayList<>();
        for (InlongClusterEntity entity : clusterList) {
            List<InlongClusterNodeEntity> nodeList = clusterNodeMapper.selectByParentId(entity.getId());
            for (InlongClusterNodeEntity nodeEntity : nodeList) {
                DataProxyNodeInfo nodeInfo = new DataProxyNodeInfo();
                nodeInfo.setId(nodeEntity.getId());
                nodeInfo.setIp(nodeEntity.getIp());
                nodeInfo.setPort(nodeEntity.getPort());
                nodeInfos.add(nodeInfo);
            }
        }

        DataProxyNodeResponse response = new DataProxyNodeResponse();
        response.setClusterId(clusterList.get(0).getId());
        response.setNodeList(nodeInfos);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("success to get data proxy nodes for inlongGroupId={}, result={}", inlongGroupId, response);
        }
        return response;
    }

    @Override
    public DataProxyConfig getDataProxyConfig(String clusterTag, String clusterName) {
        LOGGER.debug("GetDPConfig: begin to get config by cluster tag={} name={}", clusterTag, clusterName);

        // get all data proxy clusters
        ClusterPageRequest request = ClusterPageRequest.builder()
                .clusterTag(clusterTag)
                .name(clusterName)
                .type(ClusterType.DATAPROXY)
                .build();
        List<InlongClusterEntity> clusterEntityList = clusterMapper.selectByCondition(request);
        DataProxyConfig result = new DataProxyConfig();
        if (CollectionUtils.isEmpty(clusterEntityList)) {
            LOGGER.warn("GetDPConfig: not found data proxy cluster by tag={} name={}", clusterTag, clusterName);
            return result;
        }

        // get all inlong groups which was successful and belongs to this data proxy cluster
        Set<String> tagSet = new HashSet<>(16);
        clusterEntityList.forEach(e -> tagSet.addAll(Arrays.asList(e.getClusterTags().split(InlongConstants.COMMA))));
        List<String> clusterTagList = new ArrayList<>(tagSet);
        InlongGroupPageRequest groupRequest = InlongGroupPageRequest.builder()
                .status(GroupStatus.CONFIG_SUCCESSFUL.getCode())
                .clusterTagList(clusterTagList)
                .build();

        List<InlongGroupBriefInfo> groupList = groupMapper.selectBriefList(groupRequest);
        if (CollectionUtils.isEmpty(groupList)) {
            LOGGER.warn("GetDPConfig: not found inlong group with success status by cluster tags={}", clusterTagList);
            return result;
        }

        LOGGER.debug("GetDPConfig: begin to get config for cluster tags={}, associated group num={}",
                clusterTagList, groupList.size());
        List<DataProxyTopicInfo> topicList = new ArrayList<>();
        for (InlongGroupBriefInfo groupInfo : groupList) {
            String groupId = groupInfo.getInlongGroupId();
            String mqResource = groupInfo.getMqResource();
            String realClusterTag = groupInfo.getInlongClusterTag();

            String mqType = groupInfo.getMqType();
            if (MQType.PULSAR.equals(mqType) || MQType.TDMQ_PULSAR.equals(mqType)) {
                List<InlongStreamBriefInfo> streamList = streamMapper.selectBriefList(groupId);
                for (InlongStreamBriefInfo streamInfo : streamList) {
                    List<InlongClusterEntity> pulsarClusters = clusterMapper.selectByKey(realClusterTag, null,
                            ClusterType.PULSAR);
                    if (CollectionUtils.isEmpty(pulsarClusters)) {
                        LOGGER.error("GetDPConfig: not found pulsar cluster by cluster tag={}", realClusterTag);
                        continue;
                    }

                    // if there are multiple Pulsar clusters, take the first one
                    InlongClusterEntity cluster = pulsarClusters.get(0);
                    PulsarClusterDTO pulsarCluster = PulsarClusterDTO.getFromJson(cluster.getExtParams());
                    String tenant = pulsarCluster.getTenant();
                    if (StringUtils.isBlank(tenant)) {
                        tenant = InlongConstants.DEFAULT_PULSAR_TENANT;
                    }

                    String streamId = streamInfo.getInlongStreamId();
                    String topic = String.format(InlongConstants.PULSAR_TOPIC_FORMAT,
                            tenant, mqResource, streamInfo.getMqResource());
                    DataProxyTopicInfo topicConfig = new DataProxyTopicInfo();
                    topicConfig.setInlongGroupId(groupId + "/" + streamId);
                    topicConfig.setTopic(topic);
                    topicList.add(topicConfig);
                }
            } else if (MQType.TUBEMQ.equals(mqType)) {
                DataProxyTopicInfo topicConfig = new DataProxyTopicInfo();
                topicConfig.setInlongGroupId(groupId);
                topicConfig.setTopic(mqResource);
                topicList.add(topicConfig);
            }
        }

        // get mq cluster info
        LOGGER.debug("GetDPConfig: begin to get mq clusters by tags={}", clusterTagList);
        List<MQClusterInfo> mqSet = new ArrayList<>();
        List<String> typeList = Arrays.asList(ClusterType.TUBEMQ, ClusterType.PULSAR);
        ClusterPageRequest pageRequest = ClusterPageRequest.builder()
                .typeList(typeList)
                .clusterTagList(clusterTagList)
                .build();
        List<InlongClusterEntity> mqClusterList = clusterMapper.selectByCondition(pageRequest);
        for (InlongClusterEntity cluster : mqClusterList) {
            MQClusterInfo clusterInfo = new MQClusterInfo();
            clusterInfo.setUrl(cluster.getUrl());
            clusterInfo.setToken(cluster.getToken());
            clusterInfo.setMqType(cluster.getType());
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

    /**
     * Remove cluster tag from the given cluster entity.
     */
    private void removeClusterTag(InlongClusterEntity entity, String clusterTag, String operator) {
        Set<String> tagSet = Sets.newHashSet(entity.getClusterTags().split(InlongConstants.COMMA));
        tagSet.remove(clusterTag);
        String updateTags = Joiner.on(",").join(tagSet);
        entity.setClusterTags(updateTags);
        entity.setModifier(operator);
        if (InlongConstants.AFFECTED_ONE_ROW != clusterMapper.updateById(entity)) {
            LOGGER.error("cluster has already updated with name={}, type={}, curVersion={}", entity.getName(),
                    entity.getType(), entity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
    }

    /**
     * Make sure there is no InlongGroup using this tag.
     */
    private void assertNoInlongGroupExists(String clusterTag) {
        List<InlongGroupEntity> groupEntities = groupMapper.selectByClusterTag(clusterTag);
        if (CollectionUtils.isEmpty(groupEntities)) {
            return;
        }
        List<String> groupIds = groupEntities.stream()
                .map(InlongGroupEntity::getInlongGroupId)
                .collect(Collectors.toList());
        String errMsg = String.format("inlong cluster tag [%s] was used by inlong group %s", clusterTag, groupIds);
        LOGGER.error(errMsg);
        throw new BusinessException(errMsg + ", please delete them first");
    }

}
