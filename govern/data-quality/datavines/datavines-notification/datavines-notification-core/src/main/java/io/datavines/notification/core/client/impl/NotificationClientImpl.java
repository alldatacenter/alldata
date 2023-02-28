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
package io.datavines.notification.core.client.impl;

import io.datavines.notification.api.entity.SlaNotificationMessage;
import io.datavines.notification.api.entity.SlaNotificationResult;
import io.datavines.notification.api.entity.SlaConfigMessage;
import io.datavines.notification.api.entity.SlaSenderMessage;
import io.datavines.notification.core.NotificationManager;
import io.datavines.notification.core.client.NotificationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class NotificationClientImpl implements NotificationClient {

    @Autowired
    private NotificationManager notificationManager;

    @Override
    public SlaNotificationResult notify(SlaNotificationMessage slaNotificationMessage, Map<SlaSenderMessage, Set<SlaConfigMessage>> config) {
        SlaNotificationResult result = notificationManager.notify(slaNotificationMessage, config);
        return result;
    }



}
