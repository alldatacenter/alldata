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

package org.apache.inlong.manager.service.core.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.enums.DataNodeType;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.node.DataNodePageRequest;
import org.apache.inlong.manager.common.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.common.pojo.node.DataNodeResponse;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.dao.mapper.DataNodeEntityMapper;
import org.apache.inlong.manager.service.core.DataNodeService;
import org.apache.inlong.manager.service.resource.hive.HiveJdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Data node service layer implementation
 */
@Service
public class DataNodeServiceImpl implements DataNodeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataNodeServiceImpl.class);

    @Autowired
    private DataNodeEntityMapper dataNodeMapper;

    @Override
    public Integer save(DataNodeRequest request, String operator) {
        // check request
        Preconditions.checkNotNull(request, "data node info cannot be empty");
        String name = request.getName();
        String type = request.getType();
        Preconditions.checkNotEmpty(name, "data node name cannot be empty");
        Preconditions.checkNotEmpty(type, "data node type cannot be empty");

        // check if data node already exist
        DataNodeEntity exist = dataNodeMapper.selectByNameAndType(name, type);
        if (exist != null) {
            String errMsg = String.format("data node already exist for name=%s type=%s)", name, type);
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }
        DataNodeEntity entity = CommonBeanUtils.copyProperties(request, DataNodeEntity::new);
        entity.setCreator(operator);
        entity.setCreateTime(new Date());
        entity.setIsDeleted(GlobalConstants.UN_DELETED);
        dataNodeMapper.insert(entity);

        LOGGER.debug("success to save data node={}", request);
        return entity.getId();
    }

    @Override
    public DataNodeResponse get(Integer id) {
        Preconditions.checkNotNull(id, "data node id cannot be empty");
        DataNodeEntity entity = dataNodeMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("data node not found by id={}", id);
            throw new BusinessException("data node not found");
        }
        DataNodeResponse response = CommonBeanUtils.copyProperties(entity, DataNodeResponse::new);
        LOGGER.debug("success to get data node info by id={}", id);
        return response;
    }

    @Override
    public PageInfo<DataNodeResponse> list(DataNodePageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<DataNodeEntity> entityPage = (Page<DataNodeEntity>) dataNodeMapper.selectByCondition(request);
        List<DataNodeResponse> responseList = CommonBeanUtils.copyListProperties(entityPage, DataNodeResponse::new);
        PageInfo<DataNodeResponse> page = new PageInfo<>(responseList);
        page.setTotal(responseList.size());
        LOGGER.debug("success to list data node by {}", request);
        return page;
    }

    @Override
    public Boolean update(DataNodeRequest request, String operator) {
        // check request
        Preconditions.checkNotNull(request, "data node info cannot be empty");
        String name = request.getName();
        String type = request.getType();
        Preconditions.checkNotEmpty(name, "data node name cannot be empty");
        Preconditions.checkNotEmpty(type, "data node type cannot be empty");

        Integer id = request.getId();
        Preconditions.checkNotNull(id, "data node id is empty");
        DataNodeEntity exist = dataNodeMapper.selectByNameAndType(name, type);
        if (exist != null && !Objects.equals(id, exist.getId())) {
            String errMsg = String.format("data node already exist for name=%s type=%s", name, type);
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }

        DataNodeEntity entity = dataNodeMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("data node not found by id={}", id);
            throw new BusinessException(String.format("data node not found by id=%s", id));
        }
        CommonBeanUtils.copyProperties(request, entity, true);
        entity.setModifier(operator);
        dataNodeMapper.updateById(entity);

        LOGGER.info("success to update data node={}", request);
        return true;
    }

    @Override
    public Boolean delete(Integer id, String operator) {
        Preconditions.checkNotNull(id, "data node id cannot be empty");
        DataNodeEntity entity = dataNodeMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() > GlobalConstants.UN_DELETED) {
            LOGGER.error("data node not found or was already deleted for id={}", id);
            return false;
        }

        entity.setIsDeleted(entity.getId());
        entity.setModifier(operator);
        dataNodeMapper.updateById(entity);
        LOGGER.info("success to delete data node by id={}", id);
        return true;
    }

    @Override
    public Boolean testConnection(DataNodeRequest request) {
        LOGGER.info("begin test connection for: {}", request);
        Preconditions.checkNotNull(request, "Connection request cannot be empty");
        String type = request.getType();
        Preconditions.checkNotNull(type, "Connection type cannot be empty");

        Boolean result = false;
        if (DataNodeType.HIVE.toString().equals(type)) {
            result = testHiveConnection(request);
        }

        LOGGER.info("connection [{}] for: {}", result ? "success" : "failed", request);
        return result;
    }

    /**
     * Test connection for Hive
     */
    private Boolean testHiveConnection(DataNodeRequest request) {
        String url = request.getUrl();
        Preconditions.checkNotNull(url, "connection url cannot be empty");
        try (Connection ignored = HiveJdbcUtils.getConnection(url, request.getUsername(), request.getToken())) {
            LOGGER.info("hive connection not null - connection success");
            return true;
        } catch (Exception e) {
            LOGGER.error("hive connection failed: {}", e.getMessage());
            return false;
        }
    }

}
