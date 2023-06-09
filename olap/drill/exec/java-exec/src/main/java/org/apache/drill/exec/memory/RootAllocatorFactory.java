/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.memory;

import org.apache.drill.common.config.DrillConfig;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

public class RootAllocatorFactory {

  public static final String TOP_LEVEL_MAX_ALLOC = "drill.memory.top.max";

  /**
   * Constructor to prevent instantiation of this static utility class.
   */
  private RootAllocatorFactory() {}

  /**
   * Create a new Root Allocator
   * @param drillConfig
   *          the DrillConfig
   * @return a new root allocator
   */
  public static BufferAllocator newRoot(final DrillConfig drillConfig) {
    return newRoot(drillConfig.getLong(TOP_LEVEL_MAX_ALLOC));
  }

  @VisibleForTesting
  public static BufferAllocator newRoot(long maxAlloc) {
    return new RootAllocator(Math.min(DrillConfig.getMaxDirectMemory(), maxAlloc));
  }
}
