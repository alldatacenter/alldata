/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.datavines.common.utils.PasswordFilterUtils;
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;

import io.datavines.core.utils.LanguageUtils;
import io.datavines.server.api.dto.bo.storage.ErrorDataStorageCreate;
import io.datavines.server.api.dto.bo.storage.ErrorDataStorageUpdate;
import io.datavines.server.repository.entity.ErrorDataStorage;

import io.datavines.server.repository.mapper.ErrorDataStorageMapper;
import io.datavines.server.repository.service.ErrorDataStorageService;
import io.datavines.server.utils.ContextHolder;
import io.datavines.spi.PluginLoader;
import io.datavines.storage.api.StorageFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static io.datavines.common.log.SensitiveDataConverter.PWD_PATTERN_1;

@Slf4j
@Service("errorDataStorageService")
public class ErrorDataStorageServiceImpl extends ServiceImpl<ErrorDataStorageMapper, ErrorDataStorage> implements ErrorDataStorageService {

    @Override
    public long create(ErrorDataStorageCreate errorDataStorageCreate) throws DataVinesServerException {
        if (isErrorDataStorageExist(errorDataStorageCreate.getName())) {
            throw new DataVinesServerException(Status.ERROR_DATA_STORAGE_EXIST_ERROR, errorDataStorageCreate.getName());
        }
        ErrorDataStorage errorDataStorage = new ErrorDataStorage();
        BeanUtils.copyProperties(errorDataStorageCreate, errorDataStorage);
        errorDataStorage.setCreateBy(ContextHolder.getUserId());
        errorDataStorage.setCreateTime(LocalDateTime.now());
        errorDataStorage.setUpdateBy(ContextHolder.getUserId());
        errorDataStorage.setUpdateTime(LocalDateTime.now());

        if (baseMapper.insert(errorDataStorage) <= 0) {
            log.info("create errorDataStorage fail : {}", errorDataStorageCreate);
            throw new DataVinesServerException(Status.CREATE_ERROR_DATA_STORAGE_ERROR, errorDataStorageCreate.getName());
        }

        return errorDataStorage.getId();
    }

    @Override
    public int update(ErrorDataStorageUpdate errorDataStorageUpdate) throws DataVinesServerException {

        ErrorDataStorage errorDataStorage = getById(errorDataStorageUpdate.getId());
        if ( errorDataStorage == null) {
            throw new DataVinesServerException(Status.ERROR_DATA_STORAGE_NOT_EXIST_ERROR, errorDataStorageUpdate.getName());
        }

        BeanUtils.copyProperties(errorDataStorageUpdate, errorDataStorage);
        errorDataStorage.setUpdateBy(ContextHolder.getUserId());
        errorDataStorage.setUpdateTime(LocalDateTime.now());

        if (baseMapper.updateById(errorDataStorage) <= 0) {
            log.info("update errorDataStorage fail : {}", errorDataStorageUpdate);
            throw new DataVinesServerException(Status.UPDATE_ERROR_DATA_STORAGE_ERROR, errorDataStorageUpdate.getName());
        }

        return 1;
    }

    @Override
    public ErrorDataStorage getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<ErrorDataStorage> listByWorkspaceId(long workspaceId) {
        List<ErrorDataStorage> list = baseMapper.selectList(new QueryWrapper<ErrorDataStorage>().eq("workspace_id", workspaceId));
        if (CollectionUtils.isNotEmpty(list)) {
            list.forEach(errorDataStorage -> {
                errorDataStorage.setParam(PasswordFilterUtils.convertPassword(PWD_PATTERN_1, errorDataStorage.getParam()));
            });
        }

        return list;
    }

    @Override
    public int deleteById(long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public String getConfigJson(String type) {
        return PluginLoader.getPluginLoader(StorageFactory.class).getOrCreatePlugin(type).getStorageConnector().getConfigJson(!LanguageUtils.isZhContext());
    }

    private boolean isErrorDataStorageExist(String name) {
        ErrorDataStorage user = baseMapper.selectOne(new QueryWrapper<ErrorDataStorage>().eq("name", name));
        return user != null;
    }
}
