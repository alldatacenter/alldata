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

package org.apache.inlong.tubemq.server.broker.stats;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * TrafficStatsService test.
 */
public class TrafficStatsServiceTest {

    @Test
    public void testTrafficInfo() {
        // case 1
        TrafficInfo trafficInfo1 = new TrafficInfo();
        trafficInfo1.addMsgCntAndSize(1, 100);
        trafficInfo1.addMsgCntAndSize(3, 500);
        Assert.assertEquals(4, trafficInfo1.getMsgCount());
        Assert.assertEquals(600, trafficInfo1.getMsgSize());
        trafficInfo1.clear();
        trafficInfo1.addMsgCntAndSize(50, 5000);
        Assert.assertEquals(50, trafficInfo1.getMsgCount());
        Assert.assertEquals(5000, trafficInfo1.getMsgSize());
        // case 2
        TrafficInfo trafficInfo2 = new TrafficInfo(99, 1000);
        trafficInfo2.addMsgCntAndSize(1, 100);
        Assert.assertEquals(100, trafficInfo2.getMsgCount());
        Assert.assertEquals(1100, trafficInfo2.getMsgSize());
    }

    @Test
    public void testTrafficStatsService() {
        TrafficStatsService trafficService =
                new TrafficStatsService("PutCounterGroup", "Producer", 60 * 1000L);
        trafficService.add("key", 1L, 100);
        Map<String, TrafficInfo> items = new HashMap<>();
        items.put("key1", new TrafficInfo(1L, 1024));
        items.put("key2", new TrafficInfo(1L, 1024));
        // add counts
        trafficService.add(items);
        trafficService.add("key3", 3L, 500L);
    }
}
