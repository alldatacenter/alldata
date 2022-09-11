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

import org.apache.commons.lang.StringUtils;
import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.event.EventBuilder;
import org.apache.inlong.common.monitor.CounterGroup;
import org.apache.inlong.common.monitor.CounterGroupExt;
import org.apache.inlong.common.monitor.MonitorIndex;
import org.apache.inlong.common.monitor.MonitorIndexExt;
import org.apache.inlong.common.monitor.StatConstants;
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.common.util.NetworkUtils;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.consts.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.http.exception.MessageProcessException;
import org.apache.inlong.dataproxy.source.ServiceDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class SimpleMessageHandler implements MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleMessageHandler.class);
    private static final ConfigManager configManager = ConfigManager.getInstance();
    private static final String SEPARATOR = "#";
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMATTER =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyyMMddHHmm");
                }
            };
    private static final String DEFAULT_REMOTE_IDC_VALUE = "0";
    private final CounterGroup counterGroup;
    private final CounterGroupExt counterGroupExt;
    private final MonitorIndex monitorIndex = new MonitorIndex("Source", 60, 300000);
    private final MonitorIndexExt monitorIndexExt =
            new MonitorIndexExt("DataProxy_monitors#http", 60, 100000);
    private final ChannelProcessor processor;

    private final boolean isNewMetricOn = true;

    @SuppressWarnings("unused")
    private int maxMsgLength;
    private long logCounter = 0L;
    private long channelTrace = 0L;

    public SimpleMessageHandler(ChannelProcessor processor, CounterGroup counterGroup,
            CounterGroupExt counterGroupExt, ServiceDecoder decoder) {
        this.processor = processor;
        this.counterGroup = counterGroup;
        this.counterGroupExt = counterGroupExt;
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
        String topicValue = "test";
        String attr = "m=0";
        StringBuilder newAttrBuffer = new StringBuilder(attr);

        String groupId = (String) context.get(AttributeConstants.GROUP_ID);
        String streamId = (String) context.get(AttributeConstants.STREAM_ID);
        String dt = (String) context.get(AttributeConstants.DATA_TIME);

        String value = getTopic(groupId, streamId);
        if (null != value && !"".equals(value)) {
            topicValue = value.trim();
        }

        String mxValue = configManager.getMxProperties().get(groupId);
        if (null != mxValue) {
            newAttrBuffer = new StringBuilder(mxValue.trim());
        }

        newAttrBuffer.append("&groupId=").append(groupId).append("&streamId=").append(streamId)
                .append("&dt=").append(dt);
        HttpServletRequest request =
                (HttpServletRequest) context.get(AttributeConstants.HTTP_REQUEST);
        String strRemoteIP = request.getRemoteAddr();
        newAttrBuffer.append("&NodeIP=").append(strRemoteIP);
        String msgCount = request.getParameter(AttributeConstants.MESSAGE_COUNT);
        if (msgCount == null || "".equals(msgCount)) {
            msgCount = "1";
        }

        InLongMsg inLongMsg = InLongMsg.newInLongMsg(true);
        String charset = (String) context.get(AttributeConstants.CHARSET);
        if (charset == null || "".equals(charset)) {
            charset = "UTF-8";
        }
        String body = (String) context.get(AttributeConstants.BODY);
        try {
            inLongMsg.addMsg(newAttrBuffer.toString(), body.getBytes(charset));
        } catch (UnsupportedEncodingException e) {
            throw new MessageProcessException(e);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(AttributeConstants.DATA_TIME, dt);
        headers.put(ConfigConstants.TOPIC_KEY, topicValue);
        headers.put(AttributeConstants.STREAM_ID, streamId);
        headers.put(ConfigConstants.REMOTE_IP_KEY, strRemoteIP);
        headers.put(ConfigConstants.REMOTE_IDC_KEY, DEFAULT_REMOTE_IDC_VALUE);
        headers.put(ConfigConstants.MSG_COUNTER_KEY, msgCount);
        byte[] data = inLongMsg.buildArray();
        headers.put(ConfigConstants.TOTAL_LEN, String.valueOf(data.length));
        String pkgTime = DATE_FORMATTER.get().format(inLongMsg.getCreatetime());
        headers.put(ConfigConstants.PKG_TIME_KEY, pkgTime);
        Event event = EventBuilder.withBody(data, headers);

        String counterExtKey = topicValue + "#" + 0 + "#" + strRemoteIP + "#time#" + pkgTime;
        counterGroupExt.addAndGet(counterExtKey, Long.valueOf(msgCount));

        long dtten;
        try {
            dtten = Long.parseLong(dt);
        } catch (NumberFormatException e1) {
            throw new MessageProcessException(new Throwable(
                    "attribute dt=" + dt + " has error," + " detail is: " + newAttrBuffer));
        }

        dtten = dtten / 1000 / 60 / 10;
        dtten = dtten * 1000 * 60 * 10;

        StringBuilder newBase = new StringBuilder();
        newBase.append("http").append(SEPARATOR).append(topicValue).append(SEPARATOR)
                .append(streamId).append(SEPARATOR).append(strRemoteIP).append(SEPARATOR)
                .append(NetworkUtils.getLocalIp()).append(SEPARATOR)
                .append(new SimpleDateFormat("yyyyMMddHHmm").format(dtten)).append(SEPARATOR)
                .append(pkgTime);

        if (isNewMetricOn) {
            monitorIndex.addAndGet(new String(newBase), Integer.parseInt(msgCount), 1, data.length, 0);
        }
        inLongMsg.reset();

        long beginTime = System.currentTimeMillis();
        try {
            processor.processEvent(event);
            counterGroup.incrementAndGet(StatConstants.EVENT_SUCCESS);
            monitorIndexExt.incrementAndGet("EVENT_SUCCESS");

        } catch (ChannelException ex) {
            counterGroup.incrementAndGet(StatConstants.EVENT_DROPPED);
            monitorIndexExt.incrementAndGet("EVENT_DROPPED");
            if (isNewMetricOn) {
                monitorIndex.addAndGet(new String(newBase), 0, 0, 0, Integer.parseInt(msgCount));
            }

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

}
