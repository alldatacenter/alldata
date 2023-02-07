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

package org.apache.inlong.dataproxy.source.tcp;

import org.apache.flume.Event;
import org.apache.inlong.dataproxy.config.holder.CommonPropertiesHolder;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItem;
import org.apache.inlong.dataproxy.metrics.audit.AuditUtils;
import org.apache.inlong.dataproxy.source.SourceContext;
import org.apache.inlong.sdk.commons.protocol.EventUtils;
import org.apache.inlong.sdk.commons.protocol.ProxyEvent;
import org.apache.inlong.sdk.commons.protocol.ProxyPackEvent;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessagePack;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessagePackHeader;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.ResponseInfo;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * InlongTcpChannelHandler
 */
public class InlongTcpChannelHandler extends ChannelInboundHandlerAdapter {

    public static final Logger LOG = LoggerFactory.getLogger(InlongTcpChannelHandler.class);
    public static final int LENGTH_PARAM_OFFSET = 0;
    public static final int LENGTH_PARAM_LENGTH = 4;
    public static final int VERSION_PARAM_OFFSET = 4;
    public static final int VERSION_PARAM_LENGTH = 2;
    public static final int BODY_PARAM_OFFSET = 6;

    public static final int VERSION_1 = 1;

    private SourceContext sourceContext;

    /**
     * Constructor
     * 
     * @param sourceContext
     */
    public InlongTcpChannelHandler(SourceContext sourceContext) {
        this.sourceContext = sourceContext;
    }

