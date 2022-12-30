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
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of the {@link RollingPolicy}.
 *
 * <p>This policy rolls a part file if:
 * <ol>
 *     <li>there is no open part file,</li>
 * 	   <li>the current file has reached the maximum partition size (by default 128MB),</li>
 * 	   <li>the current file is older than the roll over interval (by default 600 sec). or</li>
 * 	   <li>the current file has not been written to for more than the allowed inactivityTime (by default 600 sec).</li>
 * </ol>
 */
@PublicEvolving
public final class DefaultRollingPolicy<IN> implements RollingPolicy<IN> {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultRollingPolicy.class);

  private static final long serialVersionUID = 1L;

  private final long partSize;

  private final long rolloverInterval;

  private final long inactivityInterval;

  /**
   * Private constructor to avoid direct instantiation.
   */
  private DefaultRollingPolicy(long partSize, long rolloverInterval, long inactivityInterval) {
    Preconditions.checkArgument(partSize > 0L);
    Preconditions.checkArgument(rolloverInterval > 0L);
    Preconditions.checkArgument(inactivityInterval > 0L);

    this.partSize = partSize;
    this.rolloverInterval = rolloverInterval;
    this.inactivityInterval = inactivityInterval;
  }

  /**
   * Initiates the instantiation of a {@code DefaultRollingPolicy}.
   * To finalize it and have the actual policy, call {@code .create()}.
   */
  public static PolicyBuilder create() {
    return new PolicyBuilder(
        StreamingFileSystemValidator.ROLLING_MAX_PART_SIZE_DEFAULT,
        StreamingFileSystemValidator.ROLLING_ROLLOVER_INTERVAL_DEFAULT,
        StreamingFileSystemValidator.ROLLING_INACTIVITY_INTERVAL_DEFAULT);
  }

  @Override
  public boolean shouldRollOnEvent(final PartFileInfo partFileState, IN element) {
    return partFileState.getSize() > partSize;
  }

  @Override
  public boolean shouldRollOnProcessingTime(final PartFileInfo partFileState, final long currentTime) {
    LOG.debug("closing in-progress part file due to processing time rolling policy " +
            "(in-progress file created @ {}, last updated @ {} and current time is {}).",
        partFileState.getCreationTime(), partFileState.getLastUpdateTime(), currentTime);
    return currentTime - partFileState.getCreationTime() >= rolloverInterval ||
        currentTime - partFileState.getLastUpdateTime() >= inactivityInterval;
  }

  /**
   * A helper class that holds the configuration properties for the {@link DefaultRollingPolicy}.
   */
  @PublicEvolving
  public static final class PolicyBuilder {

    private long partSize;

    private long rolloverInterval;

    private long inactivityInterval;

    public PolicyBuilder(
        final long partSize,
        final long rolloverInterval,
        final long inactivityInterval) {
      this.partSize = partSize;
      this.rolloverInterval = rolloverInterval;
      this.inactivityInterval = inactivityInterval;
    }

    /**
     * Sets the part size above which a part file will have to roll.
     *
     * @param size the allowed part size.
     */
    public PolicyBuilder withMaxPartSize(final long size) {
      Preconditions.checkState(size > 0L);
      this.partSize = size;
      return this;
    }

    /**
     * Sets the interval of allowed inactivity after which a part file will have to roll.
     *
     * @param interval the allowed inactivity interval.
     */
    public PolicyBuilder withInactivityInterval(final long interval) {
      Preconditions.checkState(interval > 0L);
      this.inactivityInterval = interval;
      return this;
    }

    /**
     * Sets the max time a part file can stay open before having to roll.
     *
     * @param interval the desired rollover interval.
     */
    public PolicyBuilder withRolloverInterval(final long interval) {
      Preconditions.checkState(interval > 0L);
      this.rolloverInterval = interval;
      return this;
    }

    /**
     * Creates the actual policy.
     */
    public <IN> DefaultRollingPolicy<IN> build() {
      return new DefaultRollingPolicy<>(partSize, rolloverInterval, inactivityInterval);
    }
  }
}
