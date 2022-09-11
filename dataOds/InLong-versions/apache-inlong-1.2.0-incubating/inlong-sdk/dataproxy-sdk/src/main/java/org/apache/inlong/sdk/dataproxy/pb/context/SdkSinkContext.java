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

package org.apache.inlong.sdk.dataproxy.pb.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.INLONG_COMPRESSED_TYPE;
import org.apache.inlong.sdk.dataproxy.pb.config.ProxyClusterConfigHolder;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyClusterConfig;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyClusterResult;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyNodeInfo;
import org.apache.inlong.sdk.dataproxy.pb.dispatch.DispatchManager;
import org.apache.inlong.sdk.dataproxy.pb.dispatch.DispatchProfile;
import org.apache.inlong.sdk.dataproxy.pb.metrics.SdkMetricItem;
import org.apache.inlong.sdk.dataproxy.pb.network.IpPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * SdkSinkContext
 */
public class SdkSinkContext extends SinkContext {

    public static final Logger LOG = LoggerFactory.getLogger(SdkSinkContext.class);
    public static final String KEY_SDK_PACK_TIMEOUT = "sdkPackTimeout";
    public static final String KEY_COMPRESSED_TYPE = "compressedType";
    public static final int DEFAULT_COMPRESSED_TYPE = INLONG_COMPRESSED_TYPE.INLONG_SNAPPY.getNumber();
    public static final int MAX_RESPONSE_LENGTH = 32 * 1024;
    public static final short PACK_VERSION = 0x0001;
    public static final int PACK_VERSION_LENGTH = 2;

    private Map<String, ProxyClusterResult> proxyClusterMap;
    // Map<proxyClusterId, List<IpPort>>
    private Map<String, Set<IpPort>> proxyIpListMap = new ConcurrentHashMap<>();
    private final long sdkPackTimeout;
    private final long maxPackCount;
    private final long maxPackSize;
    private final INLONG_COMPRESSED_TYPE compressedType;

    /**
     * Constructor
     *
     * @param context
     * @param channel
     */
    public SdkSinkContext(Context context, Channel channel) {
        super(context, channel);
        this.sdkPackTimeout = context.getLong(KEY_SDK_PACK_TIMEOUT, 60000L);
        this.maxPackCount = context.getLong(DispatchManager.KEY_DISPATCH_MAX_PACKCOUNT,
                DispatchManager.DEFAULT_DISPATCH_MAX_PACKCOUNT);
        this.maxPackSize = context.getLong(DispatchManager.KEY_DISPATCH_MAX_PACKSIZE,
                DispatchManager.DEFAULT_DISPATCH_MAX_PACKSIZE);
        this.compressedType = INLONG_COMPRESSED_TYPE
                .valueOf(context.getInteger(KEY_COMPRESSED_TYPE, DEFAULT_COMPRESSED_TYPE));
        ProxyClusterConfigHolder.start(context);
    }

