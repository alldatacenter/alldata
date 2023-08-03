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

import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.common.monitor.LogCounter;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.common.msg.MsgType;
import org.apache.inlong.dataproxy.config.CommonConfigHolder;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.consts.AttrConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.consts.StatConstants;
import org.apache.inlong.dataproxy.source.v0msg.AbsV0MsgCodec;
import org.apache.inlong.dataproxy.source.v0msg.CodecBinMsg;
import org.apache.inlong.dataproxy.source.v0msg.CodecTextMsg;
import org.apache.inlong.dataproxy.source.v1msg.InlongTcpSourceCallback;
import org.apache.inlong.dataproxy.utils.AddressUtils;
import org.apache.inlong.dataproxy.utils.DateTimeUtils;
import org.apache.inlong.sdk.commons.protocol.EventUtils;
import org.apache.inlong.sdk.commons.protocol.ProxyEvent;
import org.apache.inlong.sdk.commons.protocol.ProxyPackEvent;
import org.apache.inlong.sdk.commons.protocol.ProxySdk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.dataproxy.source.ServerMessageFactory.INLONG_LENGTH_FIELD_LENGTH;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.BIN_HB_ATTRLEN_SIZE;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.BIN_HB_BODYLEN_OFFSET;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.BIN_HB_BODY_OFFSET;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.BIN_HB_FIXED_CONTENT_SIZE;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.BIN_HB_FORMAT_SIZE;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.BIN_HB_TOTALLEN_SIZE;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.BIN_HB_VERSION_OFFSET;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.BIN_MSG_FIXED_CONTENT_SIZE;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.BIN_MSG_MAGIC;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.TXT_MSG_FIXED_CONTENT_SIZE;

/**
 * Server message handler
 *
 */
