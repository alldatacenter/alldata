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

package org.apache.inlong.dataproxy.utils;

import static org.apache.inlong.common.util.NetworkUtils.getLocalIp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flume.Event;
import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.common.monitor.LogCounter;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.common.util.NetworkUtils;
import org.apache.inlong.dataproxy.base.SinkRspEvent;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.common.msg.MsgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageUtils {

    // log print count
    private static final LogCounter logCounter =
            new LogCounter(10, 100000, 30 * 1000);

    private static final Logger logger = LoggerFactory.getLogger(MessageUtils.class);

    /**
     *  is or not sync send for order message
     * @param event  event object
     * @return true/false
     */
    public static boolean isSyncSendForOrder(Event event) {
        String syncSend = event.getHeaders().get(AttributeConstants.MESSAGE_SYNC_SEND);
        return StringUtils.isNotEmpty(syncSend) && "true".equalsIgnoreCase(syncSend);
    }

    public static boolean isSinkRspType(Event event) {
        return isSinkRspType(event.getHeaders());
    }

    public static boolean isSinkRspType(Map<String, String> attrMap) {
        String proxySend = attrMap.get(AttributeConstants.MESSAGE_PROXY_SEND);
        String syncSend = attrMap.get(AttributeConstants.MESSAGE_SYNC_SEND);
        return (StringUtils.isNotEmpty(proxySend) && "true".equalsIgnoreCase(proxySend))
                || (StringUtils.isNotEmpty(syncSend) && "true".equalsIgnoreCase(syncSend));
    }

    public static Pair<Boolean, String> getEventProcType(Event event) {
        String syncSend = event.getHeaders().get(AttributeConstants.MESSAGE_SYNC_SEND);
        String proxySend = event.getHeaders().get(AttributeConstants.MESSAGE_PROXY_SEND);
        return getEventProcType(syncSend, proxySend);
    }

    public static Pair<Boolean, String> getEventProcType(String syncSend, String proxySend) {
        boolean isOrderOrProxy = false;
        String msgProcType = "b2b";
        if (StringUtils.isNotEmpty(syncSend) && "true".equalsIgnoreCase(syncSend)) {
            isOrderOrProxy = true;
            msgProcType = "order";
        }
        if (StringUtils.isNotEmpty(proxySend) && "true".equalsIgnoreCase(proxySend)) {
            isOrderOrProxy = true;
            msgProcType = "proxy";
        }
        return Pair.of(isOrderOrProxy, msgProcType);
    }

    /**
     *  Return response to client in source
     * @param commonAttrMap attribute map
     * @param resultMap     result map
     * @param remoteChannel client channel
     * @param msgType       the message type
     */
    public static void sourceReturnRspPackage(Map<String, String> commonAttrMap,
            Map<String, Object> resultMap,
            Channel remoteChannel,
            MsgType msgType) throws Exception {
        ByteBuf binBuffer;
        String origAttrs = null;
        final StringBuilder strBuff = new StringBuilder(512);
        // check channel and msg type
        if (remoteChannel == null
                || MsgType.MSG_UNKNOWN.equals(msgType)) {
            if (logCounter.shouldPrint()) {
                if (remoteChannel == null) {
                    logger.warn("remoteChannel == null, discard it!", remoteChannel);
                } else {
                    logger.warn("Unknown msgType message from {}, discard it!", remoteChannel);
                }
            }
            return;
        }
        // build message bytes
        if (MsgType.MSG_HEARTBEAT.equals(msgType)) {
            binBuffer = buildHeartBeatMsgRspPackage();
        } else {
            // check whether return response message
            String isAck = commonAttrMap.get(AttributeConstants.MESSAGE_IS_ACK);
            if ("false".equalsIgnoreCase(isAck)) {
                return;
            }
            origAttrs = (String) resultMap.get(ConfigConstants.DECODER_ATTRS);
            // check whether channel is writable
            if (!remoteChannel.isWritable()) {
                strBuff.append("Send buffer is full1 by channel ")
                        .append(remoteChannel).append(", attr is ").append(origAttrs);
                if (logCounter.shouldPrint()) {
                    logger.warn(strBuff.toString());
                }
                throw new Exception(strBuff.toString());
            }
            // build return attribute string
            strBuff.append(ConfigConstants.DATAPROXY_IP_KEY)
                    .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(getLocalIp());
            String errCode = commonAttrMap.get(AttributeConstants.MESSAGE_PROCESS_ERRCODE);
            if (StringUtils.isNotEmpty(errCode)) {
                strBuff.append(AttributeConstants.SEPARATOR)
                        .append(AttributeConstants.MESSAGE_PROCESS_ERRCODE)
                        .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(errCode);
                String errMsg = commonAttrMap.get(AttributeConstants.MESSAGE_PROCESS_ERRMSG);
                if (StringUtils.isNotEmpty(errMsg)) {
                    strBuff.append(AttributeConstants.SEPARATOR)
                            .append(AttributeConstants.MESSAGE_PROCESS_ERRMSG)
                            .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(errMsg);
                }
            }
            if (StringUtils.isNotEmpty(origAttrs)) {
                strBuff.append(AttributeConstants.SEPARATOR).append(origAttrs);
            }
            String destAttrs = strBuff.toString();
            // build response message bytes
            if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
                binBuffer = buildBinMsgRspPackage(destAttrs,
                        commonAttrMap.get(AttributeConstants.UNIQ_ID));
            } else if (MsgType.MSG_BIN_HEARTBEAT.equals(msgType)) {
                binBuffer = buildHBRspPackage(destAttrs,
                        (Byte) resultMap.get(ConfigConstants.VERSION_TYPE), 0);
            } else {
                // MsgType.MSG_ACK_SERVICE.equals(msgType)
                // MsgType.MSG_ORIGINAL_RETURN.equals(msgType)
                // MsgType.MSG_MULTI_BODY.equals(msgType)
                // MsgType.MSG_MULTI_BODY_ATTR.equals(msgType)
                binBuffer = buildDefMsgRspPackage(msgType, destAttrs);
            }
        }
        // send response message
        if (remoteChannel.isWritable()) {
            remoteChannel.writeAndFlush(binBuffer);
        } else {
            // release allocated ByteBuf
            binBuffer.release();
            strBuff.delete(0, strBuff.length());
            strBuff.append("Send buffer is full2 by channel ")
                    .append(remoteChannel).append(", attr is ").append(origAttrs);
            if (logCounter.shouldPrint()) {
                logger.warn(strBuff.toString());
            }
            throw new Exception(strBuff.toString());
        }
    }

    /**
     *  Return response to client in sink
     * @param event    the event need to response
     * @param errCode  process error code
     * @param errMsg   error message
     */
    public static void sinkReturnRspPackage(SinkRspEvent event,
            DataProxyErrCode errCode,
            String errMsg) {
        ByteBuf binBuffer;
        final StringBuilder strBuff = new StringBuilder(512);
        // get and check channel context
        ChannelHandlerContext ctx = event.getCtx();
        if (ctx == null || ctx.channel() == null || !ctx.channel().isActive()) {
            return;
        }
        Channel remoteChannel = ctx.channel();
        // check message type
        MsgType msgType = event.getMsgType();
        if (MsgType.MSG_UNKNOWN.equals(msgType)
                || MsgType.MSG_ACK_SERVICE.equals(msgType)
                || MsgType.MSG_HEARTBEAT.equals(msgType)
                || MsgType.MSG_BIN_HEARTBEAT.equals(msgType)) {
            return;
        }
        // check whether return response message
        Map<String, String> attrMap = event.getHeaders();
        String isAck = attrMap.get(AttributeConstants.MESSAGE_IS_ACK);
        if ("false".equalsIgnoreCase(isAck)) {
            return;
        }
        String origAttrs = attrMap.get(ConfigConstants.DECODER_ATTRS);
        // check whether channel is writable
        if (!remoteChannel.isWritable()) {
            if (logCounter.shouldPrint()) {
                logger.warn(strBuff.append("Send buffer is full3 by channel ")
                        .append(remoteChannel).append(", attr is ").append(origAttrs).toString());
            }
            return;
        }
        // build return attribute string
        strBuff.append(ConfigConstants.DATAPROXY_IP_KEY)
                .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(getLocalIp());
        if (DataProxyErrCode.SUCCESS != errCode) {
            strBuff.append(AttributeConstants.SEPARATOR)
                    .append(AttributeConstants.MESSAGE_PROCESS_ERRCODE)
                    .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(errCode.getErrCode());
            if (StringUtils.isNotEmpty(errMsg)) {
                strBuff.append(AttributeConstants.SEPARATOR)
                        .append(AttributeConstants.MESSAGE_PROCESS_ERRMSG)
                        .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(errMsg);
            }
        }
        if (StringUtils.isNotEmpty(origAttrs)) {
            strBuff.append(AttributeConstants.SEPARATOR).append(origAttrs);
        }
        String destAttrs = strBuff.toString();
        // build response message bytes
        if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
            binBuffer = buildBinMsgRspPackage(destAttrs,
                    attrMap.get(AttributeConstants.UNIQ_ID));
        } else {
            // MsgType.MSG_ORIGINAL_RETURN.equals(msgType)
            // MsgType.MSG_MULTI_BODY.equals(msgType)
            // MsgType.MSG_MULTI_BODY_ATTR.equals(msgType)
            binBuffer = buildDefMsgRspPackage(msgType, destAttrs);
        }
        // send response message
        try {
            if (remoteChannel.isWritable()) {
                remoteChannel.writeAndFlush(binBuffer);
            } else {
                // release allocated ByteBuf
                binBuffer.release();
                if (logCounter.shouldPrint()) {
                    strBuff.delete(0, strBuff.length());
                    logger.warn(strBuff.append("Send buffer is full4 by channel ")
                            .append(remoteChannel).append(", attr is ").append(origAttrs).toString());
                }
            }
        } catch (Throwable e) {
            // should release bin buffer?
            if (logCounter.shouldPrint()) {
                strBuff.delete(0, strBuff.length());
                logger.error(strBuff.append("Write data to channel exception, channel=")
                        .append(remoteChannel).append(", attr is ").append(origAttrs).toString());
            }
        }
    }

    /**
     * Build hearbeat(1)-msg response message ByteBuf
     *
     * @return ByteBuf
     */
    private static ByteBuf buildHeartBeatMsgRspPackage() {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(5);
        // magic data
        buffer.writeBytes(new byte[]{0, 0, 0, 1, 1});
        return buffer;
    }

    /**
     * Build default-msg response message ByteBuf
     *
     * @param msgType  the message type
     * @param attrs    the return attribute
     * @return ByteBuf
     */
    private static ByteBuf buildDefMsgRspPackage(MsgType msgType, String attrs) {
        int attrsLen = 0;
        int bodyLen = 0;
        if (attrs != null) {
            attrsLen = attrs.length();
        }
        // backTotalLen = mstType + bodyLen + body + attrsLen + attrs
        int backTotalLen = 1 + 4 + bodyLen + 4 + attrsLen;
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(4 + backTotalLen);
        buffer.writeInt(backTotalLen);
        buffer.writeByte(msgType.getValue());
        buffer.writeInt(bodyLen);
        buffer.writeInt(attrsLen);
        if (attrsLen > 0) {
            buffer.writeBytes(attrs.getBytes(StandardCharsets.UTF_8));
        }
        return buffer;
    }

    /**
     * Build bin-msg response message ByteBuf
     *
     * @param attrs   the return attribute
     * @param sequenceId sequence Id
     * @return ByteBuf
     */
    private static ByteBuf buildBinMsgRspPackage(String attrs, String sequenceId) {
        // calculate total length
        // binTotalLen = mstType + uniq + attrsLen + attrs + magic
        int binTotalLen = 1 + 4 + 2 + 2;
        if (null != attrs) {
            binTotalLen += attrs.length();
        }
        // allocate buffer and write fields
        ByteBuf binBuffer = ByteBufAllocator.DEFAULT.buffer(4 + binTotalLen);
        binBuffer.writeInt(binTotalLen);
        binBuffer.writeByte(MsgType.MSG_BIN_MULTI_BODY.getValue());
        long uniqVal = Long.parseLong(sequenceId);
        byte[] uniq = new byte[4];
        uniq[0] = (byte) ((uniqVal >> 24) & 0xFF);
        uniq[1] = (byte) ((uniqVal >> 16) & 0xFF);
        uniq[2] = (byte) ((uniqVal >> 8) & 0xFF);
        uniq[3] = (byte) (uniqVal & 0xFF);
        binBuffer.writeBytes(uniq);
        if (null != attrs) {
            binBuffer.writeShort(attrs.length());
            binBuffer.writeBytes(attrs.getBytes(StandardCharsets.UTF_8));
        } else {
            binBuffer.writeShort(0x0);
        }
        binBuffer.writeShort(0xee01);
        return binBuffer;
    }

    /**
     * Build heartbeat response message ByteBuf
     *
     * @param attrs     the attribute string
     * @param version   the version
     * @param loadValue the node load value
     * @return ByteBuf
     */
    private static ByteBuf buildHBRspPackage(String attrs, byte version, int loadValue) {
        // calculate total length
        // binTotalLen = mstType + dataTime + version + bodyLen + body + attrsLen + attrs + magic
        int binTotalLen = 1 + 4 + 1 + 4 + 2 + 2 + 2;
        if (null != attrs) {
            binTotalLen += attrs.length();
        }
        // check load value
        if (loadValue == 0 || loadValue == (-1)) {
            loadValue = 0xffff;
        }
        // allocate buffer and write fields
        ByteBuf binBuffer = ByteBufAllocator.DEFAULT.buffer(4 + binTotalLen);
        binBuffer.writeInt(binTotalLen);
        binBuffer.writeByte(MsgType.MSG_BIN_HEARTBEAT.getValue());
        binBuffer.writeInt((int) (System.currentTimeMillis() / 1000));
        binBuffer.writeByte(version);
        binBuffer.writeInt(2);
        binBuffer.writeShort(loadValue);
        if (null != attrs) {
            binBuffer.writeShort(attrs.length());
            binBuffer.writeBytes(attrs.getBytes(StandardCharsets.UTF_8));
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
            logger.debug("Get topic by groupId = {}, streamId = {}, topic = {}",
                    groupId, streamId, topic);
        }
        return topic;
    }

    public static Map<String, String> getXfsAttrs(Map<String, String> headers, String pkgVersion) {
        // common attributes
        Map<String, String> attrs = new HashMap<>();
        attrs.put(ConfigConstants.MSG_ENCODE_VER, pkgVersion);
        if (InLongMsgVer.INLONG_V1.getName().equalsIgnoreCase(pkgVersion)) {
            attrs.put("dataproxyip", NetworkUtils.getLocalIp());
            attrs.put(Constants.INLONG_GROUP_ID, headers.get(Constants.INLONG_GROUP_ID));
            attrs.put(Constants.INLONG_STREAM_ID, headers.get(Constants.INLONG_STREAM_ID));
            attrs.put(Constants.TOPIC, headers.get(Constants.TOPIC));
            attrs.put(Constants.HEADER_KEY_MSG_TIME, headers.get(Constants.HEADER_KEY_MSG_TIME));
            attrs.put(Constants.HEADER_KEY_SOURCE_IP, headers.get(Constants.HEADER_KEY_SOURCE_IP));
        } else {
            //
            attrs.put(Constants.INLONG_GROUP_ID,
                    headers.get(AttributeConstants.GROUP_ID));
            attrs.put(Constants.INLONG_STREAM_ID,
                    headers.get(AttributeConstants.STREAM_ID));
            attrs.put(Constants.TOPIC,
                    headers.get(ConfigConstants.TOPIC_KEY));
            attrs.put(Constants.HEADER_KEY_MSG_TIME,
                    headers.get(AttributeConstants.DATA_TIME));
            attrs.put(Constants.HEADER_KEY_SOURCE_IP,
                    headers.get(ConfigConstants.REMOTE_IP_KEY));
            attrs.put(Constants.HEADER_KEY_SOURCE_TIME,
                    headers.get(AttributeConstants.RCV_TIME));
            attrs.put(ConfigConstants.DATAPROXY_IP_KEY,
                    NetworkUtils.getLocalIp());
        }
        return attrs;
    }

}
