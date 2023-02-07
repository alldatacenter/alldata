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

package org.apache.inlong.tubemq.client.producer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corebase.utils.SettingValidUtils;

/**
 * The class caches the max msg size settings
 *  returned from the server.
 */
public class MaxMsgSizeHolder {

    private final AtomicLong configId =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private final AtomicInteger defMaxMsgSize =
            new AtomicInteger(TBaseConstants.META_MAX_MESSAGE_DATA_SIZE);
    private Map<String, Integer> topicMaxSizeInBMap = new ConcurrentHashMap<>();

    public MaxMsgSizeHolder() {

    }

    // set master returned configure
    public void updAllowedSetting(ClientMaster.ApprovedClientConfig allowedConfig) {
        if (allowedConfig != null) {
            if (configId.get() != allowedConfig.getConfigId()) {
                configId.set(allowedConfig.getConfigId());
            }
            if (allowedConfig.hasMaxMsgSize()
                    && allowedConfig.getMaxMsgSize() != defMaxMsgSize.get()) {
                defMaxMsgSize.set(SettingValidUtils.validAndGetMaxMsgSizeInB(
                        allowedConfig.getMaxMsgSize()));
            }
        }
    }

    public void updTopicMaxSizeInB(Map<String, Integer> topicMaxSizeInBMap) {
        this.topicMaxSizeInBMap = topicMaxSizeInBMap;
    }

    public long getConfigId() {
        return configId.get();
    }

    public int getDefMaxMsgSize(String topicName) {
        Integer maxMsgSizeInB = topicMaxSizeInBMap.get(topicName);
        return maxMsgSizeInB == null ? defMaxMsgSize.get() : maxMsgSizeInB;
    }
}
