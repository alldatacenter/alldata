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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.common.metric.MetricUtils;
import org.apache.inlong.common.metric.MetricValue;
import org.apache.inlong.dataproxy.utils.MockUtils;
import org.apache.inlong.common.metric.MetricItemValue;
import org.apache.inlong.common.metric.MetricListener;
import org.apache.inlong.common.metric.MetricListenerRunnable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * TestMetricItemSetMBean
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({MetricRegister.class})
public class TestMetricListenerRunnable {

    public static final Logger LOG = LoggerFactory.getLogger(TestMetricListenerRunnable.class);

    public static final String CLUSTER_ID = "inlong5th_sz";
    public static final String CONTAINER_NAME = "2222.inlong.DataProxy.sz100001";
    public static final String CONTAINER_IP = "127.0.0.1";
    private static final String SOURCE_ID = "agent-source";
    private static final String SOURCE_DATA_ID = "12069";
    private static final String INLONG_GROUP_ID1 = "03a00000026";
    private static final String INLONG_GROUP_ID2 = "03a00000126";
    private static final String INLONG_STREAM_ID = "";
    private static final String SINK_ID = "inlong5th-pulsar-sz";
    private static final String SINK_DATA_ID = "PULSAR_TOPIC_1";
    private static DataProxyMetricItemSet itemSet;
    private static Map<String, String> dimSource;
    private static Map<String, String> dimSink;
    private static String keySource1;
    private static String keySource2;
    private static String keySink1;
    private static String keySink2;

    /**
     * testResult
     * 
     * @throws Exception
     */
    @Test
    public void testResult() throws Exception {
        MockUtils.mockMetricRegister();
        itemSet = new DataProxyMetricItemSet(CLUSTER_ID);
        MetricRegister.register(itemSet);
        // prepare
        DataProxyMetricItem itemSource = new DataProxyMetricItem();
        itemSource.clusterId = CLUSTER_ID;
        itemSource.sourceId = SOURCE_ID;
        itemSource.sourceDataId = SOURCE_DATA_ID;
        itemSource.inlongGroupId = INLONG_GROUP_ID1;
        itemSource.inlongStreamId = INLONG_STREAM_ID;
        dimSource = itemSource.getDimensions();
        //
        DataProxyMetricItem itemSink = new DataProxyMetricItem();
        itemSink.clusterId = CLUSTER_ID;
        itemSink.sinkId = SINK_ID;
        itemSink.sinkDataId = SINK_DATA_ID;
        itemSink.inlongGroupId = INLONG_GROUP_ID1;
        itemSink.inlongStreamId = INLONG_STREAM_ID;
        dimSink = itemSink.getDimensions();
        // increase source
        DataProxyMetricItem item = null;
        item = itemSet.findMetricItem(dimSource);
        item.readSuccessCount.incrementAndGet();
        item.readSuccessSize.addAndGet(100);
        keySource1 = MetricUtils.getDimensionsKey(dimSource);
        //
        dimSource.put("inlongGroupId", INLONG_GROUP_ID2);
        item = itemSet.findMetricItem(dimSource);
        item.readFailCount.addAndGet(20);
        item.readFailSize.addAndGet(2000);
        keySource2 = MetricUtils.getDimensionsKey(dimSource);
        // increase sink
        item = itemSet.findMetricItem(dimSink);
        item.sendCount.incrementAndGet();
        item.sendSize.addAndGet(100);
        item.sendSuccessCount.incrementAndGet();
        item.sendSuccessSize.addAndGet(100);
        keySink1 = MetricUtils.getDimensionsKey(dimSink);
        //
        dimSink.put("inlongGroupId", INLONG_GROUP_ID2);
        item = itemSet.findMetricItem(dimSink);
        item.sendCount.addAndGet(20);
        item.sendSize.addAndGet(2000);
        item.sendFailCount.addAndGet(20);
        item.sendFailSize.addAndGet(2000);
        keySink2 = MetricUtils.getDimensionsKey(dimSink);
        // report
        MetricListener listener = new MetricListener() {

            @Override
            public void snapshot(String domain, List<MetricItemValue> itemValues) {
                assertEquals("DataProxy", domain);
                for (MetricItemValue itemValue : itemValues) {
                    String key = itemValue.getKey();
                    Map<String, MetricValue> metricMap = itemValue.getMetrics();
                    if (keySource1.equals(itemValue.getKey())) {
                        assertEquals(1, metricMap.get("readSuccessCount").value);
                        assertEquals(100, metricMap.get("readSuccessSize").value);
                    } else if (keySource2.equals(key)) {
                        assertEquals(20, metricMap.get("readFailCount").value);
                        assertEquals(2000, metricMap.get("readFailSize").value);
                    } else if (keySink1.equals(key)) {
                        assertEquals(1, metricMap.get("sendCount").value);
                        assertEquals(100, metricMap.get("sendSize").value);
                        assertEquals(1, metricMap.get("sendSuccessCount").value);
                        assertEquals(100, metricMap.get("sendSuccessSize").value);
                    } else if (keySink2.equals(key)) {
                        assertEquals(20, metricMap.get("sendCount").value);
                        assertEquals(2000, metricMap.get("sendSize").value);
                        assertEquals(20, metricMap.get("sendFailCount").value);
                        assertEquals(2000, metricMap.get("sendFailSize").value);
                    } else {
                        System.out.println("bad MetricItem:" + key);
                    }
                }
            }
        };
        List<MetricListener> listeners = new ArrayList<>();
        listeners.add(listener);
        MetricListenerRunnable runnable = new MetricListenerRunnable("DataProxy", listeners);
        try {
            List<MetricItemValue> itemValues = runnable.getItemValues();
        } catch (Exception e) {
            LOG.error("has exception e = {}", e);
        }

        runnable.run();
    }
}
