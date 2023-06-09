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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.netty.buffer.DrillBuf;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.drill.categories.MemoryTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.ops.OpProfileDef;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.ops.OperatorUtilities;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.UnionAll;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.proto.BitControl;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.StoragePluginRegistryImpl;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin;
import org.apache.drill.exec.store.mock.MockSubScanPOP;
import org.apache.drill.exec.vector.BitVector;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.test.DrillTest;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.junit.experimental.categories.Category;

@Category(MemoryTest.class)
public class TestAllocators extends DrillTest {

  private static final Properties TEST_CONFIGURATIONS = new Properties() {
    {
      put(RootAllocatorFactory.TOP_LEVEL_MAX_ALLOC, "14000000");
      // put(AccountorImpl.ENABLE_FRAGMENT_MEMORY_LIMIT, "true");
      // put(AccountorImpl.FRAGMENT_MEM_OVERCOMMIT_FACTOR, "1.1");
    }
  };

  private final static String planFile = "/physical_allocator_test.json";

  /**
   * Contract for DrillBuf[] returned from getBuffers() is that buffers are returned in a reader appropriate state
   * (i.e., readIndex = 0)
   *
   * Before this patch, the following scenario breaks this contract:
   * As data being read from DrillBuf, readIndex will be pushed forward. And, later on,
   * when DrillBuf[] are read from the ValueVector, readIndex will point at the location of the most recent reading
   *
   * This unit test is added to ensure that the readIndex points at zero under this scenario
   */
  @Test // DRILL-3854
  public void ensureDrillBufReadIndexIsZero() throws Exception {
    final int length = 10;

    final Properties props = new Properties() {
      {
        put(RootAllocatorFactory.TOP_LEVEL_MAX_ALLOC, "1000000");
      }
    };
    final DrillConfig config = DrillConfig.create(props);

    final BufferAllocator allc = RootAllocatorFactory.newRoot(config);
    final TypeProtos.MajorType.Builder builder = TypeProtos.MajorType.newBuilder();
    builder.setMinorType(TypeProtos.MinorType.INT);
    builder.setMode(TypeProtos.DataMode.REQUIRED);

    final IntVector iv = new IntVector(MaterializedField.create("Field", builder.build()), allc);
    iv.allocateNew();

    // Write data to DrillBuf
    for(int i = 0; i < length; ++i) {
      iv.getBuffer().writeInt(i);
    }

    // Read data to DrillBuf
    for(int i = 0; i < length; ++i) {
      assertEquals(i, iv.getBuffer().readInt());
    }

    for(DrillBuf drillBuf : iv.getBuffers(false)) {
      assertEquals(0, drillBuf.readInt());
    }

    final List<DrillBuf> toBeClean = Lists.newArrayList();
    for(DrillBuf drillBuf : iv.getBuffers(true)) {
      assertEquals(0, drillBuf.readInt());
      toBeClean.add(drillBuf);
    }

    for(DrillBuf drillBuf : toBeClean) {
      drillBuf.release();
    }

    allc.close();
  }

  @Test
  public void testClearBitVector() {
    final Properties props = new Properties() {
      {
        put(RootAllocatorFactory.TOP_LEVEL_MAX_ALLOC, "1000000");
      }
    };
    final DrillConfig config = DrillConfig.create(props);

    final BufferAllocator allc = RootAllocatorFactory.newRoot(config);
    final TypeProtos.MajorType.Builder builder = TypeProtos.MajorType.newBuilder();
    builder.setMinorType(TypeProtos.MinorType.BIT);
    builder.setMode(TypeProtos.DataMode.REQUIRED);

    final BitVector bv = new BitVector(MaterializedField.create("Field", builder.build()), allc);
    bv.getMutator().setValueCount(1);
    assertEquals(bv.getAccessor().getValueCount(), 1);

    bv.clear();
    assertEquals(bv.getAccessor().getValueCount(), 0);
  }

  @Test
  public void testTransfer() throws Exception {
    final Properties props = new Properties() {
      {
        put(RootAllocatorFactory.TOP_LEVEL_MAX_ALLOC, "1049600");
      }
    };
    final DrillConfig config = DrillConfig.create(props);
    BufferAllocator a = RootAllocatorFactory.newRoot(config);
    BufferAllocator a1 = a.newChildAllocator("a1", 0, Integer.MAX_VALUE);
    BufferAllocator a2 = a.newChildAllocator("a2", 0, Integer.MAX_VALUE);

    DrillBuf buf1 = a1.buffer(1_000_000);
    DrillBuf buf2 = a2.buffer(1_000);
    DrillBuf buf3 = buf1.transferOwnership(a2).buffer;

    buf1.release();
    buf2.release();
    buf3.release();

    a1.close();
    a2.close();
    a.close();
  }

