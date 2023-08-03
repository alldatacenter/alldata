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

import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.common.monitor.LogCounter;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.common.msg.MsgType;
import org.apache.inlong.common.util.NetworkUtils;
import org.apache.inlong.dataproxy.base.SinkRspEvent;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.source.ServerMessageHandler;
import org.apache.inlong.sdk.commons.protocol.EventConstants;
import org.apache.inlong.sdk.commons.protocol.InlongId;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flume.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple Pack Profile
 * 
 */
public class SimplePackProfile extends PackProfile {

    private static final Logger logger = LoggerFactory.getLogger(SimplePackProfile.class);
    // log print count
    private static final LogCounter logCounter = new LogCounter(10, 100000, 30 * 1000);
    private static final long MINUTE_MS = 60L * 1000;
    private boolean needRspEvent = false;
    private Channel channel;
    private MsgType msgType;
    private Event event;

    /**
     * Constructor
     * @param uid   the inlong id
     * @param inlongGroupId   the group id
     * @param inlongStreamId  the stream id
     * @param dispatchTime    the received time
     */
    public SimplePackProfile(String uid, String inlongGroupId, String inlongStreamId, long dispatchTime) {
        super(uid, inlongGroupId, inlongStreamId, dispatchTime);
    }

    @Override
    public void ack() {
        if (!this.needRspEvent) {
            return;
        }
        responseV0Msg(DataProxyErrCode.SUCCESS, "");
    }

    @Override
    public void fail(DataProxyErrCode errCode, String errMsg) {
        if (!needRspEvent) {
            return;
        }
        responseV0Msg(errCode, errMsg);
    }

    @Override
    public boolean isResend() {
        return !needRspEvent
                && enableRetryAfterFailure
                && (maxRetries < 0 || ++retries <= maxRetries);
    }

    @Override
    public boolean addEvent(Event event, long maxPackCount, long maxPackSize) {
        setCount(1);
        setSize(event.getBody().length);
        if (event instanceof SinkRspEvent) {
            SinkRspEvent rspEvent = (SinkRspEvent) event;
            this.needRspEvent = true;
            this.event = rspEvent.getEvent();
            this.channel = rspEvent.getChannel();
            this.msgType = rspEvent.getMsgType();
        } else {
            this.event = event;
            this.needRspEvent = false;
        }
        return true;
    }

    /**
     * create simple pack profile
     * @param event  the event to process
     * @return  the package profile
     */
    public static SimplePackProfile create(Event event) {
        Map<String, String> headers = event.getHeaders();
        String inlongGroupId = headers.get(AttributeConstants.GROUP_ID);
        String inlongStreamId = headers.get(AttributeConstants.STREAM_ID);
        String uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
        long msgTime = NumberUtils.toLong(headers.get(AttributeConstants.DATA_TIME), System.currentTimeMillis());
        long dispatchTime = msgTime - msgTime % MINUTE_MS;
        SimplePackProfile profile = new SimplePackProfile(uid, inlongGroupId, inlongStreamId,
                dispatchTime);
        profile.setCount(1);
        profile.setSize(event.getBody().length);
        if (event instanceof SinkRspEvent) {
            SinkRspEvent rspEvent = (SinkRspEvent) event;
            profile.needRspEvent = true;
            profile.event = rspEvent.getEvent();
            profile.channel = rspEvent.getChannel();
            profile.msgType = rspEvent.getMsgType();
        } else {
            profile.event = event;
        }
        return profile;
    }

    public Event getEvent() {
        return event;
    }

    /**
     * get properties
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return event.getHeaders();
    }

    /**
     * get required properties sent to MQ
     *
     * @return the properties
     */
    public Map<String, String> getPropsToMQ() {
        Map<String, String> result = new HashMap<>();
        result.put(AttributeConstants.RCV_TIME, event.getHeaders().get(AttributeConstants.RCV_TIME));
        result.put(ConfigConstants.MSG_ENCODE_VER, event.getHeaders().get(ConfigConstants.MSG_ENCODE_VER));
        result.put(EventConstants.HEADER_KEY_VERSION, event.getHeaders().get(EventConstants.HEADER_KEY_VERSION));
        result.put(ConfigConstants.REMOTE_IP_KEY, event.getHeaders().get(ConfigConstants.REMOTE_IP_KEY));
        result.put(ConfigConstants.DATAPROXY_IP_KEY, NetworkUtils.getLocalIp());
        return result;
    }

    /**
     *  Return response to client in source
     */
    private void responseV0Msg(DataProxyErrCode errCode, String errMsg) {
        try {
            String uid = event.getHeaders().get(AttributeConstants.UNIQ_ID);
            if ("false".equals(event.getHeaders().get(AttributeConstants.MESSAGE_IS_ACK))) {
                if (logger.isDebugEnabled()) {
                    logger.debug("not need to rsp message: seqId = {}, inlongGroupId = {}, inlongStreamId = {}",
                            uid, this.getInlongGroupId(), this.getInlongStreamId());
                }
                return;
            }
            // check channel status
            if (channel == null || !channel.isWritable()) {
                if (logCounter.shouldPrint()) {
                    logger.warn("Prepare send msg but channel full, msgType={}, attr={}, channel={}",
                            msgType, event.getHeaders(), channel);
                }
                return;
            }
            // build return attribute string
            StringBuilder strBuff = new StringBuilder(512);
            if (errCode != DataProxyErrCode.SUCCESS) {
                strBuff.append(AttributeConstants.MESSAGE_PROCESS_ERRCODE)
                        .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(errCode.getErrCodeStr());
                if (StringUtils.isNotEmpty(errMsg)) {
                    strBuff.append(AttributeConstants.SEPARATOR).append(AttributeConstants.MESSAGE_PROCESS_ERRMSG)
                            .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(errMsg);
                }
            }
            String origAttr = event.getHeaders().getOrDefault(ConfigConstants.DECODER_ATTRS, "");
            if (StringUtils.isNotEmpty(origAttr)) {
                if (strBuff.length() > 0) {
                    strBuff.append(AttributeConstants.SEPARATOR).append(origAttr);
                } else {
                    strBuff.append(origAttr);
                }
            }
            // build and send response message
            ByteBuf retData;
            if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
                retData = ServerMessageHandler.buildBinMsgRspPackage(strBuff.toString(), Long.parseLong(uid));
            } else {
                retData = ServerMessageHandler.buildTxtMsgRspPackage(msgType, strBuff.toString());
            }
            strBuff.delete(0, strBuff.length());
            if (channel == null || !channel.isWritable()) {
                // release allocated ByteBuf
                retData.release();
                if (logCounter.shouldPrint()) {
                    logger.warn("Send msg but channel full, attr={}, channel={}", event.getHeaders(), channel);
                }
                return;
            }
            channel.writeAndFlush(strBuff);
        } catch (Throwable e) {
            //
            if (logCounter.shouldPrint()) {
                logger.warn("Send msg but failure, attr={}", event.getHeaders(), e);
            }
        }
    }
}
