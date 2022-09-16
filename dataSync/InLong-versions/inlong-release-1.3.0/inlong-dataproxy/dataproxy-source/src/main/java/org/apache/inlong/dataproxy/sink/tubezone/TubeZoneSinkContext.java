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

package org.apache.inlong.dataproxy.sink.tubezone;

import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.inlong.dataproxy.config.holder.CacheClusterConfigHolder;
import org.apache.inlong.dataproxy.config.holder.CommonPropertiesHolder;
import org.apache.inlong.dataproxy.config.holder.IdTopicConfigHolder;
import org.apache.inlong.dataproxy.dispatch.DispatchProfile;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItem;
import org.apache.inlong.dataproxy.metrics.audit.AuditUtils;
import org.apache.inlong.dataproxy.sink.SinkContext;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.INLONG_COMPRESSED_TYPE;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * TubeZoneSinkContext
 */
public class TubeZoneSinkContext extends SinkContext {

    public static final String KEY_NODE_ID = "nodeId";
    public static final String PREFIX_PRODUCER = "producer.";
    public static final String KEY_COMPRESS_TYPE = "compressType";

    private final LinkedBlockingQueue<DispatchProfile> dispatchQueue;

    private final String nodeId;
    private final Context producerContext;
    //
    private final IdTopicConfigHolder idTopicHolder;
    private final CacheClusterConfigHolder cacheHolder;
    private final INLONG_COMPRESSED_TYPE compressType;

    /**
     * Constructor
     * 
     * @param context
     */
    public TubeZoneSinkContext(String sinkName, Context context, Channel channel,
            LinkedBlockingQueue<DispatchProfile> dispatchQueue) {
        super(sinkName, context, channel);
        this.dispatchQueue = dispatchQueue;
        // nodeId
        this.nodeId = CommonPropertiesHolder.getString(KEY_NODE_ID, "127.0.0.1");
        // compressionType
        String strCompressionType = CommonPropertiesHolder.getString(KEY_COMPRESS_TYPE,
                INLONG_COMPRESSED_TYPE.INLONG_SNAPPY.name());
        this.compressType = INLONG_COMPRESSED_TYPE.valueOf(strCompressionType);
        // producerContext
        Map<String, String> producerParams = context.getSubProperties(PREFIX_PRODUCER);
        this.producerContext = new Context(producerParams);
        // idTopicHolder
        Context commonPropertiesContext = new Context(CommonPropertiesHolder.get());
        this.idTopicHolder = new IdTopicConfigHolder();
        this.idTopicHolder.configure(commonPropertiesContext);
        // cacheHolder
        this.cacheHolder = new CacheClusterConfigHolder();
        this.cacheHolder.configure(commonPropertiesContext);
    }

    /**
     * start
     */
    public void start() {
        super.start();
        this.idTopicHolder.start();
        this.cacheHolder.start();
    }

    /**
     * close
     */
    public void close() {
        super.close();
        this.idTopicHolder.close();
        this.cacheHolder.close();
    }

    /**
     * get dispatchQueue
     * 
     * @return the dispatchQueue
     */
    public LinkedBlockingQueue<DispatchProfile> getDispatchQueue() {
        return dispatchQueue;
    }

    /**
     * get producerContext
     * 
     * @return the producerContext
     */
    public Context getProducerContext() {
        return producerContext;
    }

    /**
     * get idTopicHolder
     * 
     * @return the idTopicHolder
     */
    public IdTopicConfigHolder getIdTopicHolder() {
        return idTopicHolder;
    }

    /**
     * get cacheHolder
     * 
     * @return the cacheHolder
     */
    public CacheClusterConfigHolder getCacheHolder() {
        return cacheHolder;
    }

    /**
     * get compressType
     * 
     * @return the compressType
     */
    public INLONG_COMPRESSED_TYPE getCompressType() {
        return compressType;
    }

