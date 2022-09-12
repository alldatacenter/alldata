/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.pb.metrics;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.flume.Event;
import org.apache.inlong.common.metric.CountMetric;
import org.apache.inlong.common.metric.Dimension;
import org.apache.inlong.common.metric.MetricDomain;
import org.apache.inlong.common.metric.MetricItem;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.sdk.dataproxy.pb.context.Constants;

/**
 *
 * SdkMetricItem
 */
@MetricDomain(name = "ProxySdk")
public class SdkMetricItem extends MetricItem {

    public static final String KEY_NODE_ID = "nodeId";// nodeId
    public static final String KEY_NODE_IP = "nodeIp";// nodeIp
    public static final String KEY_INLONG_GROUP_ID = "inlongGroupId";
    public static final String KEY_INLONG_STREAM_ID = "inlongStreamId";
    public static final String KEY_SINK_ID = "sinkId";// proxyClusterId
    public static final String KEY_MESSAGE_TIME = "msgTime";
    //
    public static final String M_READ_SUCCESS_COUNT = "readSuccessCount";
    public static final String M_READ_SUCCESS_SIZE = "readSuccessSize";
    public static final String M_READ_FAIL_COUNT = "readFailCount";
    public static final String M_READ_FAIL_SIZE = "readFailSize";
    public static final String M_SEND_COUNT = "sendCount";
    public static final String M_SEND_SIZE = "sendSize";
    public static final String M_SEND_SUCCESS_COUNT = "sendSuccessCount";
    public static final String M_SEND_SUCCESS_SIZE = "sendSuccessSize";
    public static final String M_SEND_FAIL_COUNT = "sendFailCount";
    public static final String M_SEND_FAIL_SIZE = "sendFailSize";
    //
    public static final String M_SINK_DURATION = "sinkDuration";
    public static final String M_NODE_DURATION = "nodeDuration";
    public static final String M_WHOLE_DURATION = "wholeDuration";

    @Dimension
    public String nodeId;
    @Dimension
    public String nodeIp;
    @Dimension
    public String inlongGroupId;
    @Dimension
    public String inlongStreamId;
    @Dimension
    public String sinkId;
    @Dimension
    public String msgTime = String.valueOf(0);
    @CountMetric
    public AtomicLong readSuccessCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong readSuccessSize = new AtomicLong(0);
    @CountMetric
    public AtomicLong readFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong readFailSize = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendSize = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendSuccessCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendSuccessSize = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendFailSize = new AtomicLong(0);
    @CountMetric
    // sinkCallbackTime - sinkBeginTime(milliseconds)
    public AtomicLong sinkDuration = new AtomicLong(0);
    @CountMetric
    // sinkCallbackTime - sourceReceiveTime(milliseconds)
    public AtomicLong nodeDuration = new AtomicLong(0);
    @CountMetric
    // sinkCallbackTime - eventCreateTime(milliseconds)
    public AtomicLong wholeDuration = new AtomicLong(0);

    /**
     * fillInlongId
     *
     * @param event
     * @param dimensions
     */
    public static void fillInlongId(Event event, Map<String, String> dimensions) {
        Map<String, String> headers = event.getHeaders();
        String inlongGroupId = getInlongGroupId(headers);
        String inlongStreamId = getInlongStreamId(headers);
        dimensions.put(KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(KEY_INLONG_STREAM_ID, inlongStreamId);
    }

    /**
     * getInlongGroupId
     *
     * @param  headers
     * @return
     */
    public static String getInlongGroupId(Map<String, String> headers) {
        String inlongGroupId = headers.get(Constants.INLONG_GROUP_ID);
        if (inlongGroupId == null) {
            inlongGroupId = headers.getOrDefault(Constants.TOPIC, "");
        }
        return inlongGroupId;
    }

    /**
     * getInlongStreamId
     *
     * @param  headers
     * @return
     */
    public static String getInlongStreamId(Map<String, String> headers) {
        String inlongStreamId = headers.get(Constants.INLONG_STREAM_ID);
        if (inlongStreamId == null) {
            inlongStreamId = headers.getOrDefault(AttributeConstants.INTERFACE_ID, "");
        }
        return inlongStreamId;
    }

    /**
     * getLogTime
     *
     * @param  headers
     * @return
     */
    public static long getLogTime(Map<String, String> headers) {
        String strLogTime = headers.get(Constants.HEADER_KEY_MSG_TIME);
        if (strLogTime == null) {
            strLogTime = headers.get(AttributeConstants.DATA_TIME);
        }
        if (strLogTime == null) {
            return System.currentTimeMillis();
        }
        long logTime = NumberUtils.toLong(strLogTime, 0);
        if (logTime == 0) {
            logTime = System.currentTimeMillis();
        }
        return logTime;
    }

    /**
     * getLogTime
     *
     * @param  event
     * @return
     */
    public static long getLogTime(Event event) {
        if (event != null) {
            Map<String, String> headers = event.getHeaders();
            return getLogTime(headers);
        }
        return System.currentTimeMillis();
    }

    /**
     * reportDurations
     * 
     * @param currentRecord
     * @param result
     * @param sendTime
     * @param dimensions
     * @param msgTime
     * @param metricItemSet
     */
    public static void reportDurations(
            Event currentRecord, boolean result, long sendTime,
            Map<String, String> dimensions, long msgTime, SdkMetricItemSet metricItemSet) {
        SdkMetricItem metricItem = metricItemSet.findMetricItem(dimensions);
        if (result) {
            metricItem.sendSuccessCount.incrementAndGet();
            metricItem.sendSuccessSize.addAndGet(currentRecord.getBody().length);
            if (sendTime > 0) {
                long currentTime = System.currentTimeMillis();
                long sinkDuration = currentTime - sendTime;
                long nodeDuration = currentTime - NumberUtils.toLong(Constants.HEADER_KEY_SOURCE_TIME, msgTime);
                long wholeDuration = currentTime - msgTime;
                metricItem.sinkDuration.addAndGet(sinkDuration);
                metricItem.nodeDuration.addAndGet(nodeDuration);
                metricItem.wholeDuration.addAndGet(wholeDuration);
            }
        } else {
            metricItem.sendFailCount.incrementAndGet();
            metricItem.sendFailSize.addAndGet(currentRecord.getBody().length);
        }
    }
}
