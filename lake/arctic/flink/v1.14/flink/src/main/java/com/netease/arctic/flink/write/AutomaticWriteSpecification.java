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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.Duration;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.AUTO_EMIT_LOGSTORE_WATERMARK_GAP;

/**
 * Automatic write specification.
 */
public class AutomaticWriteSpecification implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(AutomaticWriteSpecification.class);

  private static final long serialVersionUID = 1L;
  public final Duration writeLogstoreWatermarkGap;

  public AutomaticWriteSpecification(@Nullable Duration writeLogstoreWatermarkGap) {
    this.writeLogstoreWatermarkGap = writeLogstoreWatermarkGap;
  }

  /**
   * Returns whether the automatic writing is enabled.
   *
   * @param watermark the watermark of the operator
   * @return true: double write, false: single write.
   */
  public boolean shouldDoubleWrite(long watermark) {
    // The writeLogstoreWatermarkGap is null, which means that the logstore writer is enabled immediately once the job
    // is launched.
    if (writeLogstoreWatermarkGap == null) {
      LOG.info("The logstore writer is enabled and the {} is null," +
              " so double write immediately once the job is launched.",
          AUTO_EMIT_LOGSTORE_WATERMARK_GAP.key());
      return true;
    }
    long now = System.currentTimeMillis();
    boolean result = watermark >= now - writeLogstoreWatermarkGap.toMillis();
    if (result) {
      LOG.info("The logstore writer is enabled and the {} is {}, the watermark has caught up, {}.",
          AUTO_EMIT_LOGSTORE_WATERMARK_GAP.key(), writeLogstoreWatermarkGap, watermark);
    } else {
      LOG.debug("The logstore writer is enabled and the {} is {}, the watermark has not caught up, {}.",
          AUTO_EMIT_LOGSTORE_WATERMARK_GAP.key(), writeLogstoreWatermarkGap, watermark);
    }
    return result;
  }
}
