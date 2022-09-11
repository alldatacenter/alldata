/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.  You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.inlong.dataproxy.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.flume.Event;
import org.apache.inlong.dataproxy.consts.AttributeConstants;
import org.apache.inlong.dataproxy.source.MsgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageUtils {

    private static final Logger logger = LoggerFactory.getLogger(MessageUtils.class);

    /**
     *  is or not sync send for order message
     * @param syncSend syncSend
     * @return true/false
     */
    public static boolean isSyncSendForOrder(String syncSend) {
        if (StringUtils.isNotEmpty(syncSend) && "true".equals(syncSend)) {
            return true;
        }
        return false;
    }

    /**
     *  is or not sync send for order message
     * @param event event
     * @return true/false
     */
    public static boolean isSyncSendForOrder(Event event) {
        String syncSend = event.getHeaders().get(AttributeConstants.MESSAGE_SYNC_SEND);
        return isSyncSendForOrder(syncSend);
    }

    /**
     * Convert String to ByteBuf
     *
     * @param backattrs
     * @param msgType message type
     * @param sequenceId sequence Id
     * @return ByteBuf
     */
    public static ByteBuf getResponsePackage(String backattrs, MsgType msgType, String sequenceId) {
        int binTotalLen = 1 + 4 + 2 + 2;
        if (null != backattrs) {
            binTotalLen += backattrs.length();
        }
        ByteBuf binBuffer = ByteBufAllocator.DEFAULT.buffer(4 + binTotalLen);
        binBuffer.writeInt(binTotalLen);
        binBuffer.writeByte(msgType.getValue());

        long uniqVal = Long.parseLong(sequenceId);
        byte[] uniq = new byte[4];
        uniq[0] = (byte) ((uniqVal >> 24) & 0xFF);
        uniq[1] = (byte) ((uniqVal >> 16) & 0xFF);
        uniq[2] = (byte) ((uniqVal >> 8) & 0xFF);
        uniq[3] = (byte) (uniqVal & 0xFF);
        binBuffer.writeBytes(uniq);

        if (null != backattrs) {
            binBuffer.writeShort(backattrs.length());
            binBuffer.writeBytes(backattrs.getBytes(StandardCharsets.UTF_8));
        } else {
            binBuffer.writeShort(0x0);
        }
        binBuffer.writeShort(0xee01);
        return binBuffer;
    }

    /**
     * get topic
     */
    public static String getTopic(Map<String, String> topicsMap, String groupId, String streamId) {
        String topic = null;
        if (topicsMap != null && StringUtils.isNotEmpty(groupId)) {
            if (StringUtils.isNotEmpty(streamId)) {
                topic = topicsMap.get(groupId + "/" + streamId);
            }
            if (StringUtils.isEmpty(topic)) {
                topic = topicsMap.get(groupId);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Get topic by groupId = {}, streamId = {}, topic = {}", groupId, streamId,
                    topic);
        }
        return topic;
    }

}
