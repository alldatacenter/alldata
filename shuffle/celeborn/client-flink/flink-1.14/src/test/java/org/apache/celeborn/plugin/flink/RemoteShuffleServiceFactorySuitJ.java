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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.io.network.api.writer.ResultPartitionWriter;
import org.apache.flink.runtime.io.network.partition.consumer.IndexedInputGate;
import org.apache.flink.runtime.shuffle.ShuffleEnvironment;
import org.apache.flink.runtime.shuffle.ShuffleEnvironmentContext;
import org.junit.Assert;
import org.junit.Test;

public class RemoteShuffleServiceFactorySuitJ {
  @Test
  public void testCreateShuffleEnvironment() {
    RemoteShuffleServiceFactory remoteShuffleServiceFactory = new RemoteShuffleServiceFactory();
    ShuffleEnvironmentContext shuffleEnvironmentContext = mock(ShuffleEnvironmentContext.class);
    when(shuffleEnvironmentContext.getConfiguration()).thenReturn(new Configuration());
    when(shuffleEnvironmentContext.getNetworkMemorySize())
        .thenReturn(new MemorySize(64 * 1024 * 1024));
    MetricGroup parentMetric = mock(MetricGroup.class);
    when(shuffleEnvironmentContext.getParentMetricGroup()).thenReturn(parentMetric);
    MetricGroup childGroup = mock(MetricGroup.class);
    MetricGroup childChildGroup = mock(MetricGroup.class);
    when(parentMetric.addGroup(anyString())).thenReturn(childGroup);
    when(childGroup.addGroup(any())).thenReturn(childChildGroup);
    when(childChildGroup.gauge(any(), any())).thenReturn(null);
    ShuffleEnvironment<ResultPartitionWriter, IndexedInputGate> shuffleEnvironment =
        remoteShuffleServiceFactory.createShuffleEnvironment(shuffleEnvironmentContext);
    Assert.assertEquals(
        32 * 1024,
        ((RemoteShuffleEnvironment) shuffleEnvironment)
            .getResultPartitionFactory()
            .getNetworkBufferSize());
  }
}
