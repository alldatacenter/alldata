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

package org.apache.inlong.dataproxy.http;

import static org.apache.inlong.dataproxy.consts.AttrConstants.SEP_HASHTAG;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.event.EventBuilder;
import org.apache.inlong.common.monitor.MonitorIndex;
import org.apache.inlong.common.monitor.MonitorIndexExt;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.common.util.NetworkUtils;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.consts.AttrConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.http.exception.MessageProcessException;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.apache.inlong.dataproxy.metrics.audit.AuditUtils;
import org.apache.inlong.dataproxy.source.ServiceDecoder;
import org.apache.inlong.dataproxy.utils.DateTimeUtils;
import org.apache.inlong.dataproxy.utils.InLongMsgVer;
import org.apache.inlong.dataproxy.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleMessageHandler implements MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleMessageHandler.class);
    private static final ConfigManager configManager = ConfigManager.getInstance();

    private static final String DEFAULT_REMOTE_IDC_VALUE = "0";
    private final MonitorIndex monitorIndex;
    private final MonitorIndexExt monitorIndexExt;
    private final DataProxyMetricItemSet metricItemSet;
    private final ChannelProcessor processor;

    @SuppressWarnings("unused")
    private int maxMsgLength;
    private long logCounter = 0L;
    private long channelTrace = 0L;

    public SimpleMessageHandler(ChannelProcessor processor, MonitorIndex monitorIndex,
            MonitorIndexExt monitorIndexExt, DataProxyMetricItemSet metricItemSet,
            ServiceDecoder decoder) {
        this.processor = processor;
        this.monitorIndex = monitorIndex;
        this.monitorIndexExt = monitorIndexExt;
        this.metricItemSet = metricItemSet;
        init();
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void processMessage(Context context) throws MessageProcessException {
        StringBuilder strBuff = new StringBuilder(512);
        // get groupId and streamId
        String groupId = (String) context.get(AttributeConstants.GROUP_ID);
        String streamId = (String) context.get(AttributeConstants.STREAM_ID);
        if (StringUtils.isBlank(groupId) || StringUtils.isBlank(streamId)) {
            throw new MessageProcessException(strBuff.append("Field ")
                    .append(AttributeConstants.GROUP_ID).append(" or ")
                    .append(AttributeConstants.STREAM_ID)
                    .append(" must exist and not blank!").toString());
        }
        groupId = groupId.trim();
        streamId = streamId.trim();
        // get topicName
        String topicName = "test";
        String configedTopicName = getTopic(groupId, streamId);
        if (StringUtils.isNotBlank(configedTopicName)) {
            topicName = configedTopicName.trim();
        }
        // get message data time
        final long msgRcvTime = System.currentTimeMillis();
        String strDataTime = (String) context.get(AttributeConstants.DATA_TIME);
        long longDataTime = NumberUtils.toLong(strDataTime, msgRcvTime);
        strDataTime = String.valueOf(longDataTime);
        // get char set
        String charset = (String) context.get(AttrConstants.CHARSET);
        if (StringUtils.isBlank(charset)) {
            charset = AttrConstants.CHARSET;
        }
        String body = (String) context.get(AttrConstants.BODY);
        if (StringUtils.isEmpty(body)) {
            throw new MessageProcessException(strBuff.append("Field ")
                    .append(AttrConstants.BODY)
                    .append(" must exist and not empty!").toString());
        }
        // get m attribute
        String mxValue = "m=0";
        String configedMxAttr = configManager.getMxProperties().get(groupId);
        if (StringUtils.isNotEmpty(configedMxAttr)) {
            mxValue = configedMxAttr.trim();
        }
        // convert context to http request
        HttpServletRequest request =
                (HttpServletRequest) context.get(AttrConstants.HTTP_REQUEST);
        // get report node ip
        String strRemoteIP = request.getRemoteAddr();
        // get message count
        String strMsgCount = request.getParameter(AttributeConstants.MESSAGE_COUNT);
        int intMsgCnt = NumberUtils.toInt(strMsgCount, 1);
        strMsgCount = String.valueOf(intMsgCnt);
        // build message attributes
        InLongMsg inLongMsg = InLongMsg.newInLongMsg(true);
        strBuff.append(mxValue).append("&groupId=").append(groupId)
                .append("&streamId=").append(streamId)
                .append("&dt=").append(strDataTime)
                .append("&NodeIP=").append(strRemoteIP)
                .append("&cnt=").append(strMsgCount)
                .append("&rt=").append(msgRcvTime)
                .append(AttributeConstants.SEPARATOR).append(AttributeConstants.MSG_RPT_TIME)
                .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(msgRcvTime);
        try {
            inLongMsg.addMsg(strBuff.toString(), body.getBytes(charset));
            strBuff.delete(0, strBuff.length());
        } catch (UnsupportedEncodingException e) {
            throw new MessageProcessException(e);
        }
        // build flume event
        Map<String, String> headers = new HashMap<>();
        headers.put(AttributeConstants.GROUP_ID, groupId);
        headers.put(AttributeConstants.STREAM_ID, streamId);
        headers.put(ConfigConstants.TOPIC_KEY, topicName);
        headers.put(AttributeConstants.DATA_TIME, strDataTime);
        headers.put(ConfigConstants.REMOTE_IP_KEY, strRemoteIP);
        headers.put(ConfigConstants.REMOTE_IDC_KEY, DEFAULT_REMOTE_IDC_VALUE);
        headers.put(ConfigConstants.MSG_COUNTER_KEY, strMsgCount);
        headers.put(ConfigConstants.MSG_ENCODE_VER, InLongMsgVer.INLONG_V0.getName());
        byte[] data = inLongMsg.buildArray();
        headers.put(AttributeConstants.RCV_TIME, String.valueOf(msgRcvTime));
        Event event = EventBuilder.withBody(data, headers);
        inLongMsg.reset();
        Pair<Boolean, String> evenProcType =
                MessageUtils.getEventProcType("", "");
        // build metric data item
        longDataTime = longDataTime / 1000 / 60 / 10;
        longDataTime = longDataTime * 1000 * 60 * 10;
        strBuff.append("http").append(SEP_HASHTAG).append(topicName).append(SEP_HASHTAG)
                .append(streamId).append(SEP_HASHTAG).append(strRemoteIP).append(SEP_HASHTAG)
                .append(NetworkUtils.getLocalIp()).append(SEP_HASHTAG)
                .append(evenProcType.getRight()).append(SEP_HASHTAG)
                .append(DateTimeUtils.ms2yyyyMMddHHmm(longDataTime)).append(SEP_HASHTAG)
                .append(DateTimeUtils.ms2yyyyMMddHHmm(msgRcvTime));
        long beginTime = System.currentTimeMillis();
        try {
            processor.processEvent(event);
            if (monitorIndex != null) {
                monitorIndex.addAndGet(strBuff.toString(),
                        intMsgCnt, 1, data.length, 0);
                monitorIndexExt.incrementAndGet("EVENT_SUCCESS");
            }
            addStatistics(true, data.length, event);
        } catch (ChannelException ex) {
            if (monitorIndex != null) {
                monitorIndex.addAndGet(strBuff.toString(),
                        0, 0, 0, intMsgCnt);
                monitorIndexExt.incrementAndGet("EVENT_DROPPED");
            }
            addStatistics(false, data.length, event);
            logCounter++;
            if (logCounter == 1 || logCounter % 1000 == 0) {
                LOG.error("Error writing to channel, and will retry after 1s, ex={},"
                        + "logCounter={}, spend time={} ms", ex, logCounter, System.currentTimeMillis() - beginTime);
                if (logCounter > Long.MAX_VALUE - 10) {
                    logCounter = 0;
                    LOG.info("logCounter will reverse");
                }
            }
            throw ex;
        }
        channelTrace++;
        if (channelTrace % 600000 == 0) {
            LOG.info("processor.processEvent spend time={} ms", System.currentTimeMillis() - beginTime);
        }
        if (channelTrace > Long.MAX_VALUE - 10) {
            channelTrace = 0;
            LOG.info("channelTrace will reverse");
        }
    }

    @Override
    public void configure(org.apache.flume.Context context) {
    }

    private String getTopic(String groupId, String streamId) {
        String topic = null;
        if (StringUtils.isNotEmpty(groupId)) {
            if (StringUtils.isNotEmpty(streamId)) {
                topic = configManager.getTopicProperties().get(groupId + "/" + streamId);
            }
            if (StringUtils.isEmpty(topic)) {
                topic = configManager.getTopicProperties().get(groupId);
            }
        }
        LOG.debug("Get topic by groupId/streamId = {}, topic = {}", groupId + "/" + streamId, topic);
        return topic;
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
        metricItemSet.fillSrcMetricItemsByEvent(event, isSuccess, size);
        if (isSuccess) {
            AuditUtils.add(AuditUtils.AUDIT_ID_DATAPROXY_READ_SUCCESS, event);
        }
    }
}
