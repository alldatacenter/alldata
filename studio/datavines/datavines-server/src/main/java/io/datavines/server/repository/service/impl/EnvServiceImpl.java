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
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.api.dto.bo.env.EnvCreate;
import io.datavines.server.api.dto.bo.env.EnvUpdate;
import io.datavines.server.repository.entity.Env;
import io.datavines.server.repository.mapper.EnvMapper;
import io.datavines.server.repository.service.EnvService;
import io.datavines.server.utils.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service("envService")
public class EnvServiceImpl extends ServiceImpl<EnvMapper, Env> implements EnvService {

    @Override
    public long create(EnvCreate envCreate) throws DataVinesServerException {
        if (isEnvExist(envCreate.getEnv())) {
            throw new DataVinesServerException(Status.ENV_EXIST_ERROR, envCreate.getEnv());
        }
        Env env = new Env();
        BeanUtils.copyProperties(envCreate, env);
        env.setCreateBy(ContextHolder.getUserId());
        env.setCreateTime(LocalDateTime.now());
        env.setUpdateBy(ContextHolder.getUserId());
        env.setUpdateTime(LocalDateTime.now());

        if (baseMapper.insert(env) <= 0) {
            log.info("create env fail : {}", envCreate);
            throw new DataVinesServerException(Status.CREATE_ENV_ERROR, envCreate.getEnv());
        }

        return env.getId();
    }

    @Override
    public int update(EnvUpdate envUpdate) throws DataVinesServerException {

        Env env = getById(envUpdate.getId());
        if ( env == null) {
            throw new DataVinesServerException(Status.ENV_NOT_EXIST_ERROR, envUpdate.getEnv());
        }

        BeanUtils.copyProperties(envUpdate, env);
        env.setUpdateBy(ContextHolder.getUserId());
        env.setUpdateTime(LocalDateTime.now());

        if (baseMapper.updateById(env) <= 0) {
            log.info("update env fail : {}", envUpdate);
            throw new DataVinesServerException(Status.UPDATE_ENV_ERROR, envUpdate.getEnv());
        }

        return 1;
    }

    @Override
    public Env getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<Env> listByWorkspaceId(long workspaceId) {
        return baseMapper.selectList(new QueryWrapper<Env>().eq("workspace_id", workspaceId));
    }

    @Override
    public int deleteById(long id) {
        return baseMapper.deleteById(id);
    }

    private boolean isEnvExist(String name) {
        Env user = baseMapper.selectOne(new QueryWrapper<Env>().eq("name", name));
        return user != null;
    }
}
