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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.datavines.common.exception.DataVinesException;
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.core.utils.BeanConvertUtils;
import io.datavines.notification.api.spi.SlasHandlerPlugin;
import io.datavines.server.api.dto.bo.sla.SlaCreate;
import io.datavines.server.api.dto.bo.sla.SlaUpdate;
import io.datavines.server.api.dto.vo.SlaConfigVO;
import io.datavines.server.api.dto.vo.SlaPageVO;
import io.datavines.server.api.dto.vo.JobVO;
import io.datavines.server.api.dto.vo.SlaVO;
import io.datavines.server.repository.entity.Sla;
import io.datavines.server.repository.entity.SlaJob;
import io.datavines.server.repository.mapper.SlaMapper;
import io.datavines.server.repository.service.SlaJobService;
import io.datavines.server.repository.service.SlaService;
import io.datavines.server.utils.ContextHolder;
import io.datavines.spi.PluginLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class SlaServiceImpl extends ServiceImpl<SlaMapper, Sla> implements SlaService {

    @Autowired
    private SlaJobService slaJobService;

    @Override
    public IPage<SlaPageVO> listSlas(Long workspaceId, String searchVal, Integer pageNumber, Integer pageSize) {
        Page<JobVO> page = new Page<>(pageNumber, pageSize);
        IPage<SlaPageVO> res = baseMapper.listSlas(page, workspaceId, searchVal);
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        boolean result = removeById(id);
        LambdaQueryWrapper<SlaJob> lambda = new LambdaQueryWrapper<>();
        lambda.eq(SlaJob::getSlaId, id);
        slaJobService.remove(lambda);
        return result;
    }

    @Override
    public String getSenderConfigJson(String type) {
        return PluginLoader
                .getPluginLoader(SlasHandlerPlugin.class)
                .getOrCreatePlugin(type)
                .getConfigSenderJson();
    }

    @Override
    public Set<String> getSupportPlugin(){
        Set<String> supportedPlugins = PluginLoader
                .getPluginLoader(SlasHandlerPlugin.class)
                .getSupportedPlugins();
        return supportedPlugins;
    }

    @Override
    public Sla createSla(SlaCreate create) {
        String name = create.getName();
        LambdaQueryWrapper<Sla> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Sla::getWorkspaceId, create.getWorkspaceId());
        wrapper.eq(Sla::getName, name);
        Sla existSla = getOne(wrapper);
        if (Objects.nonNull(existSla)){
            throw new DataVinesServerException(Status.SLA_ALREADY_EXIST_ERROR, name);
        }
        Sla sla = BeanConvertUtils.convertBean(create, Sla::new);
        sla.setCreateBy(ContextHolder.getUserId());
        LocalDateTime now = LocalDateTime.now();
        sla.setUpdateBy(ContextHolder.getUserId());
        sla.setUpdateTime(now);
        sla.setCreateTime(now);
        boolean success = save(sla);
        if (!success){
            throw new DataVinesException("create sla error");
        }
        return sla;
    }

    @Override
    public boolean updateSla(SlaUpdate update) {
        LambdaQueryWrapper<Sla> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Sla::getWorkspaceId, update.getWorkspaceId());
        wrapper.eq(Sla::getName, update.getName());
        Sla existSla = getOne(wrapper);
        if (Objects.nonNull(existSla) && !existSla.getId().equals(update.getId())){
            log.error("db has sla {} is same as update {}", existSla, update);
            throw new DataVinesServerException(Status.SLA_ALREADY_EXIST_ERROR, update.getName());
        }
        Sla sla = BeanConvertUtils.convertBean(update, Sla::new);
        sla.setUpdateBy(ContextHolder.getUserId());
        sla.setUpdateTime(LocalDateTime.now());
        boolean save = updateById(sla);
        return save;
    }

    @Override
    public List<SlaVO> getSlaByJobId(Long jobId) {
        return baseMapper.getSlaByJobId(jobId);
    }

    @Override
    public List<SlaConfigVO> getSlaConfigByJobId(Long jobId) {
        return baseMapper.getSlaConfigByJobId(jobId);
    }
}
