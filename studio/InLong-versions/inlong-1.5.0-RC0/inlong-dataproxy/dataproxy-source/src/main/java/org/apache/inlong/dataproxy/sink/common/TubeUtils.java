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

package org.apache.inlong.dataproxy.sink.common;

import java.util.Map;
import org.apache.flume.Event;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.dataproxy.config.pojo.MQClusterConfig;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.utils.Constants;
import org.apache.inlong.dataproxy.utils.DateTimeUtils;
import org.apache.inlong.dataproxy.utils.InLongMsgVer;
import org.apache.inlong.dataproxy.utils.MessageUtils;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.corebase.Message;

public class TubeUtils {

    /**
     * Build TubeMQ's client configure
     *
     * @param clusterAddr    the TubeMQ cluster address
     * @param tubeConfig     the TubeMQ cluster configure
     * @return   the TubeClientConfig object
     */
    public static TubeClientConfig buildClientConfig(String clusterAddr, MQClusterConfig tubeConfig) {
        final TubeClientConfig tubeClientConfig = new TubeClientConfig(clusterAddr);
        tubeClientConfig.setLinkMaxAllowedDelayedMsgCount(tubeConfig.getLinkMaxAllowedDelayedMsgCount());
        tubeClientConfig.setSessionWarnDelayedMsgCount(tubeConfig.getSessionWarnDelayedMsgCount());
        tubeClientConfig.setSessionMaxAllowedDelayedMsgCount(tubeConfig.getSessionMaxAllowedDelayedMsgCount());
        tubeClientConfig.setNettyWriteBufferHighWaterMark(tubeConfig.getNettyWriteBufferHighWaterMark());
        tubeClientConfig.setHeartbeatPeriodMs(tubeConfig.getTubeHeartbeatPeriodMs());
        tubeClientConfig.setRpcTimeoutMs(tubeConfig.getTubeRpcTimeoutMs());
        return tubeClientConfig;
    }

    /**
     * Build TubeMQ's message
     *
     * @param topicName      the topic name of message
     * @param event          the DataProxy event
     * @return   the message object
     */
    public static Message buildMessage(String topicName, Event event) {
        Map<String, String> headers = event.getHeaders();
        Message message = new Message(topicName, event.getBody());
        String pkgVersion = headers.get(ConfigConstants.MSG_ENCODE_VER);
        if (InLongMsgVer.INLONG_V1.getName().equalsIgnoreCase(pkgVersion)) {
            long dataTimeL = Long.parseLong(headers.get(ConfigConstants.PKG_TIME_KEY));
            message.putSystemHeader(headers.get(Constants.INLONG_STREAM_ID),
                    DateTimeUtils.ms2yyyyMMddHHmm(dataTimeL));
        } else {
            long dataTimeL = Long.parseLong(headers.get(AttributeConstants.DATA_TIME));
            message.putSystemHeader(headers.get(AttributeConstants.STREAM_ID),
                    DateTimeUtils.ms2yyyyMMddHHmm(dataTimeL));
        }
        Map<String, String> extraAttrMap = MessageUtils.getXfsAttrs(headers, pkgVersion);
        for (Map.Entry<String, String> entry : extraAttrMap.entrySet()) {
            message.setAttrKeyVal(entry.getKey(), entry.getValue());
        }
        return message;
    }
}
