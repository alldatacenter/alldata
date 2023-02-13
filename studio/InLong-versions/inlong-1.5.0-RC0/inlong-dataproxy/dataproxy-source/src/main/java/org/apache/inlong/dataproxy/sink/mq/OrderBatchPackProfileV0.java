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

package org.apache.inlong.dataproxy.sink.mq;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.dataproxy.base.SinkRspEvent;
import org.apache.inlong.common.msg.MsgType;
import org.apache.inlong.sdk.commons.protocol.InlongId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * SimpleBatchPackProfileV0
 * 
 */
public class OrderBatchPackProfileV0 extends BatchPackProfile {

    public static final Logger LOG = LoggerFactory.getLogger(OrderBatchPackProfileV0.class);

    private SinkRspEvent orderProfile;

    /**
     * Constructor
     * @param uid
     * @param inlongGroupId
     * @param inlongStreamId
     * @param dispatchTime
     */
    public OrderBatchPackProfileV0(String uid, String inlongGroupId, String inlongStreamId, long dispatchTime) {
        super(uid, inlongGroupId, inlongStreamId, dispatchTime);
    }

    /**
     * create
     * @param event
     * @return
     */
    public static OrderBatchPackProfileV0 create(SinkRspEvent event) {
        Map<String, String> headers = event.getHeaders();
        String inlongGroupId = headers.get(AttributeConstants.GROUP_ID);;
        String inlongStreamId = headers.get(AttributeConstants.STREAM_ID);
        String uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
        long msgTime = NumberUtils.toLong(headers.get(AttributeConstants.DATA_TIME), System.currentTimeMillis());
        long dispatchTime = msgTime - msgTime % MINUTE_MS;
        OrderBatchPackProfileV0 profile = new OrderBatchPackProfileV0(uid, inlongGroupId, inlongStreamId,
                dispatchTime);
        profile.setCount(1);
        profile.setSize(event.getBody().length);
        profile.orderProfile = event;
        return profile;
    }

    /**
     * get event
     * @return the event
     */
    public SinkRspEvent getOrderProfile() {
        return orderProfile;
    }

    /**
     * ackOrder
     */
    public void ackOrder() {
        String sequenceId = orderProfile.getHeaders().get(AttributeConstants.UNIQ_ID);
        if ("false".equals(orderProfile.getHeaders().get(AttributeConstants.MESSAGE_IS_ACK))) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("not need to rsp message: seqId = {}, inlongGroupId = {}, inlongStreamId = {}",
                        sequenceId, this.getInlongGroupId(), this.getInlongStreamId());
            }
            return;
        }
        if (orderProfile.getCtx() != null && orderProfile.getCtx().channel().isActive()) {
            orderProfile.getCtx().channel().eventLoop().execute(() -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("order message rsp: seqId = {}, inlongGroupId = {}, inlongStreamId = {}", sequenceId,
                            this.getInlongGroupId(), this.getInlongStreamId());
                }
                ByteBuf binBuffer = getResponsePackage("", MsgType.MSG_BIN_MULTI_BODY, sequenceId);
                orderProfile.getCtx().writeAndFlush(binBuffer);
            });
        }
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
}