  @Test
  public void testAllocators() throws Exception {
    // Setup a drillbit (initializes a root allocator)
    final DrillConfig config = DrillConfig.create(TEST_CONFIGURATIONS);

    try (final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
         final Drillbit bit = new Drillbit(config, serviceSet)) {
      bit.run();
      final DrillbitContext bitContext = bit.getContext();
      FunctionImplementationRegistry functionRegistry = bitContext.getFunctionImplementationRegistry();
      StoragePluginRegistry storageRegistry = new StoragePluginRegistryImpl(bitContext);

      // Create a few Fragment Contexts

      BitControl.PlanFragment.Builder pfBuilder1 = BitControl.PlanFragment.newBuilder();
      pfBuilder1.setMemInitial(1500000);
      BitControl.PlanFragment pf1 = pfBuilder1.build();
      BitControl.PlanFragment.Builder pfBuilder2 = BitControl.PlanFragment.newBuilder();
      pfBuilder2.setMemInitial(500000);
      BitControl.PlanFragment pf2 = pfBuilder1.build();

      FragmentContextImpl fragmentContext1 = new FragmentContextImpl(bitContext, pf1, null, functionRegistry);
      FragmentContextImpl fragmentContext2 = new FragmentContextImpl(bitContext, pf2, null, functionRegistry);

      // Get a few physical operators. Easiest way is to read a physical plan.
      PhysicalPlanReader planReader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(bitContext,
          storageRegistry);
      PhysicalPlan plan = planReader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile(planFile),
          Charsets.UTF_8).read());
      List<PhysicalOperator> physicalOperators = plan.getSortedOperators();
      Iterator<PhysicalOperator> physicalOperatorIterator = physicalOperators.iterator();

      PhysicalOperator physicalOperator1 = physicalOperatorIterator.next();
      PhysicalOperator physicalOperator2 = physicalOperatorIterator.next();
      PhysicalOperator physicalOperator3 = physicalOperatorIterator.next();
      PhysicalOperator physicalOperator4 = physicalOperatorIterator.next();
      PhysicalOperator physicalOperator5 = physicalOperatorIterator.next();
      PhysicalOperator physicalOperator6 = physicalOperatorIterator.next();

      // Create some bogus Operator profile defs and stats to create operator contexts
      OpProfileDef def;
      OperatorStats stats;

      // Use some bogus operator type to create a new operator context.
      def = new OpProfileDef(physicalOperator1.getOperatorId(), MockSubScanPOP.OPERATOR_TYPE,
          OperatorUtilities.getChildCount(physicalOperator1));
      stats = fragmentContext1.getStats().newOperatorStats(def, fragmentContext1.getAllocator());

      // Add a couple of Operator Contexts
      // Initial allocation = 1000000 bytes for all operators
      OperatorContext oContext11 = fragmentContext1.newOperatorContext(physicalOperator1);
      DrillBuf b11 = oContext11.getAllocator().buffer(1000000);

      OperatorContext oContext12 = fragmentContext1.newOperatorContext(physicalOperator2, stats);
      DrillBuf b12 = oContext12.getAllocator().buffer(500000);

      OperatorContext oContext21 = fragmentContext1.newOperatorContext(physicalOperator3);

      def = new OpProfileDef(physicalOperator4.getOperatorId(), TextFormatPlugin.WRITER_OPERATOR_TYPE,
          OperatorUtilities.getChildCount(physicalOperator4));
      stats = fragmentContext2.getStats().newOperatorStats(def, fragmentContext2.getAllocator());
      OperatorContext oContext22 = fragmentContext2.newOperatorContext(physicalOperator4, stats);
      DrillBuf b22 = oContext22.getAllocator().buffer(2000000);

      // New Fragment begins
      BitControl.PlanFragment.Builder pfBuilder3 = BitControl.PlanFragment.newBuilder();
      pfBuilder3.setMemInitial(1000000);
      BitControl.PlanFragment pf3 = pfBuilder3.build();

      FragmentContextImpl fragmentContext3 = new FragmentContextImpl(bitContext, pf3, null, functionRegistry);

      // New fragment starts an operator that allocates an amount within the limit
      def = new OpProfileDef(physicalOperator5.getOperatorId(), UnionAll.OPERATOR_TYPE,
          OperatorUtilities.getChildCount(physicalOperator5));
      stats = fragmentContext3.getStats().newOperatorStats(def, fragmentContext3.getAllocator());
      OperatorContext oContext31 = fragmentContext3.newOperatorContext(physicalOperator5, stats);

      DrillBuf b31a = oContext31.getAllocator().buffer(200000);

      // Previously running operator completes
      b22.release();
      ((AutoCloseable) oContext22).close();

      // Fragment 3 asks for more and fails
      boolean outOfMem = false;
      try {
        oContext31.getAllocator().buffer(44000000);
        fail("Fragment 3 should fail to allocate buffer");
      } catch (OutOfMemoryException e) {
        outOfMem = true; // Expected.
      }
      assertTrue(outOfMem);

      // Operator is Exempt from Fragment limits. Fragment 3 asks for more and succeeds
      OperatorContext oContext32 = fragmentContext3.newOperatorContext(physicalOperator6);
      try {
        DrillBuf b32 = oContext32.getAllocator().buffer(4400000);
        b32.release();
      } catch (OutOfMemoryException e) {
        fail("Fragment 3 failed to allocate buffer");
      } finally {
        closeOp(oContext32);
      }

      b11.release();
      closeOp(oContext11);
      b12.release();
      closeOp(oContext12);
      closeOp(oContext21);
      b31a.release();
      closeOp(oContext31);

      fragmentContext1.close();
      fragmentContext2.close();
      fragmentContext3.close();

    }
  }

  private void closeOp(OperatorContext c) throws Exception {
    ((AutoCloseable) c).close();
  }
}
