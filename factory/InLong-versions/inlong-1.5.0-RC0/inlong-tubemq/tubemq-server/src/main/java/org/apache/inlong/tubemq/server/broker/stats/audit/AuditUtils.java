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

package org.apache.inlong.tubemq.server.broker.stats.audit;

import org.apache.inlong.audit.AuditOperator;
import org.apache.inlong.audit.util.AuditConfig;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.broker.stats.TrafficInfo;
import org.apache.inlong.tubemq.server.common.fileconfig.ADConfig;

import java.util.Map;

/**
 * AuditUtils
 *
 * A wrapper class for Audit report operations
 */
public class AuditUtils {

    private static ADConfig auditConfig = new ADConfig();

    /**
     * init audit instance
     *
     * @param adConfig the initial configure
     */
    public static void initAudit(ADConfig adConfig) {
        // check whether enable audit
        if (adConfig == null || !adConfig.isAuditEnable()) {
            return;
        }
        // set audit configure
        auditConfig = adConfig;

        // initial audit instance
        AuditOperator.getInstance().setAuditProxy(adConfig.getAuditProxyAddrSet());
        AuditConfig auditConfig =
                new AuditConfig(adConfig.getAuditCacheFilePath(),
                        adConfig.getAuditCacheMaxRows());
        AuditOperator.getInstance().setAuditConfig(auditConfig);
    }

    /**
     * add produce record
     *
     * @param groupId the group id
     * @param streamId the stream id
     * @param logTime the record time
     * @param count the record count
     * @param size the record size
     */
    public static void addProduceRecord(String groupId, String streamId, String logTime, long count, long size) {
        if (!auditConfig.isAuditEnable()) {
            return;
        }
        AuditOperator.getInstance().add(auditConfig.getAuditIdProduce(),
                groupId, streamId, DateTimeConvertUtils.yyyyMMddHHmm2ms(logTime), count, size);
    }

    /**
     * add consume record
     *
     * @param trafficInfos the consumed traffic information
     */
    public static void addConsumeRecord(Map<String, TrafficInfo> trafficInfos) {
        if (!auditConfig.isAuditEnable() || trafficInfos == null || trafficInfos.isEmpty()) {
            return;
        }
        for (Map.Entry<String, TrafficInfo> entry : trafficInfos.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String statKey = entry.getKey();
            String[] statKeyItems = statKey.split(TokenConstants.SEGMENT_SEP, -1);
            if (statKeyItems.length < 8) {
                continue;
            }
            if (TStringUtils.isEmpty(statKeyItems[0])) {
                continue;
            }
            // like: test_1#127.0.0.1#test_consume_127.0.0.1-32677-1656672066382-1-Pull-3.9.2
            // #127.0.0.1#32677#test_consume#2#202207041219
            // topicName, brokerIP, clientId,
            // clientIP, client processId, consume group, partitionId, msgTime
            AuditOperator.getInstance().add(auditConfig.getAuditIdConsume(),
                    statKeyItems[0], statKeyItems[5], DateTimeConvertUtils.yyyyMMddHHmm2ms(statKeyItems[7]),
                    entry.getValue().getMsgCount(), entry.getValue().getMsgSize());
        }
    }

    /**
     * Close audit, if it was enabled, send its data first.
     */
    public static void closeAudit() {
        if (!auditConfig.isAuditEnable()) {
            return;
        }
        AuditOperator.getInstance().send();
    }
}
