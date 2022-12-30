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

package com.bytedance.bitsail.connector.legacy.redis.sink;

import com.bytedance.bitsail.base.dirty.AbstractDirtyCollector;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.flink.core.dirty.FlinkBatchDirtyCollector;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.flink.types.Row;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

import java.io.IOException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class JedisOutputFormatTest {

  /**
   * Test exceptions in the last batch of data.
   */
  @Test
  public void testLastFlushWithDirtyRecord()throws IOException {
    Row row = new Row(1);

    JedisOutputFormat jedisOutputFormat = spy(JedisOutputFormat.class);

    jedisOutputFormat.jedisPool = new JedisPool();

    AbstractDirtyCollector dirtyCollector = mock(FlinkBatchDirtyCollector.class);
    jedisOutputFormat.setDirtyCollector(dirtyCollector);
    jedisOutputFormat.setEmptyMessenger();

    jedisOutputFormat.recordQueue = new CircularFifoQueue<>();
    jedisOutputFormat.recordQueue.add(row);

    doThrow(BitSailException.class).when(jedisOutputFormat).flush();
    jedisOutputFormat.close();
  }
}