    /**
     * reload
     */
    public void reload() {
        try {
            Map<String, ProxyClusterResult> newProxyClusterMap = ProxyClusterConfigHolder.getProxyClusterMap();
            if (this.proxyClusterMap != null && this.proxyClusterMap.equals(newProxyClusterMap)) {
                return;
            }
            // proxyIpListMap
            Map<String, Set<IpPort>> newProxyIpListMap = new ConcurrentHashMap<>();
            for (Entry<String, ProxyClusterResult> entry : newProxyClusterMap.entrySet()) {
                ProxyClusterConfig config = entry.getValue().getConfig();
                String proxyClusterId = config.getClusterId();
                Set<IpPort> ipPortSet = new HashSet<>();
                for (ProxyNodeInfo nodeInfo : config.getNodeList()) {
                    IpPort ipPort = new IpPort(nodeInfo.getNodeIp(), nodeInfo.getNodePort());
                    ipPortSet.add(ipPort);
                }
                newProxyIpListMap.put(proxyClusterId, ipPortSet);
            }
            // replace
            this.proxyClusterMap = newProxyClusterMap;
            this.proxyIpListMap = newProxyIpListMap;
            super.reload();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
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
        dimensions.put(SdkMetricItem.KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(SdkMetricItem.KEY_INLONG_STREAM_ID, inlongStreamId);
    }

    /**
     * addSendResultMetric
     * 
     * @param currentRecord
     * @param proxyClusterId
     * @param result
     * @param sendTime
     */
    public void addSendResultMetric(DispatchProfile currentRecord, String proxyClusterId, boolean result,
            long sendTime) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SdkMetricItem.KEY_NODE_ID, this.nodeId);
        dimensions.put(SdkMetricItem.KEY_NODE_IP, this.nodeIp);
        // metric
        fillInlongId(currentRecord, dimensions);
        dimensions.put(SdkMetricItem.KEY_SINK_ID, proxyClusterId);
        long msgTime = currentRecord.getDispatchTime();
        long auditFormatTime = msgTime - msgTime % auditFormatInterval;
        dimensions.put(SdkMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        SdkMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        long count = currentRecord.getCount();
        long size = currentRecord.getSize();
        if (result) {
            metricItem.sendSuccessCount.addAndGet(count);
            metricItem.sendSuccessSize.addAndGet(size);
            if (sendTime > 0) {
                long currentTime = System.currentTimeMillis();
                long sinkDuration = currentTime - sendTime;
                long nodeDuration = currentTime - NumberUtils.toLong(Constants.HEADER_KEY_SOURCE_TIME, msgTime);
                long wholeDuration = currentTime - msgTime;
                metricItem.sinkDuration.addAndGet(sinkDuration * count);
                metricItem.nodeDuration.addAndGet(nodeDuration * count);
                metricItem.wholeDuration.addAndGet(wholeDuration * count);
            }
//            LOG.info("addSendTrueMetric,bid:{},result:{},sendTime:{},count:{},metric:{}",
//                    bid, result, sendTime, currentRecord.getCount(), JSON.toJSONString(metricItemSet.getItemMap()));
        } else {
            metricItem.sendFailCount.addAndGet(count);
            metricItem.sendFailSize.addAndGet(size);
//            LOG.info("addSendFalseMetric,bid:{},result:{},sendTime:{},count:{},metric:{}",
//                    bid, result, sendTime, currentRecord.getCount(), JSON.toJSONString(metricItemSet.getItemMap()));
        }
    }

    /**
     * addSendMetric
     * 
     * @param currentRecord
     * @param proxyClusterId
     */
    public void addSendMetric(DispatchProfile currentRecord, String proxyClusterId) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SdkMetricItem.KEY_NODE_ID, this.nodeId);
        dimensions.put(SdkMetricItem.KEY_NODE_IP, this.nodeIp);
        // metric
        fillInlongId(currentRecord, dimensions);
        dimensions.put(SdkMetricItem.KEY_SINK_ID, proxyClusterId);
        long msgTime = currentRecord.getDispatchTime();
        long auditFormatTime = msgTime - msgTime % auditFormatInterval;
        dimensions.put(SdkMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        SdkMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        long count = currentRecord.getCount();
        long size = currentRecord.getSize();
        metricItem.sendCount.addAndGet(count);
        metricItem.sendSize.addAndGet(size);
//        LOG.info("addSendMetric,bid:{},count:{},metric:{}",
//                bid, currentRecord.getCount(), JSON.toJSONString(metricItemSet.getItemMap()));
    }

    /**
     * addReadFailMetric
     */
    public void addSendFailMetric() {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SdkMetricItem.KEY_NODE_ID, this.nodeId);
        dimensions.put(SdkMetricItem.KEY_NODE_IP, this.nodeIp);
        long msgTime = System.currentTimeMillis();
        long auditFormatTime = msgTime - msgTime % auditFormatInterval;
        dimensions.put(SdkMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        SdkMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        metricItem.readFailCount.incrementAndGet();
    }

    /**
     * get bidIpListMap
     * 
     * @return the proxyIpListMap
     */
    public Map<String, Set<IpPort>> getProxyIpListMap() {
        return proxyIpListMap;
    }

    /**
     * get sdkPackTimeout
     * 
     * @return the sdkPackTimeout
     */
    public long getSdkPackTimeout() {
        return sdkPackTimeout;
    }

    /**
     * get maxPackCount
     * 
     * @return the maxPackCount
     */
    public long getMaxPackCount() {
        return maxPackCount;
    }

    /**
     * get maxPackSize
     * 
     * @return the maxPackSize
     */
    public long getMaxPackSize() {
        return maxPackSize;
    }

    /**
     * getProxyClusterId
     * 
     * @param  uid
     * @return
     */
    public String getProxyClusterId(String uid) {
        ProxyClusterResult result = ProxyClusterConfigHolder.getInlongStreamMap().get(uid);
        if (result != null) {
            return result.getClusterId();
        }
        return null;
    }

    /**
     * get compressedType
     * 
     * @return the compressedType
     */
    public INLONG_COMPRESSED_TYPE getCompressedType() {
        return compressedType;
    }

}