    /**
     * get nodeId
     * 
     * @return the nodeId
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * addSendMetric
     * 
     * @param currentRecord
     * @param bid
     */
    public void addSendMetric(DispatchProfile currentRecord, String bid) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, this.getProxyClusterId());
        // metric
        fillInlongId(currentRecord, dimensions);
        dimensions.put(DataProxyMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(DataProxyMetricItem.KEY_SINK_DATA_ID, bid);
        long msgTime = currentRecord.getDispatchTime();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(DataProxyMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        DataProxyMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        long count = currentRecord.getCount();
        long size = currentRecord.getSize();
        metricItem.sendCount.addAndGet(count);
        metricItem.sendSize.addAndGet(size);
    }

    /**
     * addReadFailMetric
     */
    public void addSendFailMetric() {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, this.getProxyClusterId());
        dimensions.put(DataProxyMetricItem.KEY_SINK_ID, this.getSinkName());
        long msgTime = System.currentTimeMillis();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(DataProxyMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        DataProxyMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        metricItem.readFailCount.incrementAndGet();
    }

    /**
     * fillInlongId
     * 
     * @param currentRecord
     * @param dimensions
     */
    public static void fillInlongId(DispatchProfile currentRecord, Map<String, String> dimensions) {
        String inlongGroupId = currentRecord.getInlongGroupId();
        inlongGroupId = (StringUtils.isBlank(inlongGroupId)) ? "-" : inlongGroupId;
        String inlongStreamId = currentRecord.getInlongStreamId();
        inlongStreamId = (StringUtils.isBlank(inlongStreamId)) ? "-" : inlongStreamId;
        dimensions.put(DataProxyMetricItem.KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(DataProxyMetricItem.KEY_INLONG_STREAM_ID, inlongStreamId);
    }

    /**
     * processSendFail
     * @param currentRecord
     * @param producerTopic
     * @param sendTime
     */
    public void processSendFail(DispatchProfile currentRecord, String producerTopic, long sendTime) {
        if (currentRecord.isResend()) {
            dispatchQueue.offer(currentRecord);
            this.addSendResultMetric(currentRecord, producerTopic, false, sendTime);
        } else {
            currentRecord.fail();
        }
    }

    /**
     * addSendResultMetric
     * 
     * @param currentRecord
     * @param bid
     * @param result
     * @param sendTime
     */
    public void addSendResultMetric(DispatchProfile currentRecord, String bid, boolean result, long sendTime) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, this.getProxyClusterId());
        // metric
        fillInlongId(currentRecord, dimensions);
        dimensions.put(DataProxyMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(DataProxyMetricItem.KEY_SINK_DATA_ID, bid);
        long dispatchTime = currentRecord.getDispatchTime();
        long auditFormatTime = dispatchTime - dispatchTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(DataProxyMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        DataProxyMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        long count = currentRecord.getCount();
        long size = currentRecord.getSize();
        if (result) {
            metricItem.sendSuccessCount.addAndGet(count);
            metricItem.sendSuccessSize.addAndGet(size);
            currentRecord.getEvents().forEach((event) -> {
                AuditUtils.add(AuditUtils.AUDIT_ID_DATAPROXY_READ_SUCCESS, event);
            });
            if (sendTime > 0) {
                long currentTime = System.currentTimeMillis();
                currentRecord.getEvents().forEach((event) -> {
                    long sinkDuration = currentTime - sendTime;
                    long nodeDuration = currentTime - event.getSourceTime();
                    long wholeDuration = currentTime - event.getMsgTime();
                    metricItem.sinkDuration.addAndGet(sinkDuration);
                    metricItem.nodeDuration.addAndGet(nodeDuration);
                    metricItem.wholeDuration.addAndGet(wholeDuration);
                });
            }
        } else {
            metricItem.sendFailCount.addAndGet(count);
            metricItem.sendFailSize.addAndGet(size);
        }
    }
}
