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

package org.apache.inlong.manager.service.node;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.dao.mapper.DataNodeEntityMapper;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodePageRequest;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data node service layer implementation
 */
@Service
public class DataNodeServiceImpl implements DataNodeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataNodeServiceImpl.class);

    @Autowired
    private DataNodeEntityMapper dataNodeMapper;
    @Autowired
    private DataNodeOperatorFactory operatorFactory;

    @Override
    public Integer save(DataNodeRequest request, String operator) {
        String name = request.getName();
        String type = request.getType();

        // check if data node already exist
        DataNodeEntity existEntity = dataNodeMapper.selectByUniqueKey(name, type);
        if (existEntity != null) {
            String errMsg = String.format("data node already exist for name=%s type=%s)", name, type);
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }
        // according to the data type, save sink information
        DataNodeOperator dataNodeOperator = operatorFactory.getInstance(request.getType());
        int id = dataNodeOperator.saveOpt(request, operator);
        LOGGER.debug("success to save data node={}", request);
        return id;
    }

    @Override
    public Integer save(DataNodeRequest request, UserInfo opInfo) {
        // check request parameter
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.REQUEST_IS_EMPTY);
        }
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_REQUIRED);
        }
        // check if data node already exist
        DataNodeEntity existEntity =
                dataNodeMapper.selectByUniqueKey(request.getName(), request.getType());
        if (existEntity != null) {
            throw new BusinessException(ErrorCodeEnum.RECORD_DUPLICATE,
                    String.format("data node already exist for name=%s type=%s",
                            request.getName(), request.getType()));
        }
        // according to the data type, save sink information
        DataNodeOperator dataNodeOperator = operatorFactory.getInstance(request.getType());
        return dataNodeOperator.saveOpt(request, opInfo.getName());
    }

    @Override
    public DataNodeInfo get(Integer id) {
        DataNodeEntity entity = dataNodeMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("data node not found by id={}", id);
            throw new BusinessException("data node not found");
        }

        String dataNodeType = entity.getType();
        DataNodeOperator dataNodeOperator = operatorFactory.getInstance(dataNodeType);
        DataNodeInfo dataNodeInfo = dataNodeOperator.getFromEntity(entity);
        LOGGER.debug("success to get data node info by id={}", id);
        return dataNodeInfo;
    }

    @Override
    public DataNodeInfo get(Integer id, UserInfo opInfo) {
        if (id == null) {
            throw new BusinessException(ErrorCodeEnum.ID_IS_EMPTY);
        }
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_REQUIRED);
        }
        DataNodeEntity entity = dataNodeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NODE_NOT_FOUND);
        }
        String dataNodeType = entity.getType();
        DataNodeOperator dataNodeOperator = operatorFactory.getInstance(dataNodeType);
        return dataNodeOperator.getFromEntity(entity);
    }

    @Override
    public DataNodeInfo get(String name, String type) {
        DataNodeEntity entity = dataNodeMapper.selectByUniqueKey(name, type);
        if (entity == null) {
            String errMsg = String.format("data node not found by name=%s, type=%s", name, type);
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }

        DataNodeOperator dataNodeOperator = operatorFactory.getInstance(type);
        DataNodeInfo dataNodeInfo = dataNodeOperator.getFromEntity(entity);
        LOGGER.debug("success to get data node by name={} type={}", name, type);
        return dataNodeInfo;
    }

    @Override
    public PageResult<DataNodeInfo> list(DataNodePageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<DataNodeEntity> entityPage = (Page<DataNodeEntity>) dataNodeMapper.selectByCondition(request);
        List<DataNodeInfo> list = entityPage.stream()
                .map(entity -> {
                    DataNodeOperator dataNodeOperator = operatorFactory.getInstance(entity.getType());
                    return dataNodeOperator.getFromEntity(entity);
                }).collect(Collectors.toList());

        PageResult<DataNodeInfo> pageResult = new PageResult<>(list, entityPage.getTotal(),
                entityPage.getPageNum(), entityPage.getPageSize());

        LOGGER.debug("success to list data node by {}", request);
        return pageResult;
    }

    @Override
    public List<DataNodeInfo> list(DataNodePageRequest request, UserInfo opInfo) {
        // check request parameter
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.REQUEST_IS_EMPTY);
        }
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_REQUIRED);
        }
        // query result
        List<DataNodeEntity> nodeEntities = dataNodeMapper.selectByCondition(request);
        return nodeEntities.stream()
                .map(entity -> {
                    DataNodeOperator dataNodeOperator = operatorFactory.getInstance(entity.getType());
                    return dataNodeOperator.getFromEntity(entity);
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean update(DataNodeRequest request, String operator) {
        LOGGER.info("begin to update data node by id: {}", request);
        // check whether record existed
        DataNodeEntity curEntity = dataNodeMapper.selectById(request.getId());
        if (curEntity == null) {
            throw new BusinessException(ErrorCodeEnum.RECORD_NOT_FOUND,
                    String.format("data node record not found by id=%d", request.getId()));
        }
        // Check whether the data node name exists with the same name and type
        if (request.getName() != null) {
            if (StringUtils.isBlank(request.getName())) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                        "the name changed of data node is blank!");
            }
            DataNodeEntity existEntity =
                    dataNodeMapper.selectByUniqueKey(request.getName(), request.getType());
            if (existEntity != null && !existEntity.getId().equals(request.getId())) {
                throw new BusinessException(ErrorCodeEnum.RECORD_DUPLICATE,
                        String.format("data node already exist for name=%s, type=%s, required id=%s, exist id=%s",
                                request.getName(), request.getType(), request.getId(), existEntity.getId()));
            }
        }
        DataNodeOperator dataNodeOperator = operatorFactory.getInstance(request.getType());
        dataNodeOperator.updateOpt(request, operator);

        LOGGER.info("success to update data node={}", request);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean update(DataNodeRequest request, UserInfo opInfo) {
        // check request parameter
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.REQUEST_IS_EMPTY);
        }
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_REQUIRED);
        }
        // check the record existed
        DataNodeEntity curEntity = dataNodeMapper.selectById(request.getId());
        if (curEntity == null) {
            throw new BusinessException(ErrorCodeEnum.RECORD_NOT_FOUND,
                    String.format("data node record not found by id=%d", request.getId()));
        }
        // Check whether the data node name exists with the same name and type
        if (request.getName() != null) {
            if (StringUtils.isBlank(request.getName())) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                        "the name changed of data node is blank!");
            }
            DataNodeEntity existEntity =
                    dataNodeMapper.selectByUniqueKey(request.getName(), request.getType());
            if (existEntity != null && !existEntity.getId().equals(request.getId())) {
                throw new BusinessException(ErrorCodeEnum.RECORD_DUPLICATE,
                        String.format("data node already exist for name=%s, type=%s, required id=%s, exist id=%s",
                                request.getName(), request.getType(), request.getId(), existEntity.getId()));
            }
        }
        DataNodeOperator dataNodeOperator = operatorFactory.getInstance(request.getType());
        dataNodeOperator.updateOpt(request, opInfo.getName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public UpdateResult updateByKey(DataNodeRequest request, String operator) {
        LOGGER.info("begin to update data node by key: {}", request);

        // Check whether the data node exists
        String name = request.getName();
        String type = request.getType();
        DataNodeEntity entity = dataNodeMapper.selectByUniqueKey(name, type);
        if (entity == null) {
            String errMsg = String.format("data node not found by name=%s, type=%s", name, type);
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }

        request.setId(entity.getId());
        Boolean result = this.update(request, operator);
        LOGGER.info("success to update data node by key: {}", request);
        return new UpdateResult(entity.getId(), result, request.getVersion() + 1);
    }

    @Override
    public Boolean delete(Integer id, String operator) {
        DataNodeEntity entity = dataNodeMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() > InlongConstants.UN_DELETED) {
            LOGGER.error("data node not found or was already deleted for id={}", id);
            return false;
        }

        return delete(entity, operator);
    }

    @Override
    public Boolean delete(Integer id, UserInfo opInfo) {
        // check id parameter
        if (id == null) {
            throw new BusinessException(ErrorCodeEnum.ID_IS_EMPTY);
        }
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_REQUIRED);
        }
        DataNodeEntity entity = dataNodeMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() > InlongConstants.UN_DELETED) {
            return true;
        }
        // delete record
        entity.setIsDeleted(entity.getId());
        entity.setModifier(opInfo.getName());
        int rowCount = dataNodeMapper.updateById(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format("data node has already updated, data node name=%s, type=%s, current version=%s",
                            entity.getName(), entity.getType(), entity.getVersion()));
        }
        return true;
    }

    private Boolean delete(DataNodeEntity entity, String operator) {
        entity.setIsDeleted(entity.getId());
        entity.setModifier(operator);
        int rowCount = dataNodeMapper.updateById(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("data node has already updated, data node name={}, type={}, current version ={}",
                    entity.getName(), entity.getType(), entity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        LOGGER.info("success to delete data node by id={}, name={}", entity.getId(), entity.getName());
        return true;
    }

    @Override
    public Boolean deleteByKey(String name, String type, String operator) {
        DataNodeEntity entity = dataNodeMapper.selectByUniqueKey(name, type);
        if (entity == null || entity.getIsDeleted() > InlongConstants.UN_DELETED) {
            LOGGER.error("data node not found or was already deleted for name={}", name);
            return false;
        }
        return delete(entity, operator);
    }

    @Override
    public Boolean testConnection(DataNodeRequest request) {
        LOGGER.info("begin test connection for: {}", request);
        String type = request.getType();

        // according to the data node type, test connection
        DataNodeOperator dataNodeOperator = operatorFactory.getInstance(request.getType());
        Boolean result = dataNodeOperator.testConnection(request);
        LOGGER.info("connection [{}] for: {}", result ? "success" : "failed", request);
        return result;
    }

}
