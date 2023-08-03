/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.netease.arctic.flink.read.hybrid.enumerator;

import org.apache.commons.compress.utils.Lists;
import org.apache.flink.annotation.Internal;
import org.apache.iceberg.expressions.Expression;

import java.io.Closeable;
import java.util.List;

/**
 * This interface is introduced so that we can plug in a different split planner for unit test
 */
@Internal
public interface ContinuousSplitPlanner extends Closeable {

  /**
   * Discover the files appended between {@code lastPosition} and current table snapshot
   */
  default ContinuousEnumerationResult planSplits(ArcticEnumeratorOffset lastPosition) {
    return planSplits(lastPosition, Lists.newArrayList());
  }

  /**
   * Discover the files appended between {@code lastPosition} and current table snapshot,
   * filter the data with expressions.
   */
  ContinuousEnumerationResult planSplits(ArcticEnumeratorOffset lastPosition, List<Expression> filters);
}
