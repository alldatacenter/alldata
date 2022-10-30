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

package org.apache.inlong.tubemq.client.consumer;

import org.apache.inlong.tubemq.client.common.StatsConfig;
import org.apache.inlong.tubemq.client.common.StatsLevel;
import org.junit.Assert;
import org.junit.Test;

public class StatsConfigTest {
    @Test
    public void testStatsConfig() {
        StatsConfig statsConfig = new StatsConfig();
        Assert.assertEquals(statsConfig.getStatsLevel(), StatsLevel.MEDIUM);
        Assert.assertTrue(statsConfig.isEnableSelfPrint());
        Assert.assertEquals(statsConfig.getSelfPrintPeriodMs(), 6 * 1000 * 60L);
        Assert.assertEquals(statsConfig.getForcedResetPeriodMs(), 30 * 60 * 1000L);
        // test apis
        statsConfig.updateStatsControl(StatsLevel.FULL, false);
        Assert.assertEquals(statsConfig.getStatsLevel(), StatsLevel.FULL);
        Assert.assertFalse(statsConfig.isEnableSelfPrint());
        statsConfig.setStatsPeriodInfo(3000, 5000);
        Assert.assertEquals(statsConfig.getSelfPrintPeriodMs(), 1000 * 60L);
        Assert.assertEquals(statsConfig.getForcedResetPeriodMs(), 30 * 1000L);
        // test case 2
        statsConfig.updateStatsConfig(StatsLevel.ZERO, true, 300000L, 50000L);
        StatsConfig statsConfig2 = new StatsConfig();
        statsConfig2.updateStatsConfig(statsConfig);
        Assert.assertEquals(statsConfig2.getStatsLevel(), StatsLevel.ZERO);
        Assert.assertEquals(statsConfig2.isEnableSelfPrint(), statsConfig.isEnableSelfPrint());
        Assert.assertEquals(statsConfig2.getSelfPrintPeriodMs(),
                statsConfig.getSelfPrintPeriodMs());
        Assert.assertEquals(statsConfig2.getForcedResetPeriodMs(),
                statsConfig.getForcedResetPeriodMs());
    }
}
