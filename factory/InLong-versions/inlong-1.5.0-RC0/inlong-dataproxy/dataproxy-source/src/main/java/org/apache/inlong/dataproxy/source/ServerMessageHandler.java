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

package org.apache.inlong.dataproxy.source;

import static org.apache.inlong.common.util.NetworkUtils.getLocalIp;
import static org.apache.inlong.dataproxy.source.SimpleTcpSource.blacklist;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.event.EventBuilder;
import org.apache.inlong.common.monitor.MonitorIndex;
import org.apache.inlong.common.monitor.MonitorIndexExt;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.common.msg.MsgType;
import org.apache.inlong.dataproxy.base.SinkRspEvent;
import org.apache.inlong.dataproxy.base.ProxyMessage;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.consts.AttrConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.exception.MessageIDException;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.apache.inlong.dataproxy.metrics.audit.AuditUtils;
import org.apache.inlong.dataproxy.utils.DateTimeUtils;
import org.apache.inlong.dataproxy.utils.InLongMsgVer;
import org.apache.inlong.dataproxy.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server message handler
 *
 */
public class ServerMessageHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerMessageHandler.class);

    private static final String DEFAULT_REMOTE_IP_VALUE = "0.0.0.0";

    private static final String DEFAULT_REMOTE_IDC_VALUE = "0";

    private static final ConfigManager configManager = ConfigManager.getInstance();

    private static final Joiner.MapJoiner mapJoiner = Joiner.on(AttributeConstants.SEPARATOR)
            .withKeyValueSeparator(AttributeConstants.KEY_VALUE_SEPARATOR);

    private static final Splitter.MapSplitter mapSplitter = Splitter
            .on(AttributeConstants.SEPARATOR)
            .trimResults().withKeyValueSeparator(AttributeConstants.KEY_VALUE_SEPARATOR);

    private BaseSource source;

    private final ChannelGroup allChannels;

    private int maxConnections = Integer.MAX_VALUE;

    private boolean filterEmptyMsg = false;

    private final boolean isCompressed;

    private final ChannelProcessor processor;

    private final ServiceDecoder serviceDecoder;

    private final String defaultTopic;

    private String defaultMXAttr = "m=3";

    private final String protocolType;

    private MonitorIndex monitorIndex;

    private MonitorIndexExt monitorIndexExt;

    //
    private final DataProxyMetricItemSet metricItemSet;

    /**
     * Constructor
     *
     * @param source AbstractSource
     * @param serviceDecoder ServiceDecoder
     * @param allChannels ChannelGroup
     * @param topic Topic
     * @param attr String
     * @param filterEmptyMsg Boolean
     * @param maxCons maxCons
     * @param isCompressed Is compressed
     * @param monitorIndex MonitorIndex
     * @param monitorIndexExt MonitorIndexExt
     * @param protocolType protocolType
     */
    public ServerMessageHandler(BaseSource source, ServiceDecoder serviceDecoder,
            ChannelGroup allChannels,
            String topic, String attr, Boolean filterEmptyMsg,
            Integer maxCons, Boolean isCompressed, MonitorIndex monitorIndex,
            MonitorIndexExt monitorIndexExt, String protocolType) {
        this.source = source;
        this.processor = source.getChannelProcessor();
        this.serviceDecoder = serviceDecoder;
        this.allChannels = allChannels;
        this.defaultTopic = topic;
        if (null != attr) {
            this.defaultMXAttr = attr;
        }
        this.filterEmptyMsg = filterEmptyMsg;
        this.isCompressed = isCompressed;
        this.maxConnections = maxCons;
        this.protocolType = protocolType;
        this.metricItemSet = source.getMetricItemSet();
        this.monitorIndex = monitorIndex;
        this.monitorIndexExt = monitorIndexExt;
    }

    private String getRemoteIp(Channel channel) {
        return getRemoteIp(channel, null);
    }

    private String getRemoteIp(Channel channel, SocketAddress remoteAddress) {
        String strRemoteIp = DEFAULT_REMOTE_IP_VALUE;
        SocketAddress remoteSocketAddress = channel.remoteAddress();
        if (remoteSocketAddress == null) {
            remoteSocketAddress = remoteAddress;
        }
        if (null != remoteSocketAddress) {
            strRemoteIp = remoteSocketAddress.toString();
            try {
                strRemoteIp = strRemoteIp.substring(1, strRemoteIp.indexOf(':'));
            } catch (Exception ee) {
                logger.warn("fail to get the remote IP, and strIP={},remoteSocketAddress={}",
                        strRemoteIp, remoteSocketAddress);
            }
        }
        return strRemoteIp;
    }

    private byte[] newBinMsg(byte[] orgBinMsg, String extraAttr) {
        final int BIN_MSG_TOTALLEN_OFFSET = 0;
        final int BIN_MSG_TOTALLEN_SIZE = 4;
        final int BIN_MSG_BODYLEN_SIZE = 4;
        final int BIN_MSG_EXTEND_OFFSET = 9;
        final int BIN_MSG_BODYLEN_OFFSET = 21;
        final int BIN_MSG_BODY_OFFSET = BIN_MSG_BODYLEN_SIZE + BIN_MSG_BODYLEN_OFFSET;
        final int BIN_MSG_ATTRLEN_SIZE = 2;
        final int BIN_MSG_FORMAT_SIZE = 29;
        final int BIN_MSG_MAGIC_SIZE = 2;
        final int BIN_MSG_MAGIC = 0xEE01;

        ByteBuffer orgBuf = ByteBuffer.wrap(orgBinMsg);
        int totalLen = orgBuf.getInt(BIN_MSG_TOTALLEN_OFFSET);
        int dataLen = orgBuf.getInt(BIN_MSG_BODYLEN_OFFSET);
        int attrLen = orgBuf.getShort(BIN_MSG_BODY_OFFSET + dataLen);

        int newTotalLen = 0;
        String strAttr;
        if (attrLen != 0) {
            newTotalLen = totalLen + extraAttr.length() + "&".length();
            strAttr = "&" + extraAttr;
        } else {
            newTotalLen = totalLen + extraAttr.length();
            strAttr = extraAttr;
        }

        ByteBuffer dataBuf = ByteBuffer.allocate(newTotalLen + BIN_MSG_TOTALLEN_SIZE);
        dataBuf
                .put(orgBuf.array(), 0, dataLen + (BIN_MSG_FORMAT_SIZE - BIN_MSG_MAGIC_SIZE) + attrLen);
        dataBuf
                .putShort(dataLen + (BIN_MSG_FORMAT_SIZE - BIN_MSG_ATTRLEN_SIZE - BIN_MSG_MAGIC_SIZE),
                        (short) (strAttr.length() + attrLen));

        System.arraycopy(strAttr.getBytes(StandardCharsets.UTF_8), 0, dataBuf.array(),
                dataLen + (BIN_MSG_FORMAT_SIZE - BIN_MSG_MAGIC_SIZE) + attrLen,
                strAttr.length());
        int extendField = orgBuf.getShort(BIN_MSG_EXTEND_OFFSET);
        dataBuf.putShort(BIN_MSG_EXTEND_OFFSET, (short) (extendField | 0x4));
        dataBuf.putInt(0, newTotalLen);
        dataBuf.putShort(newTotalLen + BIN_MSG_TOTALLEN_SIZE - BIN_MSG_MAGIC_SIZE,
                (short) BIN_MSG_MAGIC);
        return dataBuf.array();
    }

    public boolean checkBlackIp(Channel channel) {
        String strRemoteIp = getRemoteIp(channel);
        if (strRemoteIp != null && blacklist != null && blacklist.contains(strRemoteIp)) {
            logger.error(strRemoteIp + " is in blacklist, so refuse it !");
            channel.disconnect();
            channel.close();
            allChannels.remove(channel);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (allChannels.size() - 1 >= maxConnections) {
            logger.warn("refuse to connect , and connections=" + (allChannels.size() - 1)
                    + ", maxConnections="
                    + maxConnections + ",channel is " + ctx.channel());
            ctx.channel().disconnect();
            ctx.channel().close();
        }
        if (!checkBlackIp(ctx.channel())) {
            logger.info("connections={},maxConnections={}", allChannels.size() - 1, maxConnections);
            allChannels.add(ctx.channel());
            ctx.fireChannelActive();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.error("channel inactive {}", ctx.channel());
        ctx.fireChannelInactive();
        allChannels.remove(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            logger.debug("Get null msg, just skip!");
            return;
        }
        ByteBuf cb = (ByteBuf) msg;
        try {
            Channel remoteChannel = ctx.channel();
            String strRemoteIP = getRemoteIp(remoteChannel);
            int len = cb.readableBytes();
            if (len == 0 && this.filterEmptyMsg) {
                logger.debug("Get empty msg from {}, just skip!", strRemoteIP);
                return;
            }
            // parse message
            Map<String, Object> resultMap = null;
            final long msgRcvTime = System.currentTimeMillis();
            try {
                resultMap = serviceDecoder.extractData(cb,
                        strRemoteIP, msgRcvTime, remoteChannel);
                if (resultMap == null || resultMap.isEmpty()) {
                    logger.debug("Parse message result is null, from {}", strRemoteIP);
                    return;
                }
            } catch (MessageIDException ex) {
                logger.error("MessageIDException ex = {}", ex);
                throw new IOException(ex.getCause());
            }
            // get msgType from parsed result
            MsgType msgType = (MsgType) resultMap.get(ConfigConstants.MSG_TYPE);
            // get attribute data from parsed result
            Map<String, String> commonAttrMap =
                    (Map<String, String>) resultMap.get(ConfigConstants.COMMON_ATTR_MAP);
            if (commonAttrMap == null) {
                commonAttrMap = new HashMap<>();
            }
            // check whether extract data failure
            String errCode = commonAttrMap.get(AttributeConstants.MESSAGE_PROCESS_ERRCODE);
            if (!StringUtils.isEmpty(errCode)
                    && !DataProxyErrCode.SUCCESS.getErrCodeStr().equals(errCode)) {
                MessageUtils.sourceReturnRspPackage(
                        commonAttrMap, resultMap, remoteChannel, msgType);
                return;
            }
            // process heartbeat message
            if (MsgType.MSG_HEARTBEAT.equals(msgType)
                    || MsgType.MSG_BIN_HEARTBEAT.equals(msgType)) {
                MessageUtils.sourceReturnRspPackage(
                        commonAttrMap, resultMap, remoteChannel, msgType);
                return;
            }
            // reject unsupported messages
            if (commonAttrMap.containsKey(ConfigConstants.FILE_CHECK_DATA)
                    || commonAttrMap.containsKey(ConfigConstants.MINUTE_CHECK_DATA)) {
                commonAttrMap.put(AttributeConstants.MESSAGE_PROCESS_ERRCODE,
                        DataProxyErrCode.UNSUPPORTED_EXTEND_FIELD_VALUE.getErrCodeStr());
                MessageUtils.sourceReturnRspPackage(
                        commonAttrMap, resultMap, remoteChannel, msgType);
                return;
            }
            // check message's groupId, streamId, topic
            List<ProxyMessage> msgList =
                    (List<ProxyMessage>) resultMap.get(ConfigConstants.MSG_LIST);
            if (msgList == null) {
                commonAttrMap.put(AttributeConstants.MESSAGE_PROCESS_ERRCODE,
                        DataProxyErrCode.EMPTY_MSG.getErrCodeStr());
                MessageUtils.sourceReturnRspPackage(
                        commonAttrMap, resultMap, remoteChannel, msgType);
                return;
            }
            // check sink service status
            if (!ConfigManager.getInstance().isMqClusterReady()) {
                commonAttrMap.put(AttributeConstants.MESSAGE_PROCESS_ERRCODE,
                        DataProxyErrCode.SINK_SERVICE_UNREADY.getErrCodeStr());
                MessageUtils.sourceReturnRspPackage(
                        commonAttrMap, resultMap, remoteChannel, msgType);
                return;
            }
            // convert message data
            Map<String, HashMap<String, List<ProxyMessage>>> messageMap =
                    new HashMap<>(msgList.size());
            if (!convertMsgList(msgList, commonAttrMap, messageMap, strRemoteIP)) {
                MessageUtils.sourceReturnRspPackage(
                        commonAttrMap, resultMap, remoteChannel, msgType);
                return;
            }
            // send messages to channel
            formatMessagesAndSend(ctx, commonAttrMap, resultMap,
                    messageMap, strRemoteIP, msgType, msgRcvTime);
            // return response
            if (!MessageUtils.isSinkRspType(commonAttrMap)) {
                MessageUtils.sourceReturnRspPackage(
                        commonAttrMap, resultMap, remoteChannel, msgType);
            }
        } finally {
            cb.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exception caught cause = {}", cause);
        monitorIndexExt.incrementAndGet("EVENT_OTHEREXP");
        ctx.close();
    }

    /**
     * Complete the message content and covert proxy message to map
     *
     * @param msgList  the message list
     * @param commonAttrMap common attribute map
     * @param messageMap    message list
     * @param strRemoteIP   remote ip
     *
     * @return  convert result
     */
    private boolean convertMsgList(List<ProxyMessage> msgList, Map<String, String> commonAttrMap,
            Map<String, HashMap<String, List<ProxyMessage>>> messageMap,
            String strRemoteIP) {
        for (ProxyMessage message : msgList) {
            String configTopic = null;
            String groupId = message.getGroupId();
            String streamId = message.getStreamId();
            // get topic by groupId and streamId
            if (null == groupId) {
                String num2name = commonAttrMap.get(AttrConstants.NUM2NAME);
                String groupIdNum = commonAttrMap.get(AttrConstants.GROUPID_NUM);
                String streamIdNum = commonAttrMap.get(AttrConstants.STREAMID_NUM);
                // get configured groupId and steamId by numbers
                if (configManager.getGroupIdMappingProperties() != null
                        && configManager.getStreamIdMappingProperties() != null) {
                    groupId = configManager.getGroupIdMappingProperties().get(groupIdNum);
                    streamId = (configManager.getStreamIdMappingProperties().get(groupIdNum) == null)
                            ? null
                            : configManager.getStreamIdMappingProperties().get(groupIdNum).get(streamIdNum);
                    if (groupId != null && streamId != null) {
                        String enableTrans =
                                (configManager.getGroupIdEnableMappingProperties() == null)
                                        ? null
                                        : configManager.getGroupIdEnableMappingProperties().get(groupIdNum);
                        if (("TRUE".equalsIgnoreCase(enableTrans)
                                && "TRUE".equalsIgnoreCase(num2name))) {
                            String extraAttr = "groupId=" + groupId + "&" + "streamId=" + streamId;
                            message.setData(newBinMsg(message.getData(), extraAttr));
                        }
                        // reset groupId and streamId to message and attrMap
                        message.setGroupId(groupId);
                        message.setStreamId(streamId);
                        // get configured topic name
                        configTopic = MessageUtils.getTopic(
                                configManager.getTopicProperties(), groupId, streamId);
                    }
                }
            } else {
                // get configured group Id
                String from = commonAttrMap.get(AttributeConstants.FROM);
                if ("dc".equals(from)) {
                    String dcInterfaceId = message.getStreamId();
                    if (StringUtils.isNotEmpty(dcInterfaceId)
                            && configManager.getDcMappingProperties()
                                    .containsKey(dcInterfaceId.trim())) {
                        groupId = configManager.getDcMappingProperties()
                                .get(dcInterfaceId.trim()).trim();
                        message.setGroupId(groupId);
                    }
                }
                // get configured m value
                Map<String, String> mxValue =
                        configManager.getMxPropertiesMaps().get(groupId);
                if (mxValue != null && mxValue.size() != 0) {
                    message.getAttributeMap().putAll(mxValue);
                } else {
                    message.getAttributeMap().putAll(mapSplitter.split(this.defaultMXAttr));
                }
                // get configured topic name
                configTopic = MessageUtils.getTopic(
                        configManager.getTopicProperties(), groupId, streamId);
            }
            // check topic configure
            if (StringUtils.isEmpty(configTopic)) {
                String acceptMsg =
                        configManager.getCommonProperties().getOrDefault(
                                ConfigConstants.SOURCE_NO_TOPIC_ACCEPT, "false");
                if ("true".equalsIgnoreCase(acceptMsg)) {
                    configTopic = this.defaultTopic;
                } else {
                    commonAttrMap.put(AttributeConstants.MESSAGE_PROCESS_ERRCODE,
                            DataProxyErrCode.UNCONFIGURED_GROUPID_OR_STREAMID.getErrCodeStr());
                    logger.error("Topic for message is null , inlongGroupId = {}, inlongStreamId = {}",
                            groupId, streamId);
                    return false;
                }
            }
            if (streamId == null) {
                streamId = "";
                message.setStreamId(streamId);
            }
            // append topic
            message.setTopic(configTopic);
            commonAttrMap.put(AttributeConstants.NODE_IP, strRemoteIP);
            // add ProxyMessage
            HashMap<String, List<ProxyMessage>> streamIdMsgMap = messageMap
                    .computeIfAbsent(configTopic, k -> new HashMap<>());
            List<ProxyMessage> streamIdMsgList = streamIdMsgMap
                    .computeIfAbsent(streamId, k -> new ArrayList<>());
            streamIdMsgList.add(message);
        }
        return true;
    }

    /**
     * format message to event and send to channel
     *
     * @param ctx  client connect
     * @param commonAttrMap common attribute map
     * @param resultMap    the result map
     * @param messageMap    message list
     * @param strRemoteIP   remote ip
     * @param msgType    the message type
     * @param msgRcvTime  the received time
     */
    private void formatMessagesAndSend(ChannelHandlerContext ctx, Map<String, String> commonAttrMap,
            Map<String, Object> resultMap,
            Map<String, HashMap<String, List<ProxyMessage>>> messageMap,
            String strRemoteIP, MsgType msgType, long msgRcvTime) throws MessageIDException {

        int inLongMsgVer = 1;
        if (MsgType.MSG_MULTI_BODY_ATTR.equals(msgType)) {
            inLongMsgVer = 3;
        } else if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
            inLongMsgVer = 4;
        }
        StringBuilder strBuff = new StringBuilder(512);
        int recordMsgCnt = Integer.parseInt(commonAttrMap.get(AttributeConstants.MESSAGE_COUNT));
        // process each ProxyMessage
        for (Map.Entry<String, HashMap<String, List<ProxyMessage>>> topicEntry : messageMap.entrySet()) {
            for (Map.Entry<String, List<ProxyMessage>> streamIdEntry : topicEntry.getValue().entrySet()) {
                // build InLongMsg
                String groupId = null;
                InLongMsg inLongMsg = InLongMsg.newInLongMsg(this.isCompressed, inLongMsgVer);
                if (MsgType.MSG_MULTI_BODY_ATTR.equals(msgType) || MsgType.MSG_MULTI_BODY.equals(msgType)) {
                    for (ProxyMessage message : streamIdEntry.getValue()) {
                        if (StringUtils.isEmpty(groupId)) {
                            groupId = message.getGroupId();
                        }
                        message.getAttributeMap().put(AttributeConstants.MESSAGE_COUNT, String.valueOf(1));
                        inLongMsg.addMsg(mapJoiner.join(message.getAttributeMap()), message.getData());
                    }
                } else if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
                    for (ProxyMessage message : streamIdEntry.getValue()) {
                        if (StringUtils.isEmpty(groupId)) {
                            groupId = message.getGroupId();
                        }
                        inLongMsg.addMsg(message.getData());
                    }
                } else {
                    for (ProxyMessage message : streamIdEntry.getValue()) {
                        if (StringUtils.isEmpty(groupId)) {
                            groupId = message.getGroupId();
                        }
                        inLongMsg.addMsg(mapJoiner.join(message.getAttributeMap()), message.getData());
                    }
                }
                commonAttrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(recordMsgCnt));
                // build headers
                Map<String, String> headers = new HashMap<>();
                headers.put(AttributeConstants.GROUP_ID, groupId);
                headers.put(AttributeConstants.STREAM_ID, streamIdEntry.getKey());
                headers.put(ConfigConstants.TOPIC_KEY, topicEntry.getKey());
                String strDataTime = commonAttrMap.get(AttributeConstants.DATA_TIME);
                headers.put(AttributeConstants.DATA_TIME, strDataTime);
                headers.put(ConfigConstants.REMOTE_IP_KEY, strRemoteIP);
                headers.put(ConfigConstants.REMOTE_IDC_KEY, DEFAULT_REMOTE_IDC_VALUE);
                headers.put(ConfigConstants.MSG_COUNTER_KEY,
                        commonAttrMap.get(AttributeConstants.MESSAGE_COUNT));
                headers.put(ConfigConstants.MSG_ENCODE_VER, InLongMsgVer.INLONG_V0.getName());
                headers.put(AttributeConstants.RCV_TIME,
                        commonAttrMap.get(AttributeConstants.RCV_TIME));
                headers.put(ConfigConstants.DECODER_ATTRS,
                        (String) resultMap.get(ConfigConstants.DECODER_ATTRS));
                // add extra key-value information
                headers.put(AttributeConstants.UNIQ_ID,
                        commonAttrMap.get(AttributeConstants.UNIQ_ID));
                if ("false".equals(commonAttrMap.get(AttributeConstants.MESSAGE_IS_ACK))) {
                    headers.put(AttributeConstants.MESSAGE_IS_ACK, "false");
                }
                String syncSend = commonAttrMap.get(AttributeConstants.MESSAGE_SYNC_SEND);
                if (StringUtils.isNotEmpty(syncSend)) {
                    headers.put(AttributeConstants.MESSAGE_SYNC_SEND, syncSend);
                }
                String proxySend = commonAttrMap.get(AttributeConstants.MESSAGE_PROXY_SEND);
                if (StringUtils.isNotEmpty(proxySend)) {
                    headers.put(AttributeConstants.MESSAGE_PROXY_SEND, proxySend);
                }
                String partitionKey = commonAttrMap.get(AttributeConstants.MESSAGE_PARTITION_KEY);
                if (StringUtils.isNotEmpty(partitionKey)) {
                    headers.put(AttributeConstants.MESSAGE_PARTITION_KEY, partitionKey);
                }
                String sequenceId = commonAttrMap.get(AttributeConstants.SEQUENCE_ID);
                if (StringUtils.isNotEmpty(sequenceId)) {
                    strBuff.append(topicEntry.getKey()).append(AttributeConstants.SEPARATOR)
                            .append(streamIdEntry.getKey())
                            .append(AttributeConstants.SEPARATOR).append(sequenceId);
                    headers.put(ConfigConstants.SEQUENCE_ID, strBuff.toString());
                    strBuff.delete(0, strBuff.length());
                }
                final byte[] data = inLongMsg.buildArray();
                Event event = EventBuilder.withBody(data, headers);
                event.getHeaders().putAll(headers);
                inLongMsg.reset();
                Pair<Boolean, String> evenProcType =
                        MessageUtils.getEventProcType(syncSend, proxySend);
                if (evenProcType.getLeft()) {
                    event = new SinkRspEvent(event, msgType, ctx);
                }
                // build metric data item
                long longDataTime = Long.parseLong(strDataTime);
                longDataTime = longDataTime / 1000 / 60 / 10;
                longDataTime = longDataTime * 1000 * 60 * 10;
                strBuff.append(protocolType).append(AttrConstants.SEPARATOR)
                        .append(topicEntry.getKey()).append(AttrConstants.SEPARATOR)
                        .append(streamIdEntry.getKey()).append(AttrConstants.SEPARATOR)
                        .append(strRemoteIP).append(AttrConstants.SEPARATOR)
                        .append(getLocalIp()).append(AttrConstants.SEPARATOR)
                        .append(evenProcType.getRight()).append(AttrConstants.SEPARATOR)
                        .append(DateTimeUtils.ms2yyyyMMddHHmm(longDataTime)).append(AttrConstants.SEPARATOR)
                        .append(DateTimeUtils.ms2yyyyMMddHHmm(msgRcvTime));
                try {
                    processor.processEvent(event);
                    monitorIndexExt.incrementAndGet("EVENT_SUCCESS");
                    this.addStatistics(true, data.length, event);
                    monitorIndex.addAndGet(strBuff.toString(),
                            recordMsgCnt, 1, data.length, 0);
                    strBuff.delete(0, strBuff.length());
                } catch (Throwable ex) {
                    logger.error("Error writting to channel, data will discard.", ex);
                    monitorIndexExt.incrementAndGet("EVENT_DROPPED");
                    monitorIndex.addAndGet(strBuff.toString(), 0, 0, 0, recordMsgCnt);
                    this.addStatistics(false, data.length, event);
                    strBuff.delete(0, strBuff.length());
                    throw new ChannelException("ProcessEvent error can't write event to channel.");
                }
            }
        }
    }

    /**
     * add statistics information
     *
     * @param isSuccess  success or failure
     * @param size    message size
     * @param event   message event
     */
    private void addStatistics(boolean isSuccess, long size, Event event) {
        if (event == null) {
            return;
        }
        this.metricItemSet.fillSrcMetricItemsByEvent(event, isSuccess, size);
        if (isSuccess) {
            AuditUtils.add(AuditUtils.AUDIT_ID_DATAPROXY_READ_SUCCESS, event);
        }
    }
}
