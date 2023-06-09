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
package org.apache.drill.exec.util;

import static org.junit.Assert.assertEquals;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.DrillTest;
import org.apache.drill.test.OperatorFixture;
import org.junit.Rule;
import org.junit.Test;

public class TestQueryMemoryAlloc extends DrillTest {

  public static final long ONE_MB = 1024 * 1024;
  public static final long ONE_GB = 1024L * ONE_MB;

  @Rule
  public final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @Test
  public void testDefaultOptions() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    builder.systemOption(ExecConstants.PERCENT_MEMORY_PER_QUERY_KEY, 0.05);
    builder.systemOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE_KEY, 2 * ONE_GB);

    try (OperatorFixture fixture = builder.build()) {
      final OptionManager optionManager = fixture.getOptionManager();

      optionManager.setLocalOption(ExecConstants.PERCENT_MEMORY_PER_QUERY_KEY, 0.05);
      optionManager.setLocalOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE_KEY, 2 * ONE_GB);

      // Out-of-box memory, use query memory per node as floor.
      long mem = MemoryAllocationUtilities.computeQueryMemory(fixture.config(), optionManager, 8 * ONE_GB);
      assertEquals(2 * ONE_GB, mem);

      // Up to 40 GB, query memory dominates.

      mem = MemoryAllocationUtilities.computeQueryMemory(fixture.config(), optionManager, 40 * ONE_GB);
      assertEquals(2 * ONE_GB, mem);

      // After 40 GB, the percent dominates

      mem = MemoryAllocationUtilities.computeQueryMemory(fixture.config(), optionManager, 100 * ONE_GB);
      assertEquals(5 * ONE_GB, mem);
    }
  }

  @Test
  public void testCustomFloor() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    builder.systemOption(ExecConstants.PERCENT_MEMORY_PER_QUERY_KEY, 0.05);
    builder.systemOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE_KEY, 2 * ONE_GB);

    try (OperatorFixture fixture = builder.build()) {
      final OptionManager optionManager = fixture.getOptionManager();

      optionManager.setLocalOption(ExecConstants.PERCENT_MEMORY_PER_QUERY_KEY, 0.05);
      optionManager.setLocalOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE_KEY, 2 * ONE_GB);

      // Out-of-box memory, use query memory per node as floor.
      long mem = MemoryAllocationUtilities.computeQueryMemory(fixture.config(), optionManager, 8 * ONE_GB);
      assertEquals(2 * ONE_GB, mem);

      // Up to 60 GB, query memory dominates.

      mem = MemoryAllocationUtilities.computeQueryMemory(fixture.config(), optionManager, 60 * ONE_GB);
      assertEquals(3 * ONE_GB, mem);

      // After 60 GB, the percent dominates

      mem = MemoryAllocationUtilities.computeQueryMemory(fixture.config(), optionManager, 100 * ONE_GB);
      assertEquals(5 * ONE_GB, mem);
    }
  }

  @Test
  public void testCustomPercent() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    builder.systemOption(ExecConstants.PERCENT_MEMORY_PER_QUERY_KEY, 0.10);
    builder.systemOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE_KEY, 2 * ONE_GB);

    try (OperatorFixture fixture = builder.build()) {
      final OptionManager optionManager = fixture.getOptionManager();

      optionManager.setLocalOption(ExecConstants.PERCENT_MEMORY_PER_QUERY_KEY, 0.10);
      optionManager.setLocalOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE_KEY, 2 * ONE_GB);

      // Out-of-box memory, use query memory per node as floor.

      long mem = MemoryAllocationUtilities.computeQueryMemory(fixture.config(), optionManager, 8 * ONE_GB);
      assertEquals(2 * ONE_GB, mem);

      // Up to 20 GB, query memory dominates.

      mem = MemoryAllocationUtilities.computeQueryMemory(fixture.config(), optionManager, 20 * ONE_GB);
      assertEquals(2 * ONE_GB, mem);

      // After 20 GB, the percent dominates

      mem = MemoryAllocationUtilities.computeQueryMemory(fixture.config(), optionManager, 30 * ONE_GB);
      assertEquals(3 * ONE_GB, mem);
    }
  }

  /**
   * Test with default options, various memory configs.
   * Since we can't change the actual CPUs on this node, use an
   * option to specify the number (rather than the usual 70% of
   * actual cores.)
   *
   * @throws Exception
   */

  @Test
  public void testOpMemory() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    builder.systemOption(ExecConstants.CPU_LOAD_AVERAGE_KEY, 0.7);
    builder.systemOption(ExecConstants.MAX_WIDTH_PER_NODE_KEY, 10);
    builder.systemOption(ExecConstants.MIN_MEMORY_PER_BUFFERED_OP_KEY, 40 * ONE_MB);

    try (OperatorFixture fixture = builder.build()) {
      final OptionManager optionManager = fixture.getOptionManager();

      optionManager.setLocalOption(ExecConstants.CPU_LOAD_AVERAGE_KEY, 0.7);
      optionManager.setLocalOption(ExecConstants.MAX_WIDTH_PER_NODE_KEY, 10);
      optionManager.setLocalOption(ExecConstants.MIN_MEMORY_PER_BUFFERED_OP_KEY, 40 * ONE_MB);

      // Enough memory to go above configured minimum.

      long opMinMem = MemoryAllocationUtilities.computeOperatorMemory(optionManager, 4 * ONE_GB, 2);
      assertEquals(4 * ONE_GB / 10 / 2, opMinMem);

      // Too little memory per operator. Use configured minimum.

      opMinMem = MemoryAllocationUtilities.computeOperatorMemory(optionManager, ONE_GB, 100);
      assertEquals(40 * ONE_MB, opMinMem);
    }
  }
}
