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
package io.datavines.server.repository.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import io.datavines.notification.api.entity.SlaConfigMessage;
import io.datavines.notification.api.entity.SlaSenderMessage;
import io.datavines.server.api.dto.bo.sla.SlaNotificationCreate;
import io.datavines.server.api.dto.bo.sla.SlaNotificationUpdate;
import io.datavines.server.repository.entity.SlaNotification;

import java.util.Map;
import java.util.Set;

public interface SlaNotificationService extends IService<SlaNotification>{

    Map<SlaSenderMessage, Set<SlaConfigMessage>> getSlasNotificationConfigurationBySlasId(Long slaId);

    Map<SlaSenderMessage, Set<SlaConfigMessage>> getSlasNotificationConfigurationByJobId(Long job);

    String getConfigJson(String type);

    SlaNotification createNotification(SlaNotificationCreate create);

    SlaNotification updateNotification(SlaNotificationUpdate update);

    IPage<SlaNotification> pageListNotification(Long workspaceId,  String searchVal, Integer pageNumber, Integer pageSize);

    Set<SlaConfigMessage> listReceiverMessageBySlaId(Long id);

    IPage<SlaNotification> pageListNotification(IPage<SlaNotification> page, Long workspaceId, String searchVal);
}
