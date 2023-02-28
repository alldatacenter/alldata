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
import io.datavines.core.utils.BeanConvertUtils;
import io.datavines.notification.api.entity.SlaConfigMessage;
import io.datavines.notification.api.entity.SlaSenderMessage;
import io.datavines.notification.api.spi.SlasHandlerPlugin;
import io.datavines.server.api.dto.bo.sla.SlaNotificationCreate;
import io.datavines.server.api.dto.bo.sla.SlaNotificationUpdate;
import io.datavines.server.repository.entity.SlaJob;
import io.datavines.server.repository.entity.SlaNotification;
import io.datavines.server.repository.entity.SlaSender;
import io.datavines.server.repository.mapper.SlaNotificationMapper;
import io.datavines.server.repository.service.SlaJobService;
import io.datavines.server.repository.service.SlaNotificationService;
import io.datavines.server.repository.service.SlaSenderService;
import io.datavines.server.utils.ContextHolder;
import io.datavines.spi.PluginLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SlaNotificationServiceImpl extends ServiceImpl<SlaNotificationMapper, SlaNotification> implements SlaNotificationService {

    @Autowired
    private SlaSenderService slaSenderService;

    @Autowired
    private SlaJobService slaJobService;

    /**
     * get sla sender and receiver configuration from db by slaId. it will return empty Map if sla not association with sender and receiver
     * @param slaId
     * @return
     */
    @Override
    public Map<SlaSenderMessage, Set<SlaConfigMessage>> getSlasNotificationConfigurationBySlasId(Long slaId){
        //get all notification config
        LambdaQueryWrapper<SlaNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlaNotification::getSlaId, slaId);
        List<SlaNotification> slaNotificationList = list(wrapper);
        if (CollectionUtils.isEmpty(slaNotificationList)){
            return new HashMap<>();
        }
        //make receiverSet and senderSet
        Set<Long> senderSet = slaNotificationList.stream().map(SlaNotification::getSenderId).collect(Collectors.toSet());
        List<SlaSender> slaSenders = slaSenderService.listByIds(senderSet);
        Map<Long, SlaSenderMessage> senderMap = slaSenders
                .stream()
                .map(x -> BeanConvertUtils.convertBean(x, SlaSenderMessage::new))
                .collect(Collectors.toMap(SlaSenderMessage::getId, x -> x));
        HashMap<SlaSenderMessage, Set<SlaConfigMessage>> result = new HashMap<>();
        for(SlaNotification entity: slaNotificationList){
            Long senderId = entity.getSenderId();
            SlaSenderMessage slaSender = senderMap.get(senderId);
            Set<SlaConfigMessage> existSet = result.getOrDefault(slaSender, new HashSet<>());
            SlaConfigMessage configMessage = new SlaConfigMessage();
            configMessage.setType(entity.getType());
            configMessage.setConfig(entity.getConfig());
            existSet.add(configMessage);
            result.putIfAbsent(slaSender, existSet);
        }
        return result;
    }

    @Override
    public Map<SlaSenderMessage, Set<SlaConfigMessage>> getSlasNotificationConfigurationByJobId(Long jobId) {
        LambdaQueryWrapper<SlaJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlaJob::getJobId, jobId);
        SlaJob one = slaJobService.getOne(wrapper);
        if (Objects.isNull(one)){
            return new HashMap<>();
        }
        Long slaId = one.getSlaId();
        return getSlasNotificationConfigurationBySlasId(slaId);
    }

    @Override
    public Set<SlaConfigMessage> listReceiverMessageBySlaId(Long id){
        return baseMapper.listReceiverMessageBySlaId(id);
    }

    @Override
    public String getConfigJson(String type) {
        return PluginLoader
                .getPluginLoader(SlasHandlerPlugin.class)
                .getOrCreatePlugin(type)
                .getConfigJson();
    }

    @Override
    public IPage<SlaNotification> pageListNotification(IPage<SlaNotification> page, Long workspaceId, String searchVal) {
        return baseMapper.pageListNotification(page, workspaceId, searchVal);
    }

    @Override
    public SlaNotification createNotification(SlaNotificationCreate create) {
        SlaNotification bean = BeanConvertUtils.convertBean(create, SlaNotification::new);
        bean.setCreateBy(ContextHolder.getUserId());
        LocalDateTime now = LocalDateTime.now();
        bean.setCreateTime(now);
        bean.setUpdateTime(now);
        bean.setUpdateBy(ContextHolder.getUserId());
        boolean success = save(bean);
        if (!success){
            throw new DataVinesException("create sender error");
        }
        return bean;
    }

    @Override
    public SlaNotification updateNotification(SlaNotificationUpdate update) {
        SlaNotification bean = BeanConvertUtils.convertBean(update, SlaNotification::new);
        bean.setCreateBy(ContextHolder.getUserId());
        LocalDateTime now = LocalDateTime.now();
        bean.setCreateTime(now);
        bean.setUpdateTime(now);
        bean.setUpdateBy(ContextHolder.getUserId());
        boolean success = updateById(bean);
        if (!success){
            throw new DataVinesException("update sender error");
        }
        return bean;
    }

    @Override
    public IPage<SlaNotification> pageListNotification(Long workspaceId, String searchVal, Integer pageNumber, Integer pageSize) {
        LambdaQueryWrapper<SlaNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlaNotification::getWorkspaceId, workspaceId);
        Page<SlaNotification> page = new Page<>(pageNumber, pageSize);
        IPage<SlaNotification> result = pageListNotification(page, workspaceId, searchVal);
        return result;
    }
}
