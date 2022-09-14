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

package org.apache.inlong.common.metric.set;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.inlong.common.metric.MetricItem;
import org.apache.inlong.common.metric.MetricItemMBean;
import org.apache.inlong.common.metric.MetricItemSetMBean;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.common.metric.MetricUtils;
import org.apache.inlong.common.metric.MetricValue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * TestMetricItemSetMBean
 */
public class TestMetricItemSetMBean {

    public static final String SET_ID = "inlong5th_sz";
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

    /**
     * setup
     */
    @BeforeClass
    public static void setup() {
        itemSet = DataProxyMetricItemSet.getInstance();
        MetricRegister.register(itemSet);
        // prepare
        DataProxyMetricItem itemSource = new DataProxyMetricItem();
        itemSource.setId = SET_ID;
        itemSource.containerName = CONTAINER_NAME;
        itemSource.containerIp = CONTAINER_IP;
        itemSource.sourceId = SOURCE_ID;
        itemSource.sourceDataId = SOURCE_DATA_ID;
        itemSource.inlongGroupId = INLONG_GROUP_ID1;
        itemSource.inlongStreamId = INLONG_STREAM_ID;
        dimSource = itemSource.getDimensions();
        //
        DataProxyMetricItem itemSink = new DataProxyMetricItem();
        itemSink.setId = SET_ID;
        itemSink.containerName = CONTAINER_NAME;
        itemSink.containerIp = CONTAINER_IP;
        itemSink.sinkId = SINK_ID;
        itemSink.sinkDataId = SINK_DATA_ID;
        itemSink.inlongGroupId = INLONG_GROUP_ID1;
        itemSink.inlongStreamId = INLONG_STREAM_ID;
        dimSink = itemSink.getDimensions();
    }

    /**
     * testResult
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testResult() throws Exception {
        // increase source
        DataProxyMetricItem item = null;
        item = itemSet.findMetricItem(dimSource);
        item.readSuccessCount.incrementAndGet();
        item.readSuccessSize.addAndGet(100);
        String keySource1 = MetricUtils.getDimensionsKey(dimSource);
        //
        dimSource.put("inlongGroupId", INLONG_GROUP_ID2);
        item = itemSet.findMetricItem(dimSource);
        item.readFailCount.addAndGet(20);
        item.readFailSize.addAndGet(2000);
        String keySource2 = MetricUtils.getDimensionsKey(dimSource);
        // increase sink
        item = itemSet.findMetricItem(dimSink);
        item.sendCount.incrementAndGet();
        item.sendSize.addAndGet(100);
        item.sendSuccessCount.incrementAndGet();
        item.sendSuccessSize.addAndGet(100);
        String keySink1 = MetricUtils.getDimensionsKey(dimSink);
        //
        dimSink.put("inlongGroupId", INLONG_GROUP_ID2);
        item = itemSet.findMetricItem(dimSink);
        item.sendCount.addAndGet(20);
        item.sendSize.addAndGet(2000);
        item.sendFailCount.addAndGet(20);
        item.sendFailSize.addAndGet(2000);
        String keySink2 = MetricUtils.getDimensionsKey(dimSink);
        // report
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        StringBuilder beanName = new StringBuilder();
        beanName.append(MetricRegister.JMX_DOMAIN).append(MetricItemMBean.DOMAIN_SEPARATOR)
                .append("type=").append(MetricUtils.getDomain(DataProxyMetricItemSet.class))
                .append(MetricItemMBean.PROPERTY_SEPARATOR)
                .append("name=").append(itemSet.getName());
        String strBeanName = beanName.toString();
        ObjectName objName = new ObjectName(strBeanName);
        {
            List<MetricItem> items = (List<MetricItem>) mbs.invoke(objName, MetricItemSetMBean.METHOD_SNAPSHOT, null,
                    null);
            for (MetricItem itemObj : items) {
                if (keySource1.equals(itemObj.getDimensionsKey())) {
                    Map<String, MetricValue> metricMap = itemObj.snapshot();
                    assertEquals(1, metricMap.get("readSuccessCount").value);
                    assertEquals(100, metricMap.get("readSuccessSize").value);
                } else if (keySource2.equals(itemObj.getDimensionsKey())) {
                    Map<String, MetricValue> metricMap = itemObj.snapshot();
                    assertEquals(20, metricMap.get("readFailCount").value);
                    assertEquals(2000, metricMap.get("readFailSize").value);
                } else if (keySink1.equals(itemObj.getDimensionsKey())) {
                    Map<String, MetricValue> metricMap = itemObj.snapshot();
                    assertEquals(1, metricMap.get("sendCount").value);
                    assertEquals(100, metricMap.get("sendSize").value);
                    assertEquals(1, metricMap.get("sendSuccessCount").value);
                    assertEquals(100, metricMap.get("sendSuccessSize").value);
                } else if (keySink2.equals(itemObj.getDimensionsKey())) {
                    Map<String, MetricValue> metricMap = itemObj.snapshot();
                    assertEquals(20, metricMap.get("sendCount").value);
                    assertEquals(2000, metricMap.get("sendSize").value);
                    assertEquals(20, metricMap.get("sendFailCount").value);
                    assertEquals(2000, metricMap.get("sendFailSize").value);
                } else {
                    System.out.println("bad MetricItem:" + itemObj.getDimensionsKey());
                }
            }
        }
    }
}
