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
package io.datavines.notification.core;

import io.datavines.common.exception.DataVinesException;
import io.datavines.notification.api.entity.SlaNotificationMessage;
import io.datavines.notification.api.entity.SlaNotificationResult;
import io.datavines.notification.api.entity.SlaConfigMessage;
import io.datavines.notification.api.entity.SlaSenderMessage;
import io.datavines.notification.api.spi.SlasHandlerPlugin;
import io.datavines.spi.PluginLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class NotificationManager {

    private Set<String> supportedPlugins;

    public NotificationManager(){
        supportedPlugins = PluginLoader
                .getPluginLoader(SlasHandlerPlugin.class)
                .getSupportedPlugins();
    }

    public SlaNotificationResult notify(SlaNotificationMessage slaNotificationMessage, Map<SlaSenderMessage, Set<SlaConfigMessage>> config){
        if (config == null || config.isEmpty()){
            throw new DataVinesException("message cannot be send without sender and receiver");
        }
        SlaNotificationResult result = new SlaNotificationResult();
        result.setStatus(true);

        for (Map.Entry<SlaSenderMessage, Set<SlaConfigMessage>> entry: config.entrySet()) {
            String type = entry.getKey().getType();
            if (!supportedPlugins.contains(type)) {
                throw new DataVinesException("sender type not support of "+ type);
            }
            SlasHandlerPlugin handlerPlugin = PluginLoader
                    .getPluginLoader(SlasHandlerPlugin.class)
                    .getOrCreatePlugin(type);
            Map<SlaSenderMessage, Set<SlaConfigMessage>> senderEntity = new HashMap(){
                {
                    put(entry.getKey(), entry.getValue());
                }
            };
            SlaNotificationResult entryResult = handlerPlugin.notify(slaNotificationMessage, senderEntity);
            result.merge(entryResult);
        }
        return result;
    }

}
