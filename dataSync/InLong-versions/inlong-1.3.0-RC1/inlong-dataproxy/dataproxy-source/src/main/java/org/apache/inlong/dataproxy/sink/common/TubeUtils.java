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
import org.apache.inlong.dataproxy.config.pojo.MQClusterConfig;
import org.apache.inlong.dataproxy.consts.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.utils.Constants;
import org.apache.inlong.dataproxy.utils.NetworkUtils;
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
     * @param addExtraAttrs  whether to add extra attributes
     * @return   the message object
     */
    public static Message buildMessage(String topicName,
                                       Event event, boolean addExtraAttrs) {
        Message message = new Message(topicName, event.getBody());
        message.setAttrKeyVal("dataproxyip", NetworkUtils.getLocalIp());
        String streamId = "";
        if (event.getHeaders().containsKey(AttributeConstants.STREAM_ID)) {
            streamId = event.getHeaders().get(AttributeConstants.STREAM_ID);
        } else if (event.getHeaders().containsKey(AttributeConstants.INAME)) {
            streamId = event.getHeaders().get(AttributeConstants.INAME);
        }
        message.putSystemHeader(streamId, event.getHeaders().get(ConfigConstants.PKG_TIME_KEY));
        if (addExtraAttrs) {
            // common attributes
            Map<String, String> headers = event.getHeaders();
            message.setAttrKeyVal(Constants.INLONG_GROUP_ID, headers.get(Constants.INLONG_GROUP_ID));
            message.setAttrKeyVal(Constants.INLONG_STREAM_ID, headers.get(Constants.INLONG_STREAM_ID));
            message.setAttrKeyVal(Constants.TOPIC, headers.get(Constants.TOPIC));
            message.setAttrKeyVal(Constants.HEADER_KEY_MSG_TIME, headers.get(Constants.HEADER_KEY_MSG_TIME));
            message.setAttrKeyVal(Constants.HEADER_KEY_SOURCE_IP, headers.get(Constants.HEADER_KEY_SOURCE_IP));
        }
        return message;
    }
}
