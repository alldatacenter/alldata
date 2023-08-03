/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.plugin.flink;

import org.apache.flink.configuration.Configuration;
import org.junit.Assert;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.plugin.flink.utils.FlinkUtils;

public class PluginSideConfSuiteJ {
  @Test
  public void testCoalesce() {
    Configuration flinkConf = new Configuration();
    CelebornConf celebornConf = FlinkUtils.toCelebornConf(flinkConf);
    Assert.assertEquals(8 * 1024 * 1024, celebornConf.clientFlinkMemoryPerResultPartitionMin());
    Assert.assertEquals(8 * 1024 * 1024, celebornConf.clientFlinkMemoryPerInputGateMin());
    Assert.assertTrue(Integer.MAX_VALUE == celebornConf.clientFlinkNumConcurrentReading());
    Assert.assertEquals(64 * 1024 * 1024, celebornConf.clientFlinkMemoryPerResultPartition());
    Assert.assertEquals(32 * 1024 * 1024, celebornConf.clientFlinkMemoryPerInputGate());

    Assert.assertEquals(true, celebornConf.clientFlinkDataCompressionEnabled());
    Assert.assertEquals("LZ4", celebornConf.shuffleCompressionCodec().name());

    flinkConf.setString("remote-shuffle.job.min.memory-per-partition", "16m");
    flinkConf.setString("remote-shuffle.job.min.memory-per-gate", "17m");
    flinkConf.setString("remote-shuffle.job.concurrent-readings-per-gate", "12323");
    flinkConf.setString("remote-shuffle.job.memory-per-partition", "1888m");
    flinkConf.setString("remote-shuffle.job.memory-per-gate", "176m");
    flinkConf.setString("remote-shuffle.job.enable-data-compression", "false");
    flinkConf.setString("remote-shuffle.job.support-floating-buffer-per-input-gate", "false");
    flinkConf.setString("remote-shuffle.job.compression.codec", "ZSTD");

    celebornConf = FlinkUtils.toCelebornConf(flinkConf);
    Assert.assertEquals(16 * 1024 * 1024, celebornConf.clientFlinkMemoryPerResultPartitionMin());
    Assert.assertEquals(17 * 1024 * 1024, celebornConf.clientFlinkMemoryPerInputGateMin());
    Assert.assertTrue(12323 == celebornConf.clientFlinkNumConcurrentReading());
    Assert.assertEquals(1888 * 1024 * 1024, celebornConf.clientFlinkMemoryPerResultPartition());
    Assert.assertEquals(176 * 1024 * 1024, celebornConf.clientFlinkMemoryPerInputGate());
    Assert.assertEquals(false, celebornConf.clientFlinkDataCompressionEnabled());
    Assert.assertEquals("ZSTD", celebornConf.shuffleCompressionCodec().name());
  }
}
