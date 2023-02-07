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

package org.apache.inlong.dataproxy.metrics;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flume.Event;
import org.apache.inlong.common.metric.MetricDomain;
import org.apache.inlong.common.metric.MetricItemSet;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.dataproxy.config.holder.CommonPropertiesHolder;
import org.apache.inlong.dataproxy.consts.ConfigConstants;

/**
 * 
 * MetaSinkMetricItemSet
 */
@MetricDomain(name = "DataProxy")
public class DataProxyMetricItemSet extends MetricItemSet<DataProxyMetricItem> {

    private String clusterId = null;
    private String sourceDataId = null;

    /**
     * Constructor
     * 
     * @param name
     */
    public DataProxyMetricItemSet(String name) {
        super(name);
    }

    /**
     * Constructor
     *
     * @param clusterId  the cluster id
     * @param name    the module name
     */
    public DataProxyMetricItemSet(String clusterId, String name) {
        super(name);
        this.clusterId = clusterId;
    }

    /**
     * Constructor
     *
     * @param clusterId  the cluster id
     * @param name       the module name
     * @param sourceDataId   the source data id
     */
    public DataProxyMetricItemSet(String clusterId, String name, String sourceDataId) {
        super(name);
        this.clusterId = clusterId;
        this.sourceDataId = sourceDataId;
    }

    /**
     * Fill source metric items by event
     *
     * @param event    the event object
     * @param isSuccess  whether success read
     * @param size       the message size
     */
    public void fillSrcMetricItemsByEvent(Event event, boolean isSuccess, long size) {
        fillMetricItemsByEvent(event, true, true, isSuccess, size, 0);
    }

    /**
     * Fill sink send metric items by event
     *
     * @param event    the event object
     * @param sentTime   the sent time
     * @param isSuccess  whether success read or send
     * @param size       the message size
     */
    public void fillSinkSendMetricItemsByEvent(Event event, long sentTime,
            boolean isSuccess, long size) {
        fillMetricItemsByEvent(event, false, false, isSuccess, size, sentTime);
    }

    /**
     * Fill metric items by event
     *
     * @param event    the event object
     * @param isSource   whether source part
     * @param isReadOp   whether read operation
     * @param isSuccess  whether success read or send
     * @param size       the message size
     */
    private void fillMetricItemsByEvent(Event event, boolean isSource,
            boolean isReadOp, boolean isSuccess,
            long size, long sendTime) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, clusterId);
        dimensions.put(DataProxyMetricItem.KEY_INLONG_GROUP_ID,
                event.getHeaders().get(AttributeConstants.GROUP_ID));
        dimensions.put(DataProxyMetricItem.KEY_INLONG_STREAM_ID,
                event.getHeaders().get(AttributeConstants.STREAM_ID));
        long dataTime = NumberUtils.toLong(
                event.getHeaders().get(AttributeConstants.DATA_TIME));
        long msgCount = NumberUtils.toLong(
                event.getHeaders().get(ConfigConstants.MSG_COUNTER_KEY));
        long auditFormatTime = dataTime - dataTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(DataProxyMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        if (isSource) {
            dimensions.put(DataProxyMetricItem.KEY_SOURCE_ID, name);
            dimensions.put(DataProxyMetricItem.KEY_SOURCE_DATA_ID, sourceDataId);
        } else {
            dimensions.put(DataProxyMetricItem.KEY_SINK_ID, name);
            dimensions.put(DataProxyMetricItem.KEY_SINK_DATA_ID,
                    event.getHeaders().get(ConfigConstants.TOPIC_KEY));
        }
        DataProxyMetricItem metricItem = findMetricItem(dimensions);
        if (isReadOp) {
            if (isSuccess) {
                metricItem.readSuccessCount.addAndGet(msgCount);
                metricItem.readSuccessSize.addAndGet(size);
            } else {
                metricItem.readFailCount.addAndGet(msgCount);
                metricItem.readFailSize.addAndGet(size);
            }
        } else {
            if (isSuccess) {
                metricItem.sendSuccessCount.addAndGet(msgCount);
                metricItem.sendSuccessSize.addAndGet(size);
                if (sendTime > 0) {
                    long currentTime = System.currentTimeMillis();
                    long msgDataTimeL = Long.parseLong(
                            event.getHeaders().get(AttributeConstants.DATA_TIME));
                    long msgRcvTimeL = Long.parseLong(
                            event.getHeaders().get(AttributeConstants.RCV_TIME));
                    metricItem.sinkDuration.addAndGet(currentTime - sendTime);
                    metricItem.nodeDuration.addAndGet(currentTime - msgRcvTimeL);
                    metricItem.wholeDuration.addAndGet(currentTime - msgDataTimeL);
                }
            } else {
                metricItem.sendFailCount.addAndGet(msgCount);
                metricItem.sendFailSize.addAndGet(size);
            }
            metricItem.sendCount.addAndGet(msgCount);
            metricItem.sendSize.addAndGet(size);
        }
    }

    /**
     * createItem
     * 
     * @return
     */
    @Override
    protected DataProxyMetricItem createItem() {
        return new DataProxyMetricItem();
    }

}
