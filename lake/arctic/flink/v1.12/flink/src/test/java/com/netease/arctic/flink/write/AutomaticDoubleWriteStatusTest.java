/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.write;

import com.netease.arctic.flink.FlinkTestBase;
import com.netease.arctic.flink.table.ArcticTableLoader;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.LOG_STORE_CATCH_UP;

public class AutomaticDoubleWriteStatusTest extends FlinkTestBase {
  public ArcticTableLoader tableLoader;

  @Test
  public void testTableProperties() {
    tableLoader = ArcticTableLoader.of(PK_TABLE_ID, catalogBuilder);

    AutomaticDoubleWriteStatus status = new AutomaticDoubleWriteStatus(tableLoader, Duration.ofSeconds(10));
    status.open();
    Assert.assertFalse(status.isDoubleWrite());
    status.processWatermark(new Watermark(System.currentTimeMillis() - 11 * 1000));
    Assert.assertFalse(status.isDoubleWrite());
    Assert.assertFalse(
        Boolean.parseBoolean(tableLoader.loadArcticTable().properties()
            .getOrDefault(LOG_STORE_CATCH_UP.key(), "false")));
    status.processWatermark(new Watermark(System.currentTimeMillis() - 9 * 1000));
    Assert.assertTrue(status.isDoubleWrite());

    Assert.assertTrue(status.isDoubleWrite());
    Assert.assertTrue(
        Boolean.parseBoolean(tableLoader.loadArcticTable().properties()
            .getOrDefault(LOG_STORE_CATCH_UP.key(), "false")));
  }
}