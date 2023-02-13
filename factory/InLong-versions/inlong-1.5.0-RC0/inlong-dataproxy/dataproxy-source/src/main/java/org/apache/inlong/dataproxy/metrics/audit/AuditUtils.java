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

package org.apache.inlong.dataproxy.metrics.audit;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flume.Event;
import org.apache.inlong.audit.AuditOperator;
import org.apache.inlong.audit.util.AuditConfig;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.dataproxy.config.holder.CommonPropertiesHolder;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItem;
import org.apache.inlong.dataproxy.utils.Constants;
import org.apache.inlong.dataproxy.utils.InLongMsgVer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * Audit utils
 */
public class AuditUtils {

    public static final String AUDIT_KEY_FILE_PATH = "audit.filePath";
    public static final String AUDIT_DEFAULT_FILE_PATH = "/data/inlong/audit/";
    public static final String AUDIT_KEY_MAX_CACHE_ROWS = "audit.maxCacheRows";
    public static final int AUDIT_DEFAULT_MAX_CACHE_ROWS = 2000000;
    public static final String AUDIT_KEY_PROXYS = "audit.proxys";
    public static final String AUDIT_KEY_IS_AUDIT = "audit.enable";

    public static final int AUDIT_ID_DATAPROXY_READ_SUCCESS = 5;
    public static final int AUDIT_ID_DATAPROXY_SEND_SUCCESS = 6;

    private static boolean IS_AUDIT = true;

    /**
     * Init audit
     */
    public static void initAudit() {
        // IS_AUDIT
        IS_AUDIT = BooleanUtils.toBoolean(CommonPropertiesHolder.getString(AUDIT_KEY_IS_AUDIT));
        if (IS_AUDIT) {
            // AuditProxy
            String strIpPorts = CommonPropertiesHolder.getString(AUDIT_KEY_PROXYS);
            HashSet<String> proxys = new HashSet<>();
            if (!StringUtils.isBlank(strIpPorts)) {
                String[] ipPorts = strIpPorts.split("\\s+");
                Collections.addAll(proxys, ipPorts);
            }
            AuditOperator.getInstance().setAuditProxy(proxys);
            // AuditConfig
            String filePath = CommonPropertiesHolder.getString(AUDIT_KEY_FILE_PATH, AUDIT_DEFAULT_FILE_PATH);
            int maxCacheRow = NumberUtils.toInt(
                    CommonPropertiesHolder.getString(AUDIT_KEY_MAX_CACHE_ROWS),
                    AUDIT_DEFAULT_MAX_CACHE_ROWS);
            AuditConfig auditConfig = new AuditConfig(filePath, maxCacheRow);
            AuditOperator.getInstance().setAuditConfig(auditConfig);
        }
    }

    /**
     * Add audit data
     */
    public static void add(int auditID, Event event) {
        if (!IS_AUDIT || event == null) {
            return;
        }
        Map<String, String> headers = event.getHeaders();
        String pkgVersion = headers.get(ConfigConstants.MSG_ENCODE_VER);
        if (InLongMsgVer.INLONG_V1.getName().equalsIgnoreCase(pkgVersion)) {
            String inlongGroupId = DataProxyMetricItem.getInlongGroupId(headers);
            String inlongStreamId = DataProxyMetricItem.getInlongStreamId(headers);
            long logTime = getLogTime(headers);
            long msgCount = 1L;
            if (event.getHeaders().containsKey(ConfigConstants.MSG_COUNTER_KEY)) {
                msgCount = Long.parseLong(event.getHeaders().get(ConfigConstants.MSG_COUNTER_KEY));
            }
            AuditOperator.getInstance().add(auditID, inlongGroupId,
                    inlongStreamId, logTime, msgCount, event.getBody().length);
        } else {
            String groupId = headers.get(AttributeConstants.GROUP_ID);
            String streamId = headers.get(AttributeConstants.STREAM_ID);
            long dataTime = NumberUtils.toLong(headers.get(AttributeConstants.DATA_TIME));
            long msgCount = NumberUtils.toLong(headers.get(ConfigConstants.MSG_COUNTER_KEY));
            AuditOperator.getInstance().add(auditID, groupId,
                    streamId, dataTime, msgCount, event.getBody().length);
        }
    }

    /**
     * Get LogTime from headers
     */
    public static long getLogTime(Map<String, String> headers) {
        String strLogTime = headers.get(Constants.HEADER_KEY_MSG_TIME);
        if (strLogTime == null) {
            strLogTime = headers.get(AttributeConstants.DATA_TIME);
        }
        if (strLogTime == null) {
            return System.currentTimeMillis();
        }
        long logTime = NumberUtils.toLong(strLogTime, 0);
        if (logTime == 0) {
            logTime = System.currentTimeMillis();
        }
        return logTime;
    }

    /**
     * Get LogTime from event
     */
    public static long getLogTime(Event event) {
        if (event != null) {
            Map<String, String> headers = event.getHeaders();
            return getLogTime(headers);
        }
        return System.currentTimeMillis();
    }

    /**
     * Get AuditFormatTime
     */
    public static long getAuditFormatTime(long msgTime) {
        return msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
    }

    /**
     * Send audit data
     */
    public static void send() {
        AuditOperator.getInstance().send();
    }
}
