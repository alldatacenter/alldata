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

import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.util.ArcticUtils;
import com.netease.arctic.table.ArcticTable;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.iceberg.UpdateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.LOG_STORE_CATCH_UP;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.LOG_STORE_CATCH_UP_TIMESTAMP;

/**
 * This is an automatic logstore writer util class.
 */
public class AutomaticDoubleWriteStatus implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(AutomaticDoubleWriteStatus.class);

  private static final long serialVersionUID = 1L;
  private final ArcticTableLoader tableLoader;
  private final AutomaticWriteSpecification specification;
  private ArcticTable table;
  private transient boolean shouldDoubleWrite = false;
  private int subtaskId;

  public AutomaticDoubleWriteStatus(ArcticTableLoader tableLoader, Duration writeLogstoreWatermarkGap) {
    this.tableLoader = tableLoader;
    this.specification = new AutomaticWriteSpecification(writeLogstoreWatermarkGap);
  }

  public void setup(int indexOfThisSubtask) {
    this.subtaskId = indexOfThisSubtask;
  }

  public void open() {
    table = ArcticUtils.loadArcticTable(tableLoader);
    sync();
  }

  public boolean isDoubleWrite() {
    return shouldDoubleWrite;
  }

  public void processWatermark(Watermark mark) {
    if (isDoubleWrite()) {
      return;
    }
    if (specification.shouldDoubleWrite(mark.getTimestamp())) {
      shouldDoubleWrite = true;
      LOG.info("processWatermark {}, subTaskId is {}, should double write is true.", mark, subtaskId);
      LOG.info("begin update arctic table, set {} to true", LOG_STORE_CATCH_UP.key());
      UpdateProperties updateProperties = table.updateProperties();
      updateProperties.set(LOG_STORE_CATCH_UP.key(), String.valueOf(true));
      updateProperties.set(LOG_STORE_CATCH_UP_TIMESTAMP.key(), String.valueOf(System.currentTimeMillis()));
      updateProperties.commit();
      LOG.info("end update arctic table.");
    }
  }

  public void sync() {
    table.refresh();
    Map<String, String> properties = table.properties();
    shouldDoubleWrite =
        Boolean.parseBoolean(
            properties.getOrDefault(LOG_STORE_CATCH_UP.key(), String.valueOf(LOG_STORE_CATCH_UP.defaultValue())));
    LOG.info("AutomaticDoubleWriteStatus sync, subTaskId: {}, should double write: {}", subtaskId, shouldDoubleWrite);
  }
}
