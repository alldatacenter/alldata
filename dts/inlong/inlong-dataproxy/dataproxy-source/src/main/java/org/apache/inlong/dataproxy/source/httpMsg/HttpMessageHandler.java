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

package org.apache.inlong.dataproxy.source.httpMsg;

import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.common.enums.DataProxyMsgEncType;
import org.apache.inlong.common.monitor.LogCounter;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.consts.AttrConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.consts.HttpAttrConst;
import org.apache.inlong.dataproxy.consts.StatConstants;
import org.apache.inlong.dataproxy.source.BaseSource;
import org.apache.inlong.dataproxy.utils.AddressUtils;
import org.apache.inlong.dataproxy.utils.DateTimeUtils;
import org.apache.inlong.sdk.commons.protocol.EventConstants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flume.Event;
import org.apache.flume.event.EventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;

/**
 * HTTP Server message handler
 */
public class HttpMessageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(HttpMessageHandler.class);
    // log print count
    private static final LogCounter logCounter = new LogCounter(10, 100000, 30 * 1000);
    private final BaseSource source;

    /**
     * Constructor
     *
     * @param source AbstractSource
     */
    public HttpMessageHandler(BaseSource source) {
        this.source = source;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        // process 100-continue request
        if (is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }
        // get current time and clientIP
        final long msgRcvTime = System.currentTimeMillis();
        final String clientIp = AddressUtils.getChannelRemoteIP(ctx.channel());
        // check request decode result
        if (!req.decoderResult().isSuccess()) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_DECODE_FAIL);
            sendErrorMsg(ctx, DataProxyErrCode.HTTP_DECODE_REQ_FAILURE);
            return;
        }
        // check service status.
        if (source.isRejectService()) {
            source.fileMetricIncSumStats(StatConstants.EVENT_SERVICE_CLOSED);
            sendErrorMsg(ctx, DataProxyErrCode.SERVICE_CLOSED);
            return;
        }
        // check sink service status
        if (!ConfigManager.getInstance().isMqClusterReady()) {
            source.fileMetricIncSumStats(StatConstants.EVENT_SERVICE_SINK_UNREADY);
            sendErrorMsg(ctx, DataProxyErrCode.SINK_SERVICE_UNREADY);
            return;
        }
        // check request method
        if (req.method() != HttpMethod.GET && req.method() != HttpMethod.POST) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_METHOD_INVALID);
            sendErrorMsg(ctx, DataProxyErrCode.HTTP_UNSUPPORTED_METHOD,
                    "Only support [" + HttpMethod.GET.name() + ", "
                            + HttpMethod.POST.name() + "] methods");
            return;
        }
        // parse request uri
        QueryStringDecoder uriDecoder =
                new QueryStringDecoder(req.uri(), Charsets.toCharset(CharEncoding.UTF_8));
        // check requested service url
        if (!HttpAttrConst.KEY_SRV_URL_HEARTBEAT.equals(uriDecoder.path())
                && !HttpAttrConst.KEY_SRV_URL_REPORT_MSG.equals(uriDecoder.path())) {
            if (!HttpAttrConst.KEY_URL_FAVICON_ICON.equals(uriDecoder.path())) {
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_PATH_INVALID);
                sendErrorMsg(ctx, DataProxyErrCode.HTTP_UNSUPPORTED_SERVICE_URI,
                        "Only support [" + HttpAttrConst.KEY_SRV_URL_HEARTBEAT + ", "
                                + HttpAttrConst.KEY_SRV_URL_REPORT_MSG + "] paths!");
            }
            return;
        }
        // get connection status
        boolean closeConnection = isCloseConnection(req);
        // process hb service
        if (HttpAttrConst.KEY_SRV_URL_HEARTBEAT.equals(uriDecoder.path())) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_HB_SUCCESS);
            sendResponse(ctx, closeConnection);
            return;
        }
        // get request attributes
        final Map<String, String> reqAttrs = new HashMap<>();
        getAttrsFromDecoder(uriDecoder, reqAttrs);
        if (req.method() == HttpMethod.POST) {
            // check and get content value
            String cntLengthStr = req.headers().get(HttpHeaderNames.CONTENT_LENGTH);
            if (StringUtils.isNotBlank(cntLengthStr) && NumberUtils.toInt(cntLengthStr, 0) > 0) {
                String cntType = req.headers().get(HttpHeaderNames.CONTENT_TYPE);
                if (StringUtils.isNotBlank(cntType)) {
                    cntType = cntType.trim();
                    if (!cntType.equalsIgnoreCase(
                            HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())) {
                        source.fileMetricIncSumStats(StatConstants.EVENT_MSG_CONTYPE_INVALID);
                        sendErrorMsg(ctx, DataProxyErrCode.HTTP_UNSUPPORTED_CONTENT_TYPE,
                                "Only support [" + HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED
                                        + "] content type!");
                        return;
                    }
                    String cntStr = req.content().toString(Charsets.toCharset(CharEncoding.UTF_8));
                    QueryStringDecoder cntDecoder = new QueryStringDecoder(cntStr, false);
                    getAttrsFromDecoder(cntDecoder, reqAttrs);
                }
            }
        }
        // process message request
        processMessage(ctx, reqAttrs, msgRcvTime, clientIp, closeConnection);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        source.fileMetricIncSumStats(StatConstants.EVENT_VISIT_LINKIN);
        // check illegal ip
        if (ConfigManager.getInstance().needChkIllegalIP()) {
            String strRemoteIp = AddressUtils.getChannelRemoteIP(ctx.channel());
            if (strRemoteIp != null
                    && ConfigManager.getInstance().isIllegalIP(strRemoteIp)) {
                source.fileMetricIncSumStats(StatConstants.EVENT_VISIT_ILLEGAL);
                ctx.channel().disconnect();
                ctx.channel().close();
                if (logCounter.shouldPrint()) {
                    logger.error(strRemoteIp + " is Illegal IP, so refuse it !");
                }
                return;
            }
        }
        // check max allowed connection count
        if (source.getAllChannels().size() >= source.getMaxConnections()) {
            source.fileMetricIncSumStats(StatConstants.EVENT_VISIT_OVERMAX);
            ctx.channel().disconnect();
            ctx.channel().close();
            if (logCounter.shouldPrint()) {
                logger.warn("{} refuse to connect = {} , connections = {}, maxConnections = {}",
                        source.getName(), ctx.channel(), source.getAllChannels().size(), source.getMaxConnections());
            }
            return;
        }
        // add legal channel
        source.getAllChannels().add(ctx.channel());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        source.fileMetricIncSumStats(StatConstants.EVENT_VISIT_LINKOUT);
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        source.fileMetricIncSumStats(StatConstants.EVENT_VISIT_EXCEPTION);
        if (logCounter.shouldPrint()) {
            logger.warn("{} received an exception from channel {}",
                    source.getName(), ctx.channel(), cause);
        }
        if (cause instanceof IOException) {
            ctx.close();
        } else {
            sendErrorMsg(ctx, DataProxyErrCode.UNKNOWN_ERROR,
                    "Process message failure: " + cause.getMessage());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
            ctx.close();
        }
    }

    /**
     * Process http report message
     *
     * @param ctx the handler context
     * @param reqAttrs the attributes
     * @param msgRcvTime  the message received time
     * @param clientIp  the report ip
     * @param isCloseCon  whether close connection
     *
     * @return whether process success
     */
    private boolean processMessage(ChannelHandlerContext ctx, Map<String, String> reqAttrs,
            long msgRcvTime, String clientIp, boolean isCloseCon) throws Exception {
        StringBuilder strBuff = new StringBuilder(512);
        String groupId = reqAttrs.get(HttpAttrConst.KEY_GROUP_ID);
        if (StringUtils.isBlank(groupId)) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_GROUPID_MISSING);
            sendResponse(ctx, DataProxyErrCode.MISS_REQUIRED_GROUPID_ARGUMENT.getErrCode(),
                    strBuff.append("Field ").append(HttpAttrConst.KEY_GROUP_ID)
                            .append(" must exist and not blank!").toString(),
                    isCloseCon);
            return false;
        }
        // get and check streamId
        String streamId = reqAttrs.get(HttpAttrConst.KEY_STREAM_ID);
        if (StringUtils.isBlank(streamId)) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_STREAMID_MISSING);
            sendResponse(ctx, DataProxyErrCode.MISS_REQUIRED_STREAMID_ARGUMENT.getErrCode(),
                    strBuff.append("Field ").append(HttpAttrConst.KEY_STREAM_ID)
                            .append(" must exist and not blank!").toString(),
                    isCloseCon);
            return false;
        }
        // get and check topicName
        String topicName = ConfigManager.getInstance().getTopicName(groupId, streamId);
        if (StringUtils.isBlank(topicName)) {
            source.fileMetricIncSumStats(StatConstants.EVENT_CONFIG_TOPIC_MISSING);
            sendResponse(ctx, DataProxyErrCode.TOPIC_IS_BLANK.getErrCode(),
                    strBuff.append("Topic not configured for ").append(HttpAttrConst.KEY_GROUP_ID)
                            .append("(").append(groupId).append("),")
                            .append(HttpAttrConst.KEY_STREAM_ID)
                            .append("(,").append(streamId).append(")").toString(),
                    isCloseCon);
            return false;
        }
        // get and check dt
        long dataTime = msgRcvTime;
        String dt = reqAttrs.get(HttpAttrConst.KEY_DATA_TIME);
        if (StringUtils.isNotEmpty(dt)) {
            try {
                dataTime = Long.parseLong(dt);
            } catch (Throwable e) {
                //
            }
        }
        // get and check body
        String body = reqAttrs.get(HttpAttrConst.KEY_BODY);
        if (StringUtils.isBlank(body)) {
            if (body == null) {
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_BODY_MISSING);
                sendResponse(ctx, DataProxyErrCode.MISS_REQUIRED_BODY_ARGUMENT.getErrCode(),
                        strBuff.append("Field ").append(HttpAttrConst.KEY_BODY)
                                .append(" is not exist!").toString(),
                        isCloseCon);
            } else {
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_BODY_BLANK);
                sendResponse(ctx, DataProxyErrCode.EMPTY_MSG.getErrCode(),
                        strBuff.append("Field ").append(HttpAttrConst.KEY_BODY)
                                .append(" is Blank!").toString(),
                        isCloseCon);
            }
            return false;
        }
        if (body.length() > source.getMaxMsgLength()) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_BODY_OVERMAX);
            sendResponse(ctx, DataProxyErrCode.BODY_EXCEED_MAX_LEN.getErrCode(),
                    strBuff.append("Error msg, the ").append(HttpAttrConst.KEY_BODY)
                            .append(" length(").append(body.length())
                            .append(") is bigger than allowed length(")
                            .append(source.getMaxMsgLength()).append(")").toString(),
                    isCloseCon);
            return false;
        }
        // get message count
        int intMsgCnt = NumberUtils.toInt(reqAttrs.get(HttpAttrConst.KEY_MESSAGE_COUNT), 1);
        String strMsgCount = String.valueOf(intMsgCnt);
        // build message attributes
        InLongMsg inLongMsg = InLongMsg.newInLongMsg(source.isCompressed());
        strBuff.append("groupId=").append(groupId)
                .append("&streamId=").append(streamId)
                .append("&dt=").append(dataTime)
                .append("&clientIp=").append(clientIp)
                .append("&cnt=").append(strMsgCount)
                .append("&rt=").append(msgRcvTime)
                .append(AttributeConstants.SEPARATOR).append(AttributeConstants.MSG_RPT_TIME)
                .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(msgRcvTime);
        inLongMsg.addMsg(strBuff.toString(), body.getBytes(HttpAttrConst.VAL_DEF_CHARSET));
        byte[] inlongMsgData = inLongMsg.buildArray();
        long pkgTime = inLongMsg.getCreatetime();
        inLongMsg.reset();
        strBuff.delete(0, strBuff.length());
        // build flume event
        Map<String, String> eventHeaders = new HashMap<>();
        eventHeaders.put(AttributeConstants.GROUP_ID, groupId);
        eventHeaders.put(AttributeConstants.STREAM_ID, streamId);
        eventHeaders.put(ConfigConstants.TOPIC_KEY, topicName);
        eventHeaders.put(AttributeConstants.DATA_TIME, String.valueOf(dataTime));
        eventHeaders.put(ConfigConstants.REMOTE_IP_KEY, clientIp);
        eventHeaders.put(ConfigConstants.MSG_COUNTER_KEY, strMsgCount);
        eventHeaders.put(ConfigConstants.MSG_ENCODE_VER,
                DataProxyMsgEncType.MSG_ENCODE_TYPE_INLONGMSG.getStrId());
        eventHeaders.put(EventConstants.HEADER_KEY_VERSION,
                DataProxyMsgEncType.MSG_ENCODE_TYPE_INLONGMSG.getStrId());
        eventHeaders.put(AttributeConstants.RCV_TIME, String.valueOf(msgRcvTime));
        eventHeaders.put(ConfigConstants.PKG_TIME_KEY, DateTimeUtils.ms2yyyyMMddHHmm(pkgTime));
        Event event = EventBuilder.withBody(inlongMsgData, eventHeaders);
        // build metric data item
        dataTime = dataTime / 1000 / 60 / 10;
        dataTime = dataTime * 1000 * 60 * 10;
        String statsKey = strBuff.append(source.getProtocolName()).append(AttrConstants.SEP_HASHTAG)
                .append(groupId).append(AttrConstants.SEP_HASHTAG)
                .append(streamId).append(AttrConstants.SEP_HASHTAG)
                .append(clientIp).append(AttrConstants.SEP_HASHTAG)
                .append(source.getSrcHost()).append(AttrConstants.SEP_HASHTAG)
                .append("b2b").append(AttrConstants.SEP_HASHTAG)
                .append(DateTimeUtils.ms2yyyyMMddHHmm(dataTime)).append(AttrConstants.SEP_HASHTAG)
                .append(DateTimeUtils.ms2yyyyMMddHHmm(msgRcvTime)).toString();
        strBuff.delete(0, strBuff.length());
        try {
            source.getChannelProcessor().processEvent(event);
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_V0_POST_SUCCESS);
            source.fileMetricAddSuccCnt(statsKey, intMsgCnt, 1, event.getBody().length);
            source.addMetric(true, event.getBody().length, event);
            sendResponse(ctx, isCloseCon);
            return true;
        } catch (Throwable ex) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_V0_POST_FAILURE);
            source.fileMetricAddFailCnt(statsKey, 1);
            source.addMetric(false, event.getBody().length, event);
            sendErrorMsg(ctx, DataProxyErrCode.PUT_EVENT_TO_CHANNEL_FAILURE,
                    strBuff.append("Put event to channel failure: ").append(ex.getMessage()).toString());
            if (logCounter.shouldPrint()) {
                logger.error("Error writing HTTP event to channel failure.", ex);
            }
            return false;
        }
    }

    /**
     * Get attributes from decoder
     *
     * @param decoder the decode object
     * @param reqAttrs the attributes
     */
    private void getAttrsFromDecoder(QueryStringDecoder decoder, Map<String, String> reqAttrs) {
        for (Map.Entry<String, List<String>> attr : decoder.parameters().entrySet()) {
            if (attr == null
                    || attr.getKey() == null
                    || attr.getValue() == null
                    || attr.getValue().isEmpty()) {
                continue;
            }
            reqAttrs.put(attr.getKey(), attr.getValue().get(0));
        }
    }

    private boolean isCloseConnection(FullHttpRequest req) {
        String connStatus = req.headers().get(HttpHeaderNames.CONNECTION);
        if (connStatus == null) {
            return false;
        }
        connStatus = connStatus.trim();
        return connStatus.equalsIgnoreCase(HttpHeaderValues.CLOSE.toString());
    }

    private void sendErrorMsg(ChannelHandlerContext ctx, DataProxyErrCode errCodeObj) {
        sendResponse(ctx, errCodeObj.getErrCode(), errCodeObj.getErrMsg(), true);
    }

    private void sendErrorMsg(ChannelHandlerContext ctx, DataProxyErrCode errCodeObj, String errMsg) {
        sendResponse(ctx, errCodeObj.getErrCode(), errMsg, true);
    }

    private void sendResponse(ChannelHandlerContext ctx, boolean isClose) {
        sendResponse(ctx, DataProxyErrCode.SUCCESS.getErrCode(), DataProxyErrCode.SUCCESS.getErrMsg(), isClose);
    }

    private void sendResponse(ChannelHandlerContext ctx, int errCode, String errMsg, boolean isClose) {
        if (ctx == null || ctx.channel() == null) {
            return;
        }
        if (!ctx.channel().isWritable()) {
            source.fileMetricIncSumStats(StatConstants.EVENT_REMOTE_UNWRITABLE);
            if (logCounter.shouldPrint()) {
                logger.warn("Send msg but channel full, channel={}", ctx.channel());
            }
            return;
        }
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpAttrConst.RET_CNT_TYPE);
        StringBuilder builder =
                new StringBuilder().append("{\"code\":\"").append(errCode)
                        .append("\",\"msg\":\"").append(errMsg).append("\"}");
        ByteBuf buffer = Unpooled.copiedBuffer(builder.toString(), CharsetUtil.UTF_8);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
        response.content().writeBytes(buffer);
        buffer.release();
        ctx.writeAndFlush(response).addListener(new SendResultListener(isClose));
    }

    private class SendResultListener implements ChannelFutureListener {

        private final boolean isClose;

        public SendResultListener(boolean isClose) {
            this.isClose = isClose;
        }

        @Override
        public void operationComplete(ChannelFuture channelFuture) throws Exception {
            if (!channelFuture.isSuccess()) {
                Throwable throwable = channelFuture.cause();
                String clientIp = AddressUtils.getChannelRemoteIP(channelFuture.channel());
                if (logCounter.shouldPrint()) {
                    logger.error("Http return response to client {} failed, exception:{}, errmsg:{}",
                            clientIp, throwable, throwable.getLocalizedMessage());
                }
                channelFuture.channel().close();
            }
            if (isClose) {
                channelFuture.channel().close();
            }
        }
    }
}
