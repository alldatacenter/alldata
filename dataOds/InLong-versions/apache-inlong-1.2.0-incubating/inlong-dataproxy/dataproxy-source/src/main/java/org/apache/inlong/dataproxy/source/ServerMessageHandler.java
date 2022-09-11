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

import static org.apache.inlong.dataproxy.consts.AttributeConstants.SEPARATOR;
import static org.apache.inlong.dataproxy.consts.ConfigConstants.SLA_METRIC_DATA;
import static org.apache.inlong.dataproxy.consts.ConfigConstants.SLA_METRIC_GROUPID;
import static org.apache.inlong.dataproxy.source.SimpleTcpSource.blacklist;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.AbstractSource;
import org.apache.inlong.common.monitor.MonitorIndex;
import org.apache.inlong.common.monitor.MonitorIndexExt;
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.dataproxy.base.OrderEvent;
import org.apache.inlong.dataproxy.base.ProxyMessage;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.consts.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.exception.ErrorCode;
import org.apache.inlong.dataproxy.exception.MessageIDException;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItem;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.apache.inlong.dataproxy.metrics.audit.AuditUtils;
import org.apache.inlong.dataproxy.utils.MessageUtils;
import org.apache.inlong.dataproxy.utils.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

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

    private static final ThreadLocal<SimpleDateFormat> dateFormator =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMddHHmm"));

    private AbstractSource source;

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
    public ServerMessageHandler(AbstractSource source, ServiceDecoder serviceDecoder,
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
        if (source instanceof SimpleTcpSource) {
            this.metricItemSet = ((SimpleTcpSource) source).getMetricItemSet();
        } else {
            this.metricItemSet = new DataProxyMetricItemSet(this.toString());
        }
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
                        strRemoteIp,
                        remoteSocketAddress);
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

    private void checkGroupIdInfo(ProxyMessage message, Map<String, String> commonAttrMap,
            Map<String, String> attrMap, AtomicReference<String> topicInfo) {
        String groupId = message.getGroupId();
        String streamId = message.getStreamId();
        if (null != groupId) {
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

            String value = MessageUtils.getTopic(configManager.getTopicProperties(), groupId,
                    streamId);
            if (StringUtils.isNotEmpty(value)) {
                topicInfo.set(value.trim());
            }

            Map<String, String> mxValue = configManager.getMxPropertiesMaps().get(groupId);
            if (mxValue != null && mxValue.size() != 0) {
                message.getAttributeMap().putAll(mxValue);
            } else {
                message.getAttributeMap().putAll(mapSplitter.split(this.defaultMXAttr));
            }
        } else {
            String num2name = commonAttrMap.get(AttributeConstants.NUM2NAME);
            String groupIdNum = commonAttrMap.get(AttributeConstants.GROUPID_NUM);
            String streamIdNum = commonAttrMap.get(AttributeConstants.STREAMID_NUM);

            if (configManager.getGroupIdMappingProperties() != null
                    && configManager.getStreamIdMappingProperties() != null) {
                groupId = configManager.getGroupIdMappingProperties().get(groupIdNum);
                streamId = (configManager.getStreamIdMappingProperties().get(groupIdNum) == null)
                        ? null : configManager.getStreamIdMappingProperties().get(groupIdNum).get(streamIdNum);
                if (groupId != null && streamId != null) {
                    String enableTrans =
                            (configManager.getGroupIdEnableMappingProperties() == null)
                                    ? null : configManager.getGroupIdEnableMappingProperties().get(groupIdNum);
                    if (("TRUE".equalsIgnoreCase(enableTrans) && "TRUE"
                            .equalsIgnoreCase(num2name))) {
                        String extraAttr = "groupId=" + groupId + "&" + "streamId=" + streamId;
                        message.setData(newBinMsg(message.getData(), extraAttr));
                    }

                    attrMap.put(AttributeConstants.GROUP_ID, groupId);
                    attrMap.put(AttributeConstants.STREAM_ID, streamId);
                    message.setGroupId(groupId);
                    message.setStreamId(streamId);

                    String value = MessageUtils.getTopic(configManager.getTopicProperties(),
                            groupId, streamId);
                    if (StringUtils.isNotEmpty(value)) {
                        topicInfo.set(value.trim());
                    }
                }
            }
        }
    }

    private boolean updateMsgList(List<ProxyMessage> msgList, Map<String, String> commonAttrMap,
            Map<String, HashMap<String, List<ProxyMessage>>> messageMap,
            String strRemoteIP, MsgType msgType) {
        for (ProxyMessage message : msgList) {
            Map<String, String> attrMap = message.getAttributeMap();

            String topic = this.defaultTopic;

            AtomicReference<String> topicInfo = new AtomicReference<>(topic);
            checkGroupIdInfo(message, commonAttrMap, attrMap, topicInfo);
            String groupId = message.getGroupId();
            String streamId = message.getStreamId();
            topic = topicInfo.get();
            if (StringUtils.isEmpty(topic)) {
                logger.warn("Topic for message is null , inlongGroupId = {}, inlongStreamId = {}",
                        groupId, streamId);
            }
            //                if(groupId==null)groupId="b_test";//default groupId

            message.setTopic(topic);
            commonAttrMap.put(AttributeConstants.NODE_IP, strRemoteIP);

            // whether sla
            if (SLA_METRIC_GROUPID.equals(groupId)) {
                commonAttrMap.put(SLA_METRIC_DATA, "true");
                message.setTopic(SLA_METRIC_DATA);
            }

            if (groupId != null && streamId != null) {
                String tubeSwtichKey = groupId + SEPARATOR + streamId;
                if (configManager.getTubeSwitchProperties().get(tubeSwtichKey) != null
                        && "false".equals(configManager.getTubeSwitchProperties()
                        .get(tubeSwtichKey).trim())) {
                    continue;
                }
            }

            if (!"pb".equals(attrMap.get(AttributeConstants.MESSAGE_TYPE))
                    && !MsgType.MSG_MULTI_BODY.equals(msgType)
                    && !MsgType.MSG_MULTI_BODY_ATTR.equals(msgType)) {
                byte[] data = message.getData();
                if (data[data.length - 1] == '\n') {
                    int tripDataLen = data.length - 1;
                    if (data[data.length - 2] == '\r') {
                        tripDataLen = data.length - 2;
                    }
                    byte[] tripData = new byte[tripDataLen];
                    System.arraycopy(data, 0, tripData, 0, tripDataLen);
                    message.setData(tripData);
                }
            }

            if (streamId == null) {
                streamId = "";
            }
            HashMap<String, List<ProxyMessage>> streamIdMsgMap = messageMap
                    .computeIfAbsent(topic, k -> new HashMap<>());
            List<ProxyMessage> streamIdMsgList = streamIdMsgMap
                    .computeIfAbsent(streamId, k -> new ArrayList<>());
            streamIdMsgList.add(message);
        }
        return true;
    }

    private void formatMessagesAndSend(ChannelHandlerContext ctx, Map<String, String> commonAttrMap,
            Map<String, HashMap<String, List<ProxyMessage>>> messageMap,
            String strRemoteIP, MsgType msgType) throws MessageIDException {

        int inLongMsgVer = 1;
        if (MsgType.MSG_MULTI_BODY_ATTR.equals(msgType)) {
            inLongMsgVer = 3;
        } else if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
            inLongMsgVer = 4;
        }

        for (Map.Entry<String, HashMap<String, List<ProxyMessage>>> topicEntry : messageMap.entrySet()) {
            for (Map.Entry<String, List<ProxyMessage>> streamIdEntry : topicEntry.getValue().entrySet()) {

                InLongMsg inLongMsg = InLongMsg.newInLongMsg(this.isCompressed, inLongMsgVer);
                Map<String, String> headers = new HashMap<String, String>();
                for (ProxyMessage message : streamIdEntry.getValue()) {
                    if (MsgType.MSG_MULTI_BODY_ATTR.equals(msgType) || MsgType.MSG_MULTI_BODY.equals(msgType)) {
                        message.getAttributeMap().put(AttributeConstants.MESSAGE_COUNT, String.valueOf(1));
                        inLongMsg.addMsg(mapJoiner.join(message.getAttributeMap()), message.getData());
                    } else if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
                        inLongMsg.addMsg(message.getData());
                    } else {
                        inLongMsg.addMsg(mapJoiner.join(message.getAttributeMap()), message.getData());
                    }
                }

                long pkgTimeInMillis = inLongMsg.getCreatetime();
                String pkgTimeStr = dateFormator.get().format(pkgTimeInMillis);

                if (inLongMsgVer == 4) {
                    if (commonAttrMap.containsKey(ConfigConstants.PKG_TIME_KEY)) {
                        pkgTimeStr = commonAttrMap.get(ConfigConstants.PKG_TIME_KEY);
                    } else {
                        pkgTimeStr = dateFormator.get().format(System.currentTimeMillis());
                    }
                }

                if (commonAttrMap.get(AttributeConstants.DATA_TIME) != null) {
                    headers.put(AttributeConstants.DATA_TIME, commonAttrMap.get(AttributeConstants.DATA_TIME));
                } else {
                    headers.put(AttributeConstants.DATA_TIME, String.valueOf(System.currentTimeMillis()));
                }

                if ("false".equals(commonAttrMap.get(AttributeConstants.MESSAGE_IS_ACK))) {
                    headers.put(AttributeConstants.MESSAGE_IS_ACK, "false");
                }

                String syncSend = commonAttrMap.get(AttributeConstants.MESSAGE_SYNC_SEND);
                if (StringUtils.isNotEmpty(syncSend)) {
                    headers.put(AttributeConstants.MESSAGE_SYNC_SEND, syncSend);
                }

                String partitionKey = commonAttrMap.get(AttributeConstants.MESSAGE_PARTITION_KEY);
                if (StringUtils.isNotEmpty(partitionKey)) {
                    headers.put(AttributeConstants.MESSAGE_PARTITION_KEY, partitionKey);
                }

                headers.put(ConfigConstants.TOPIC_KEY, topicEntry.getKey());
                headers.put(AttributeConstants.GROUP_ID,
                        streamIdEntry.getValue().get(0).getGroupId());
                headers.put(AttributeConstants.STREAM_ID, streamIdEntry.getKey());
                headers.put(ConfigConstants.REMOTE_IP_KEY, strRemoteIP);
                headers.put(ConfigConstants.REMOTE_IDC_KEY, DEFAULT_REMOTE_IDC_VALUE);
                // every message share the same msg cnt? what if msgType = 5
                String proxyMetricMsgCnt = commonAttrMap.get(AttributeConstants.MESSAGE_COUNT);
                headers.put(ConfigConstants.MSG_COUNTER_KEY, proxyMetricMsgCnt);

                byte[] data = inLongMsg.buildArray();
                headers.put(ConfigConstants.TOTAL_LEN, String.valueOf(data.length));

                headers.put(AttributeConstants.UNIQ_ID,
                        commonAttrMap.get(AttributeConstants.UNIQ_ID));
                String sequenceId = commonAttrMap.get(AttributeConstants.SEQUENCE_ID);
                if (StringUtils.isNotEmpty(sequenceId)) {
                    StringBuilder sidBuilder = new StringBuilder();
                    sidBuilder.append(topicEntry.getKey()).append(SEPARATOR).append(streamIdEntry.getKey())
                            .append(SEPARATOR).append(sequenceId);
                    headers.put(ConfigConstants.SEQUENCE_ID, sidBuilder.toString());
                }

                headers.put(ConfigConstants.PKG_TIME_KEY, pkgTimeStr);
                Event event = EventBuilder.withBody(data, headers);
                String orderType = "non-order";
                if (MessageUtils.isSyncSendForOrder(event)) {
                    event = new OrderEvent(ctx, event);
                    orderType = "order";
                }
                long dtten = 0;
                try {
                    dtten = Long.parseLong(headers.get(AttributeConstants.DATA_TIME));
                } catch (Exception e1) {
                    long uniqVal = Long.parseLong(commonAttrMap.get(AttributeConstants.UNIQ_ID));
                    throw new MessageIDException(uniqVal,
                            ErrorCode.DT_ERROR,
                            new Throwable("attribute dt=" + headers.get(AttributeConstants.DATA_TIME
                                    + " has error, detail is: topic=" + topicEntry.getKey() + "&streamId="
                                    + streamIdEntry.getKey() + "&NodeIP=" + strRemoteIP), e1));
                }

                dtten = dtten / 1000 / 60 / 10;
                dtten = dtten * 1000 * 60 * 10;
                StringBuilder newbase = new StringBuilder();
                newbase.append(protocolType).append(SEPARATOR)
                        .append(topicEntry.getKey()).append(SEPARATOR)
                        .append(streamIdEntry.getKey()).append(SEPARATOR)
                        .append(strRemoteIP).append(SEPARATOR)
                        .append(NetworkUtils.getLocalIp()).append(SEPARATOR)
                        .append(orderType).append(SEPARATOR)
                        .append(new SimpleDateFormat("yyyyMMddHHmm")
                                .format(dtten)).append(SEPARATOR).append(pkgTimeStr);
                try {
                    processor.processEvent(event);
                    monitorIndexExt.incrementAndGet("EVENT_SUCCESS");
                    this.addMetric(true, data.length, event);
                    monitorIndex.addAndGet(new String(newbase),
                            Integer.parseInt(proxyMetricMsgCnt), 1, data.length, 0);
                } catch (Throwable ex) {
                    logger.error("Error writting to channel,data will discard.", ex);
                    monitorIndexExt.incrementAndGet("EVENT_DROPPED");
                    monitorIndex.addAndGet(new String(newbase), 0,0,0,
                            Integer.parseInt(proxyMetricMsgCnt));
                    this.addMetric(false, data.length, event);
                    throw new ChannelException("ProcessEvent error can't write event to channel.");
                }
            }
        }
    }

    private void responsePackage(ChannelHandlerContext ctx, Map<String, String> commonAttrMap,
            Map<String, Object> resultMap,
            Channel remoteChannel,
            SocketAddress remoteSocketAddress,
            MsgType msgType) throws Exception {
        String isAck = commonAttrMap.get(AttributeConstants.MESSAGE_IS_ACK);
        if (isAck == null || "true".equals(isAck)) {
            if (MsgType.MSG_ACK_SERVICE.equals(msgType) || MsgType.MSG_ORIGINAL_RETURN
                    .equals(msgType)
                    || MsgType.MSG_MULTI_BODY.equals(msgType) || MsgType.MSG_MULTI_BODY_ATTR
                    .equals(msgType)) {
                byte[] backAttr = mapJoiner.join(commonAttrMap).getBytes(StandardCharsets.UTF_8);
                byte[] backBody = null;

                if (backAttr != null && !new String(backAttr, StandardCharsets.UTF_8).isEmpty()) {
                    if (MsgType.MSG_ORIGINAL_RETURN.equals(msgType)) {

                        backBody = (byte[]) resultMap.get(ConfigConstants.DECODER_BODY);
                    } else {

                        backBody = new byte[]{50};
                    }
                    int backTotalLen = 1 + 4 + backBody.length + 4 + backAttr.length;
                    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(4 + backTotalLen);
                    buffer.writeInt(backTotalLen);
                    buffer.writeByte(msgType.getValue());
                    buffer.writeInt(backBody.length);
                    buffer.writeBytes(backBody);
                    buffer.writeInt(backAttr.length);
                    buffer.writeBytes(backAttr);
                    if (remoteChannel.isWritable()) {
                        remoteChannel.writeAndFlush(buffer);
                    } else {
                        String backAttrStr = new String(backAttr, StandardCharsets.UTF_8);
                        logger.warn(
                                "the send buffer1 is full, so disconnect it!please check remote client"
                                        + "; Connection info:"
                                        + remoteChannel + ";attr is " + backAttrStr);
                        buffer.release();
                        throw new Exception(new Throwable(
                                "the send buffer1 is full, so disconnect it!please check remote client"
                                        +
                                        "; Connection info:" + remoteChannel + ";attr is "
                                        + backAttrStr));
                    }
                }
            } else if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
                String backattrs = null;
                if (resultMap.containsKey(ConfigConstants.DECODER_ATTRS)) {
                    backattrs = (String) resultMap.get(ConfigConstants.DECODER_ATTRS);
                }
                String uniqVal = commonAttrMap.get(AttributeConstants.UNIQ_ID);
                ByteBuf binBuffer = MessageUtils.getResponsePackage(backattrs, msgType, uniqVal);
                if (remoteChannel.isWritable()) {
                    remoteChannel.writeAndFlush(binBuffer);
                    logger.debug("Connection info: {} ; attr is {} ; uniqVal {}",
                            remoteChannel, backattrs, uniqVal);
                } else {
                    binBuffer.release();
                    logger.warn(
                            "the send buffer2 is full, so disconnect it!please check remote client"
                                    + "; Connection info:" + remoteChannel + ";attr is "
                                    + backattrs);
                    throw new Exception(new Throwable(
                            "the send buffer2 is full,so disconnect it!please check remote client, Connection info:"
                                    + remoteChannel + ";attr is " + backattrs));
                }
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("message received");
        if (msg == null) {
            logger.error("get null msg, just skip");
            this.addMetric(false, 0, null);
            return;
        }
        ByteBuf cb = (ByteBuf) msg;
        try {
            Channel remoteChannel = ctx.channel();
            String strRemoteIP = getRemoteIp(remoteChannel);
            int len = cb.readableBytes();
            if (len == 0 && this.filterEmptyMsg) {
                logger.warn("skip empty msg.");
                this.addMetric(false, 0, null);
                return;
            }

            Map<String, Object> resultMap = null;
            try {
                resultMap = serviceDecoder.extractData(cb, remoteChannel);
            } catch (MessageIDException ex) {
                logger.error("MessageIDException ex = {}", ex);
                this.addMetric(false, 0, null);
                throw new IOException(ex.getCause());
            }

            if (resultMap == null) {
                logger.info("result is null");
                this.addMetric(false, 0, null);
                return;
            }

            MsgType msgType = (MsgType) resultMap.get(ConfigConstants.MSG_TYPE);
            if (MsgType.MSG_HEARTBEAT.equals(msgType)) {
                ByteBuf heartbeatBuffer = ByteBufAllocator.DEFAULT.buffer(5);
                heartbeatBuffer.writeBytes(new byte[]{0, 0, 0, 1, 1});
                remoteChannel.writeAndFlush(heartbeatBuffer);
                this.addMetric(false, 0, null);
                return;
            }

            if (MsgType.MSG_BIN_HEARTBEAT.equals(msgType)) {
                this.addMetric(false, 0, null);
                return;
            }

            Map<String, String> commonAttrMap =
                    (Map<String, String>) resultMap.get(ConfigConstants.COMMON_ATTR_MAP);
            if (commonAttrMap == null) {
                commonAttrMap = new HashMap<String, String>();
            }

            List<ProxyMessage> msgList = (List<ProxyMessage>) resultMap.get(ConfigConstants.MSG_LIST);

            boolean checkMessageTopic = true;
            if (msgList != null
                    && !commonAttrMap.containsKey(ConfigConstants.FILE_CHECK_DATA)
                    && !commonAttrMap.containsKey(ConfigConstants.MINUTE_CHECK_DATA)) {
                Map<String, HashMap<String, List<ProxyMessage>>> messageMap =
                        new HashMap<String, HashMap<String, List<ProxyMessage>>>(
                                msgList.size());

                checkMessageTopic = updateMsgList(msgList, commonAttrMap, messageMap, strRemoteIP,
                        msgType);
                if (checkMessageTopic) {
                    formatMessagesAndSend(ctx, commonAttrMap, messageMap,
                            strRemoteIP, msgType);
                }
            } else if (msgList != null && commonAttrMap.containsKey(ConfigConstants.FILE_CHECK_DATA)) {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("msgtype", "filestatus");
                headers.put(ConfigConstants.FILE_CHECK_DATA,
                        "true");
                headers.put(AttributeConstants.UNIQ_ID,
                        commonAttrMap.get(AttributeConstants.UNIQ_ID));
                for (ProxyMessage message : msgList) {
                    byte[] body = message.getData();
                    Event event = EventBuilder.withBody(body, headers);
                    if (MessageUtils.isSyncSendForOrder(commonAttrMap
                            .get(AttributeConstants.MESSAGE_SYNC_SEND))) {
                        event = new OrderEvent(ctx, event);
                    }
                    try {
                        processor.processEvent(event);
                        this.addMetric(true, body.length, event);
                    } catch (Throwable ex) {
                        logger.error("Error writing to controller,data will discard.", ex);
                        this.addMetric(false, body.length, event);
                        throw new ChannelException(
                                "Process Controller Event error can't write event to channel.");
                    }
                }
            } else if (msgList != null && commonAttrMap
                    .containsKey(ConfigConstants.MINUTE_CHECK_DATA)) {
                logger.info("i am in MINUTE_CHECK_DATA");
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("msgtype", "measure");
                headers.put(ConfigConstants.FILE_CHECK_DATA,
                        "true");
                headers.put(AttributeConstants.UNIQ_ID,
                        commonAttrMap.get(AttributeConstants.UNIQ_ID));
                for (ProxyMessage message : msgList) {
                    byte[] body = message.getData();
                    Event event = EventBuilder.withBody(body, headers);
                    if (MessageUtils.isSyncSendForOrder(commonAttrMap
                            .get(AttributeConstants.MESSAGE_SYNC_SEND))) {
                        event = new OrderEvent(ctx, event);
                    }
                    try {
                        processor.processEvent(event);
                        this.addMetric(true, body.length, event);
                    } catch (Throwable ex) {
                        logger.error("Error writing to controller,data will discard.", ex);
                        this.addMetric(false, body.length, event);
                        throw new ChannelException(
                                "Process Controller Event error can't write event to channel.");
                    }
                }
            }
            SocketAddress remoteSocketAddress = remoteChannel.remoteAddress();
            if (!checkMessageTopic || !MessageUtils.isSyncSendForOrder(commonAttrMap
                    .get(AttributeConstants.MESSAGE_SYNC_SEND))) {
                responsePackage(ctx, commonAttrMap, resultMap, remoteChannel,
                        remoteSocketAddress, msgType);
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
     * addMetric
     * 
     * @param result
     * @param size
     * @param event
     */
    private void addMetric(boolean result, long size, Event event) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, "DataProxy");
        dimensions.put(DataProxyMetricItem.KEY_SOURCE_ID, source.getName());
        dimensions.put(DataProxyMetricItem.KEY_SOURCE_DATA_ID, source.getName());
        DataProxyMetricItem.fillInlongId(event, dimensions);
        DataProxyMetricItem.fillAuditFormatTime(event, dimensions);
        DataProxyMetricItem metricItem = this.metricItemSet.findMetricItem(dimensions);
        if (result) {
            metricItem.readSuccessCount.incrementAndGet();
            metricItem.readSuccessSize.addAndGet(size);
            try {
                AuditUtils.add(AuditUtils.AUDIT_ID_DATAPROXY_READ_SUCCESS, event);
            } catch (Exception e) {
                logger.error("add metric has exception e= {}", e);
            }
        } else {
            metricItem.readFailCount.incrementAndGet();
            metricItem.readFailSize.addAndGet(size);
        }
    }
}