    /**
     * channelRead
     * 
     * @param  ctx
     * @param  msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LOG.debug("message received");
        if (msg == null) {
            LOG.error("get null msg, just skip");
            this.addMetric(false, 0, null);
            return;
        }
        ByteBuf cb = (ByteBuf) msg;
        try {
            int readableLength = cb.readableBytes();
            if (readableLength == 0) {
                LOG.warn("skip empty msg.");
                cb.clear();
                this.addMetric(false, 0, null);
                return;
            }
            if (readableLength > LENGTH_PARAM_LENGTH + VERSION_PARAM_LENGTH + sourceContext.getMaxMsgLength()) {
                this.addMetric(false, 0, null);
                throw new Exception("err msg, MSG_MAX_LENGTH_BYTES "
                        + "< readableLength, and readableLength=" + readableLength + ", and MSG_MAX_LENGTH_BYTES="
                        + sourceContext.getMaxMsgLength());
            }
            // save index, reset it if buffer is not satisfied.
            cb.markReaderIndex();
            int totalPackLength = cb.readInt();
            if (readableLength < totalPackLength + LENGTH_PARAM_LENGTH) {
                // reset index.
                cb.resetReaderIndex();
                this.addMetric(false, 0, null);
                throw new Exception("err msg, channel buffer is not satisfied, and  readableLength="
                        + readableLength + ", and totalPackLength=" + totalPackLength);
            }

            // read version
            int version = cb.readShort();
            switch (version) {
                case VERSION_1:
                    // decode version 1
                    int bodyLength = totalPackLength - VERSION_PARAM_LENGTH;
                    decodeVersion1(ctx, cb, bodyLength);
                    break;
                default:
                    this.addMetric(false, 0, null);
                    throw new Exception("err version, unknown version:" + version);
            }
        } finally {
            cb.release();
        }
    }

    private void decodeVersion1(ChannelHandlerContext ctx, ByteBuf cb, int bodyLength) throws Exception {
        // read bytes
        byte[] msgBytes = new byte[bodyLength];
        cb.readBytes(msgBytes);
        // decode
        MessagePack packObject = MessagePack.parseFrom(msgBytes);
        // reject service
        if (sourceContext.isRejectService()) {
            this.addMetric(false, 0, null);
            this.responsePackage(ctx, ResultCode.ERR_REJECT, packObject);
            return;
        }
        // uncompress
        List<ProxyEvent> events = EventUtils.decodeSdkPack(packObject);
        // response success if event size is zero
        if (events.size() == 0) {
            this.responsePackage(ctx, ResultCode.SUCCUSS, packObject);
        }
        // process
        if (!CommonPropertiesHolder.isResponseAfterSave()) {
            this.processAndResponse(ctx, packObject, events);
        } else {
            this.processAndWaitingSave(ctx, packObject, events);
        }
    }

    /**
     * processAndWaitingSave
     * @param ctx
     * @param packObject
     * @param events
     * @throws Exception
     */
    private void processAndWaitingSave(ChannelHandlerContext ctx, MessagePack packObject, List<ProxyEvent> events)
            throws Exception {
        MessagePackHeader header = packObject.getHeader();
        InlongTcpSourceCallback callback = new InlongTcpSourceCallback(ctx, header);
        String inlongGroupId = header.getInlongGroupId();
        String inlongStreamId = header.getInlongStreamId();
        ProxyPackEvent packEvent = new ProxyPackEvent(inlongGroupId, inlongStreamId, events, callback);
        // put to channel
        try {
            sourceContext.getSource().getChannelProcessor().processEvent(packEvent);
            events.forEach(event -> {
                this.addMetric(true, event.getBody().length, event);
            });
            boolean awaitResult = callback.getLatch().await(CommonPropertiesHolder.getMaxResponseTimeout(),
                    TimeUnit.MILLISECONDS);
            if (!awaitResult) {
                if (!callback.getHasResponsed().getAndSet(true)) {
                    this.responsePackage(ctx, ResultCode.ERR_REJECT, packObject);
                }
            }
        } catch (Throwable ex) {
            LOG.error("Process Controller Event error can't write event to channel.", ex);
            events.forEach(event -> {
                this.addMetric(false, event.getBody().length, event);
            });
            if (!callback.getHasResponsed().getAndSet(true)) {
                this.responsePackage(ctx, ResultCode.ERR_REJECT, packObject);
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
    private void processAndResponse(ChannelHandlerContext ctx, MessagePack packObject, List<ProxyEvent> events)
            throws Exception {
        for (ProxyEvent event : events) {
            String uid = event.getUid();
            String topic = sourceContext.getIdHolder().getTopic(uid);
            if (topic != null) {
                event.setTopic(topic);
            }
            // put to channel
            try {
                sourceContext.getSource().getChannelProcessor().processEvent(event);
                this.addMetric(true, event.getBody().length, event);
            } catch (Throwable ex) {
                LOG.error("Process Controller Event error can't write event to channel.", ex);
                this.addMetric(false, event.getBody().length, event);
                this.responsePackage(ctx, ResultCode.ERR_REJECT, packObject);
                return;
            }
        }
        this.responsePackage(ctx, ResultCode.SUCCUSS, packObject);
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
        dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, sourceContext.getProxyClusterId());
        dimensions.put(DataProxyMetricItem.KEY_SOURCE_ID, sourceContext.getSourceId());
        dimensions.put(DataProxyMetricItem.KEY_SOURCE_DATA_ID, sourceContext.getSourceDataId());
        DataProxyMetricItem.fillInlongId(event, dimensions);
        DataProxyMetricItem.fillAuditFormatTime(event, dimensions);
        DataProxyMetricItem metricItem = this.sourceContext.getMetricItemSet().findMetricItem(dimensions);
        if (result) {
            metricItem.readSuccessCount.incrementAndGet();
            metricItem.readSuccessSize.addAndGet(size);
            AuditUtils.add(AuditUtils.AUDIT_ID_DATAPROXY_READ_SUCCESS, event);
        } else {
            metricItem.readFailCount.incrementAndGet();
            metricItem.readFailSize.addAndGet(size);
        }
    }

    /**
     * responsePackage
     *
     * @param  ctx
     * @param  code
     * @throws Exception
     */
    private void responsePackage(ChannelHandlerContext ctx, ResultCode code, MessagePack packObject)
            throws Exception {
        ResponseInfo.Builder builder = ResponseInfo.newBuilder();
        builder.setResult(code);
        MessagePackHeader header = packObject.getHeader();
        builder.setPackId(header.getPackId());

        // encode
        byte[] responseBytes = builder.build().toByteArray();
        //
        ByteBuf buffer = Unpooled.wrappedBuffer(responseBytes);
        Channel remoteChannel = ctx.channel();
        if (remoteChannel.isWritable()) {
            remoteChannel.write(buffer);
        } else {
            LOG.warn(
                    "the send buffer2 is full, so disconnect it!please check remote client"
                            + "; Connection info:" + remoteChannel);
            buffer.release();
            throw new Exception(
                    "the send buffer2 is full,so disconnect it!please check remote client, Connection info:"
                            + remoteChannel);
        }
    }

    /**
     * exceptionCaught
     * 
     * @param  ctx
     * @param  cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("exception caught cause = {}", cause);
        ctx.fireExceptionCaught(cause);
        if (ctx.channel() != null) {
            try {
                ctx.channel().disconnect();
                ctx.channel().close();
            } catch (Exception ex) {
                LOG.error("Close connection error!", ex);
            }
            sourceContext.getAllChannels().remove(ctx.channel());
        }
    }

    /**
     * channelInactive
     *
     * @param  ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.debug("Connection to {} disconnected.", ctx.channel());
        ctx.fireChannelInactive();
        try {
            ctx.channel().disconnect();
            ctx.channel().close();
        } catch (Exception ex) {
            LOG.error("channelInactive has exception e = {}", ex);
        }
        sourceContext.getAllChannels().remove(ctx.channel());

    }

    /**
     * channelActive
     *
     * @param  ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (sourceContext.getAllChannels().size() - 1 >= sourceContext.getMaxConnections()) {
            LOG.warn("refuse to connect , and connections="
                    + (sourceContext.getAllChannels().size() - 1)
                    + ", maxConnections="
                    + sourceContext.getMaxConnections() + ",channel is " + ctx.channel());
            ctx.channel().disconnect();
            ctx.channel().close();
        }
    }
}