public class ServerMessageHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerMessageHandler.class);
    // log print count
    private static final LogCounter logCounter = new LogCounter(10, 100000, 30 * 1000);

    private static final int INLONG_MSG_V1 = 1;

    private static final ConfigManager configManager = ConfigManager.getInstance();
    private final BaseSource source;

    /**
     * Constructor
     *
     * @param source AbstractSource
     */
    public ServerMessageHandler(BaseSource source) {
        this.source = source;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            source.fileMetricIncSumStats(StatConstants.EVENT_PKG_READABLE_EMPTY);
            return;
        }
        ByteBuf cb = (ByteBuf) msg;
        try {
            int readableLength = cb.readableBytes();
            if (readableLength == 0 && source.isFilterEmptyMsg()) {
                cb.clear();
                source.fileMetricIncSumStats(StatConstants.EVENT_PKG_READABLE_EMPTY);
                return;
            }
            if (readableLength > source.getMaxMsgLength()) {
                source.fileMetricIncSumStats(StatConstants.EVENT_PKG_READABLE_OVERMAX);
                throw new Exception("Error msg, readableLength(" + readableLength +
                        ") > max allowed message length (" + source.getMaxMsgLength() + ")");
            }
            // save index
            cb.markReaderIndex();
            // read total data length
            int totalDataLen = cb.readInt();
            if (readableLength < totalDataLen + INLONG_LENGTH_FIELD_LENGTH) {
                // reset index when buffer is not satisfied.
                cb.resetReaderIndex();
                source.fileMetricIncSumStats(StatConstants.EVENT_PKG_READABLE_UNFILLED);
                return;
            }
            // read type
            int msgTypeValue = cb.readByte();
            if (msgTypeValue == 0x0) {
                // process v1 messsages
                msgTypeValue = cb.readByte();
                if (msgTypeValue == INLONG_MSG_V1) {
                    // decode version 1
                    int bodyLength = totalDataLen - 2;
                    processV1Msg(ctx, cb, bodyLength);
                } else {
                    // unknown message type
                    source.fileMetricIncSumStats(StatConstants.EVENT_PKG_MSGTYPE_V1_INVALID);
                    throw new Exception("Unknown V1 message version, version = " + msgTypeValue);
                }
            } else {
                // process v0 messages
                Channel channel = ctx.channel();
                MsgType msgType = MsgType.valueOf(msgTypeValue);
                final long msgRcvTime = System.currentTimeMillis();
                if (MsgType.MSG_UNKNOWN == msgType) {
                    source.fileMetricIncSumStats(StatConstants.EVENT_PKG_MSGTYPE_V0_INVALID);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Received unknown message, channel {}", channel);
                    }
                    throw new Exception("Unknown V0 message type, type = " + msgTypeValue);
                } else if (MsgType.MSG_HEARTBEAT == msgType) {
                    // send response message
                    flushV0MsgPackage(source, channel,
                            buildHeartBeatMsgRspPackage(), MsgType.MSG_HEARTBEAT.name());
                    return;
                } else if (MsgType.MSG_BIN_HEARTBEAT == msgType) {
                    procBinHeartbeatMsg(source, channel, cb, totalDataLen);
                    return;
                }
                // process msgType in {2,3,4,5,6,7}
                AbsV0MsgCodec msgCodec;
                String strRemoteIP = AddressUtils.getChannelRemoteIP(channel);
                // check whether totalDataLen is valid.
                if (MsgType.MSG_BIN_MULTI_BODY == msgType) {
                    if (totalDataLen < BIN_MSG_FIXED_CONTENT_SIZE) {
                        source.fileMetricIncSumStats(StatConstants.EVENT_MSG_BIN_TOTALLEN_BELOWMIN);
                        String errMsg = String.format("Malformed msg, totalDataLen(%d) < min bin7-msg length(%d)",
                                totalDataLen, BIN_MSG_FIXED_CONTENT_SIZE);
                        if (logger.isDebugEnabled()) {
                            logger.debug(errMsg + ", channel {}", channel);
                        }
                        throw new Exception(errMsg);
                    }
                    msgCodec = new CodecBinMsg(totalDataLen, msgTypeValue, msgRcvTime, strRemoteIP);
                } else {
                    if (totalDataLen < TXT_MSG_FIXED_CONTENT_SIZE) {
                        source.fileMetricIncSumStats(StatConstants.EVENT_MSG_TXT_TOTALLEN_BELOWMIN);
                        String errMsg = String.format("Malformed msg, totalDataLen(%d) < min txt-msg length(%d)",
                                totalDataLen, TXT_MSG_FIXED_CONTENT_SIZE);
                        if (logger.isDebugEnabled()) {
                            logger.debug(errMsg + ", channel {}", channel);
                        }
                        throw new Exception(errMsg);
                    }
                    msgCodec = new CodecTextMsg(totalDataLen, msgTypeValue, msgRcvTime, strRemoteIP);
                }
                // process request
                processV0Msg(channel, cb, msgCodec);
            }
        } finally {
            cb.release();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // check illegal ip
        if (ConfigManager.getInstance().needChkIllegalIP()) {
            String strRemoteIp = AddressUtils.getChannelRemoteIP(ctx.channel());
            if (strRemoteIp != null
                    && ConfigManager.getInstance().isIllegalIP(strRemoteIp)) {
                source.fileMetricIncSumStats(StatConstants.EVENT_VISIT_ILLEGAL);
                ctx.channel().disconnect();
                ctx.channel().close();
                logger.error(strRemoteIp + " is Illegal IP, so refuse it !");
                return;
            }
        }
        // check max allowed connection count
        if (source.getAllChannels().size() >= source.getMaxConnections()) {
            source.fileMetricIncSumStats(StatConstants.EVENT_VISIT_OVERMAX);
            ctx.channel().disconnect();
            ctx.channel().close();
            logger.warn("{} refuse to connect = {} , connections = {}, maxConnections = {}",
                    source.getName(), ctx.channel(), source.getAllChannels().size(), source.getMaxConnections());
            return;
        }
        // add legal channel
        source.getAllChannels().add(ctx.channel());
        ctx.fireChannelActive();
        source.fileMetricIncSumStats(StatConstants.EVENT_VISIT_LINKIN);
        logger.info("{} added new channel {}, current connections = {}, maxConnections = {}",
                source.getName(), ctx.channel(), source.getAllChannels().size(), source.getMaxConnections());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        source.fileMetricIncSumStats(StatConstants.EVENT_VISIT_LINKOUT);
        ctx.fireChannelInactive();
        source.getAllChannels().remove(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        source.fileMetricIncSumStats(StatConstants.EVENT_VISIT_EXCEPTION);
        if (logCounter.shouldPrint()) {
            logger.warn("{} received an exception from channel {}",
                    source.getName(), ctx.channel(), cause);
        }
        if (ctx.channel() != null) {
            source.getAllChannels().remove(ctx.channel());
            try {
                ctx.channel().disconnect();
                ctx.channel().close();
            } catch (Exception ex) {
                //
            }
        }
        ctx.close();
    }

    private void processV0Msg(Channel channel, ByteBuf cb, AbsV0MsgCodec msgCodec) throws Exception {
        final StringBuilder strBuff = new StringBuilder(512);
        // decode the request message
        if (!msgCodec.descMsg(source, cb)) {
            responseV0Msg(channel, msgCodec, strBuff);
            return;
        }
        // check service status.
        if (source.isRejectService()) {
            source.fileMetricIncSumStats(StatConstants.EVENT_SERVICE_CLOSED);
            msgCodec.setFailureInfo(DataProxyErrCode.SERVICE_CLOSED);
            responseV0Msg(channel, msgCodec, strBuff);
            return;
        }
        // check if the node is linked to the Manager.
        if (!ConfigManager.getInstance().isMqClusterReady()) {
            source.fileMetricIncSumStats(StatConstants.EVENT_SERVICE_SINK_UNREADY);
            msgCodec.setFailureInfo(DataProxyErrCode.SINK_SERVICE_UNREADY);
            responseV0Msg(channel, msgCodec, strBuff);
            return;
        }
        // valid and fill extra fields.
        if (!msgCodec.validAndFillFields(source, strBuff)) {
            responseV0Msg(channel, msgCodec, strBuff);
            return;
        }
        // build InLong event.
        Event event = msgCodec.encEventPackage(source, channel);
        // build metric data item
        long longDataTime = msgCodec.getDataTimeMs() / 1000 / 60 / 10;
        longDataTime = longDataTime * 1000 * 60 * 10;
        String statsKey = strBuff.append(source.getProtocolName()).append(AttrConstants.SEP_HASHTAG)
                .append(msgCodec.getGroupId()).append(AttrConstants.SEP_HASHTAG)
                .append(msgCodec.getStreamId()).append(AttrConstants.SEP_HASHTAG)
                .append(msgCodec.getStrRemoteIP()).append(AttrConstants.SEP_HASHTAG)
                .append(source.getSrcHost()).append(AttrConstants.SEP_HASHTAG)
                .append(msgCodec.getMsgProcType()).append(AttrConstants.SEP_HASHTAG)
                .append(DateTimeUtils.ms2yyyyMMddHHmm(longDataTime)).append(AttrConstants.SEP_HASHTAG)
                .append(DateTimeUtils.ms2yyyyMMddHHmm(msgCodec.getMsgRcvTime())).toString();
        strBuff.delete(0, strBuff.length());
        try {
            source.getChannelProcessor().processEvent(event);
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_V0_POST_SUCCESS);
            source.fileMetricAddSuccCnt(statsKey, msgCodec.getMsgCount(), 1, event.getBody().length);
            source.addMetric(true, event.getBody().length, event);
            if (msgCodec.isNeedResp() && !msgCodec.isOrderOrProxy()) {
                msgCodec.setSuccessInfo();
                responseV0Msg(channel, msgCodec, strBuff);
            }
        } catch (Throwable ex) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_V0_POST_FAILURE);
            source.fileMetricAddFailCnt(statsKey, 1);
            source.addMetric(false, event.getBody().length, event);
            if (msgCodec.isNeedResp() && !msgCodec.isOrderOrProxy()) {
                msgCodec.setFailureInfo(DataProxyErrCode.PUT_EVENT_TO_CHANNEL_FAILURE,
                        strBuff.append("Put event to channel failure: ").append(ex.getMessage()).toString());
                strBuff.delete(0, strBuff.length());
                responseV0Msg(channel, msgCodec, strBuff);
            }
            if (logCounter.shouldPrint()) {
                logger.error("Error writing event to channel failure.", ex);
            }
        }
    }

    private void processV1Msg(ChannelHandlerContext ctx, ByteBuf cb, int bodyLength) throws Exception {
        // read bytes
        byte[] msgBytes = new byte[bodyLength];
        cb.readBytes(msgBytes);
        // decode
        ProxySdk.MessagePack packObject = ProxySdk.MessagePack.parseFrom(msgBytes);
        // reject service
        if (source.isRejectService()) {
            source.addMetric(false, 0, null);
            source.fileMetricIncSumStats(StatConstants.EVENT_SERVICE_CLOSED);
            this.responsePackage(ctx, ProxySdk.ResultCode.ERR_REJECT, packObject);
            return;
        }
        // uncompress
        List<ProxyEvent> events = EventUtils.decodeSdkPack(packObject);
        // response success if event size is zero
        if (events.size() == 0) {
            this.responsePackage(ctx, ProxySdk.ResultCode.SUCCUSS, packObject);
        }
        // process
        if (CommonConfigHolder.getInstance().isResponseAfterSave()) {
            this.processAndWaitingSave(ctx, packObject, events);
        } else {
            this.processAndResponse(ctx, packObject, events);
        }
    }

    /**
     * responsePackage
     *
     * @param  ctx
     * @param  code
     * @throws Exception
     */
    private void responsePackage(ChannelHandlerContext ctx,
            ProxySdk.ResultCode code,
            ProxySdk.MessagePack packObject) throws Exception {
        ProxySdk.ResponseInfo.Builder builder = ProxySdk.ResponseInfo.newBuilder();
        builder.setResult(code);
        ProxySdk.MessagePackHeader header = packObject.getHeader();
        builder.setPackId(header.getPackId());

        // encode
        byte[] responseBytes = builder.build().toByteArray();
        //
        ByteBuf buffer = Unpooled.wrappedBuffer(responseBytes);
        Channel remoteChannel = ctx.channel();
        if (remoteChannel.isWritable()) {
            remoteChannel.write(buffer);
        } else {
            buffer.release();
            logger.warn("Send buffer2 is not writable, disconnect {}", remoteChannel);
            throw new Exception("Send buffer2 is not writable, disconnect " + remoteChannel);
        }
    }

    /**
     * processAndWaitingSave
     * @param ctx
     * @param packObject
     * @param events
     * @throws Exception
     */
    private void processAndWaitingSave(ChannelHandlerContext ctx,
            ProxySdk.MessagePack packObject,
            List<ProxyEvent> events) throws Exception {
        ProxySdk.MessagePackHeader header = packObject.getHeader();
        InlongTcpSourceCallback callback = new InlongTcpSourceCallback(ctx, header);
        String inlongGroupId = header.getInlongGroupId();
        String inlongStreamId = header.getInlongStreamId();
        ProxyPackEvent packEvent = new ProxyPackEvent(inlongGroupId, inlongStreamId, events, callback);
        // put to channel
        try {
            source.getChannelProcessor().processEvent(packEvent);
            events.forEach(event -> {
                source.addMetric(true, event.getBody().length, event);
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_V1_POST_SUCCESS);
            });
            boolean awaitResult = callback.getLatch().await(
                    CommonConfigHolder.getInstance().getMaxResAfterSaveTimeout(), TimeUnit.MILLISECONDS);
            if (!awaitResult) {
                if (!callback.getHasResponsed().getAndSet(true)) {
                    this.responsePackage(ctx, ProxySdk.ResultCode.ERR_REJECT, packObject);
                }
            }
        } catch (Throwable ex) {
            logger.error("Process Controller Event error can't write event to channel.", ex);
            events.forEach(event -> {
                source.addMetric(false, event.getBody().length, event);
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_V1_POST_DROPPED);
            });
            if (!callback.getHasResponsed().getAndSet(true)) {
                this.responsePackage(ctx, ProxySdk.ResultCode.ERR_REJECT, packObject);
            }
        }
    }

    /**
     * processAndResponse
     * @param ctx
     * @param packObject
     * @param events
     * @throws Exception
     */
    private void processAndResponse(ChannelHandlerContext ctx,
            ProxySdk.MessagePack packObject,
            List<ProxyEvent> events) throws Exception {
        for (ProxyEvent event : events) {
            // get configured topic name
            String topic = configManager.getTopicName(event.getInlongGroupId(), event.getInlongStreamId());
            if (StringUtils.isBlank(topic)) {
                source.fileMetricIncSumStats(StatConstants.EVENT_CONFIG_TOPIC_MISSING);
                source.addMetric(false, event.getBody().length, event);
                this.responsePackage(ctx, ProxySdk.ResultCode.ERR_ID_ERROR, packObject);
                return;
            }
            event.setTopic(topic);
            // put to channel
            try {
                source.getChannelProcessor().processEvent(event);
                source.addMetric(true, event.getBody().length, event);
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_V1_POST_SUCCESS);
            } catch (Throwable ex) {
                logger.error("Process Controller Event error can't write event to channel.", ex);
                source.addMetric(false, event.getBody().length, event);
                this.responsePackage(ctx, ProxySdk.ResultCode.ERR_REJECT, packObject);
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_V1_POST_DROPPED);
                return;
            }
        }
        this.responsePackage(ctx, ProxySdk.ResultCode.SUCCUSS, packObject);
    }

    /**
     *  Return response to client in source
     */
    private void responseV0Msg(Channel channel, AbsV0MsgCodec msgObj, StringBuilder strBuff) throws Exception {
        // check channel status
        if (channel == null || !channel.isWritable()) {
            source.fileMetricIncSumStats(StatConstants.EVENT_REMOTE_UNWRITABLE);
            if (logCounter.shouldPrint()) {
                logger.warn("Prepare send msg but channel full, msgType={}, attr={}, channel={}",
                        msgObj.getMsgType(), msgObj.getAttr(), channel);
            }
            throw new Exception("Prepare send msg but channel full");
        }
        // check whether return response message
        if (!msgObj.isNeedResp()) {
            return;
        }
        // build return attribute string
        strBuff.append(ConfigConstants.DATAPROXY_IP_KEY)
                .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(source.getSrcHost());
        if (msgObj.getErrCode() != DataProxyErrCode.SUCCESS) {
            strBuff.append(AttributeConstants.SEPARATOR).append(AttributeConstants.MESSAGE_PROCESS_ERRCODE)
                    .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(msgObj.getErrCode().getErrCodeStr());
            if (StringUtils.isNotEmpty(msgObj.getErrMsg())) {
                strBuff.append(AttributeConstants.SEPARATOR).append(AttributeConstants.MESSAGE_PROCESS_ERRMSG)
                        .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(msgObj.getErrMsg());
            }
        }
        if (StringUtils.isNotEmpty(msgObj.getAttr())) {
            strBuff.append(AttributeConstants.SEPARATOR).append(msgObj.getAttr());
        }
        // build and send response message
        ByteBuf retData;
        MsgType msgType = MsgType.valueOf(msgObj.getMsgType());
        if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
            retData = buildBinMsgRspPackage(strBuff.toString(), msgObj.getUniq());
        } else {
            retData = buildTxtMsgRspPackage(msgType, strBuff.toString(), msgObj);
        }
        strBuff.delete(0, strBuff.length());
        flushV0MsgPackage(source, channel, retData, msgObj.getAttr());
    }

    /**
     * extract and process bin heart beat msg, message type is 8
     */
    private void procBinHeartbeatMsg(BaseSource source, Channel channel,
            ByteBuf cb, int totalDataLen) throws Exception {
        // Check if the message is complete and legal
        if (totalDataLen < BIN_HB_FIXED_CONTENT_SIZE) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_HB_TOTALLEN_BELOWMIN);
            String errMsg = String.format("Malformed msg, totalDataLen(%d) < min hb-msg length(%d)",
                    totalDataLen, BIN_HB_FIXED_CONTENT_SIZE);
            if (logger.isDebugEnabled()) {
                logger.debug(errMsg + ", channel {}", channel);
            }
            throw new Exception(errMsg);
        }
        // check validation
        int msgHeadPos = cb.readerIndex() - 5;
        int bodyLen = cb.getInt(msgHeadPos + BIN_HB_BODYLEN_OFFSET);
        int attrLen = cb.getShort(msgHeadPos + BIN_HB_BODY_OFFSET + bodyLen);
        int msgMagic = cb.getUnsignedShort(msgHeadPos
                + BIN_HB_BODY_OFFSET + bodyLen + BIN_HB_ATTRLEN_SIZE + attrLen);
        if (msgMagic != BIN_MSG_MAGIC) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_HB_MAGIC_UNEQUAL);
            String errMsg = String.format(
                    "Malformed msg, msgMagic(%d) != %d", msgMagic, BIN_MSG_MAGIC);
            if (logger.isDebugEnabled()) {
                logger.debug(errMsg + ", channel {}", channel);
            }
            throw new Exception(errMsg);
        }
        if (totalDataLen + BIN_HB_TOTALLEN_SIZE < (bodyLen + attrLen + BIN_HB_FORMAT_SIZE)) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_HB_LEN_MALFORMED);
            String errMsg = String.format(
                    "Malformed msg, bodyLen(%d) + attrLen(%d) > totalDataLen(%d)",
                    bodyLen, attrLen, totalDataLen);
            if (logger.isDebugEnabled()) {
                logger.debug(errMsg + ", channel {}", channel);
            }
            throw new Exception(errMsg);
        }
        // read message content
        byte version = cb.getByte(msgHeadPos + BIN_HB_VERSION_OFFSET);
        byte[] attrData = null;
        if (attrLen > 0) {
            attrData = new byte[attrLen];
            cb.getBytes(msgHeadPos + BIN_HB_BODY_OFFSET
                    + bodyLen + BIN_HB_ATTRLEN_SIZE, attrData, 0, attrLen);
        }
        // build and send response message
        flushV0MsgPackage(source, channel,
                buildHBRspPackage(attrData, version, 0), MsgType.MSG_BIN_HEARTBEAT.name());
    }

    /**
     * Build bin-msg response message ByteBuf
     *
     * @param attrs   the return attribute
     * @param uniqVal sequence Id
     * @return ByteBuf
     */
    public static ByteBuf buildBinMsgRspPackage(String attrs, long uniqVal) {
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
     * Build default-msg response message ByteBuf
     *
     * @param msgType  the message type
     * @param attrs    the return attribute
     * @return ByteBuf
     */
    public static ByteBuf buildTxtMsgRspPackage(MsgType msgType, String attrs) {
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
     * Build default-msg response message ByteBuf
     *
     * @param msgType  the message type
     * @param attrs    the return attribute
     * @param msgObj   the request message object
     * @return ByteBuf
     */
    private ByteBuf buildTxtMsgRspPackage(MsgType msgType, String attrs, AbsV0MsgCodec msgObj) {
        int attrsLen = 0;
        int bodyLen = 0;
        byte[] backBody = null;
        if (attrs != null) {
            attrsLen = attrs.length();
        }
        if (MsgType.MSG_ORIGINAL_RETURN.equals(msgType)) {
            backBody = msgObj.getOrigBody();
            if (backBody != null) {
                bodyLen = backBody.length;
            }
        }
        // backTotalLen = mstType + bodyLen + body + attrsLen + attrs
        int backTotalLen = 1 + 4 + bodyLen + 4 + attrsLen;
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(4 + backTotalLen);
        buffer.writeInt(backTotalLen);
        buffer.writeByte(msgType.getValue());
        buffer.writeInt(bodyLen);
        if (bodyLen > 0) {
            buffer.writeBytes(backBody);
        }
        buffer.writeInt(attrsLen);
        if (attrsLen > 0) {
            buffer.writeBytes(attrs.getBytes(StandardCharsets.UTF_8));
        }
        return buffer;
    }

    /**
     * Build heartbeat response message ByteBuf
     *
     * @param attrData  the attribute data
     * @param version   the version
     * @param loadValue the node load value
     * @return ByteBuf
     */
    private ByteBuf buildHBRspPackage(byte[] attrData, byte version, int loadValue) {
        // calculate total length
        // binTotalLen = mstType + dataTime + version + bodyLen + body + attrsLen + attrs + magic
        int binTotalLen = 1 + 4 + 1 + 4 + 2 + 2 + 2;
        if (null != attrData) {
            binTotalLen += attrData.length;
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
        if (null != attrData) {
            binBuffer.writeShort(attrData.length);
            binBuffer.writeBytes(attrData);
        } else {
            binBuffer.writeShort(0x0);
        }
        binBuffer.writeShort(0xee01);
        return binBuffer;
    }

    /**
     * Build hearbeat(1)-msg response message ByteBuf
     *
     * @return ByteBuf
     */
    private ByteBuf buildHeartBeatMsgRspPackage() {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(5);
        // magic data
        buffer.writeBytes(new byte[]{0, 0, 0, 1, 1});
        return buffer;
    }

    private void flushV0MsgPackage(BaseSource source, Channel channel,
            ByteBuf binBuffer, String orgAttr) throws Exception {
        if (channel == null || !channel.isWritable()) {
            // release allocated ByteBuf
            binBuffer.release();
            source.fileMetricIncSumStats(StatConstants.EVENT_REMOTE_UNWRITABLE);
            if (logCounter.shouldPrint()) {
                logger.warn("Send msg but channel full, attr={}, channel={}", orgAttr, channel);
            }
            throw new Exception("Send response but channel full");
        }
        channel.writeAndFlush(binBuffer);
    }
}
