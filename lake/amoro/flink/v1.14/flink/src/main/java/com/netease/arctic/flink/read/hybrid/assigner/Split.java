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

package com.netease.arctic.flink.read.hybrid.assigner;

import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.util.Preconditions;

/**
 * This is a wrapper Split of {@link ArcticSplit} with split status.
 */
public class Split {

  public enum Status {
    AVAILABLE,

    /**
     * Assigner has pending splits. But current subtask doesn't have pending splits.
     */
    SUBTASK_UNAVAILABLE,

    /**
     * Assigner doesn't have pending splits.
     */
    UNAVAILABLE
  }

  private final Status status;
  private final ArcticSplit split;

  private Split(Status status) {
    this.status = status;
    this.split = null;
  }

  private Split(ArcticSplit split) {
    Preconditions.checkNotNull(split, "Split cannot be null");
    this.status = Status.AVAILABLE;
    this.split = split;
  }

  @VisibleForTesting
  public Status status() {
    return status;
  }

  public boolean isAvailable() {
    return status == Status.AVAILABLE;
  }

  public boolean isUnavailable() {
    return status == Status.UNAVAILABLE;
  }

  public ArcticSplit split() {
    return split;
  }

  private static final Split UNAVAILABLE = new Split(Status.UNAVAILABLE);
  private static final Split SUBTASK_UNAVAILABLE = new Split(Status.SUBTASK_UNAVAILABLE);


  public static Split unavailable() {
    return UNAVAILABLE;
  }

  public static Split subtaskUnavailable() {
    return SUBTASK_UNAVAILABLE;
  }


  public static Split of(ArcticSplit arcticSplit) {
    return new Split(arcticSplit);
  }
}
