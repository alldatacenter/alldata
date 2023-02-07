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
package org.apache.drill.exec.record;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.exec.store.mock.MockSubScanPOP;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.drill.categories.VectorTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.ops.OpProfileDef;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.ops.OperatorUtilities;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.impl.ImplCreator;
import org.apache.drill.exec.physical.impl.SimpleRootExec;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.proto.BitControl;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.vector.ValueVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.util.List;

@Category(VectorTest.class)
public class TestRecordIterator extends PopUnitTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestRecordIterator.class);
  DrillConfig c = DrillConfig.create();

  @Test
  public void testSimpleIterator() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);

    final String planStr = Files.asCharSource(DrillFileUtils.getResourceAsFile("/record/test_recorditerator.json"), Charsets.UTF_8).read();

    final PhysicalPlan plan = reader.readPhysicalPlan(planStr);
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, BitControl.PlanFragment.getDefaultInstance(), connection, registry);
    final List<PhysicalOperator> operatorList = plan.getSortedOperators(false);
    SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) operatorList.iterator().next()));

    RecordBatch singleBatch = exec.getIncoming();
    PhysicalOperator dummyPop = operatorList.iterator().next();
    OpProfileDef def = new OpProfileDef(dummyPop.getOperatorId(), MockSubScanPOP.OPERATOR_TYPE,
      OperatorUtilities.getChildCount(dummyPop));
    OperatorStats stats = exec.getContext().getStats().newOperatorStats(def, exec.getContext().getAllocator());
    RecordIterator iter = new RecordIterator(singleBatch, null, exec.getContext().newOperatorContext(dummyPop, stats), 0, false, null);
    int totalRecords = 0;
    List<ValueVector> vectors = null;

    while (true) {
      iter.next();
      if (iter.finished()) {
        break;
      } else {
        // First time save vectors.
        if (vectors == null) {
          vectors = Lists.newArrayList();
          for (VectorWrapper vw : iter) {
            vectors.add(vw.getValueVector());
          }
        }
        final int position = iter.getCurrentPosition();
        if (position %2 == 0 ) {
          assertTrue(checkValues(vectors, position));
        } else {
          assertTrue(checkValues(vectors, position));
        }
        totalRecords++;
      }
      assertEquals(0, iter.cachedBatches().size());
    }
    assertEquals(11112, totalRecords);
    try {
      iter.mark();
      fail();
    } catch (UnsupportedOperationException e) {}
    try {
      iter.reset();
      fail();
    } catch (UnsupportedOperationException e) {}
  }

  @Test
  public void testMarkResetIterator() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    final String planStr = Files.asCharSource(DrillFileUtils.getResourceAsFile("/record/test_recorditerator.json"), Charsets.UTF_8).read();

    final PhysicalPlan plan = reader.readPhysicalPlan(planStr);
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, BitControl.PlanFragment.getDefaultInstance(), connection, registry);
    final List<PhysicalOperator> operatorList = plan.getSortedOperators(false);
    SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) operatorList.iterator().next()));

    RecordBatch singleBatch = exec.getIncoming();
    PhysicalOperator dummyPop = operatorList.iterator().next();
    OpProfileDef def = new OpProfileDef(dummyPop.getOperatorId(), MockSubScanPOP.OPERATOR_TYPE,
        OperatorUtilities.getChildCount(dummyPop));
    OperatorStats stats = exec.getContext().getStats().newOperatorStats(def, exec.getContext().getAllocator());
    RecordIterator iter = new RecordIterator(singleBatch, null, exec.getContext().newOperatorContext(dummyPop, stats), 0, null);
    List<ValueVector> vectors;
    // batche sizes
    // 1, 100, 10, 10000, 1, 1000
    // total = 11112

    // BATCH 1 : 1, starting outerposition: 0
    iter.next();
    assertFalse(iter.finished());
    assertEquals(1, iter.getTotalRecordCount());
    assertEquals(0, iter.getCurrentPosition());
    assertEquals(0, iter.getOuterPosition());
    assertEquals(1, iter.cachedBatches().size());
    vectors = Lists.newArrayList();
    for (VectorWrapper vw : iter) {
      vectors.add(vw.getValueVector());
    }
    // mark at position 0
    iter.mark();
    checkValues(vectors, 0);

    // BATCH 2: 100, starting outerposition: 1
    iter.next();
    assertFalse(iter.finished());
    assertEquals(101, iter.getTotalRecordCount(), 101);
    assertEquals(0, iter.getCurrentPosition());
    assertEquals(100, iter.getInnerRecordCount());
    assertEquals(1, iter.getOuterPosition());
    assertEquals(2, iter.cachedBatches().size());
    for (int i = 0; i < 100; i++) {
      checkValues(vectors, i);
      iter.next();
    }

    // BATCH 3 :10, starting outerposition: 101
    assertFalse(iter.finished());
    assertEquals(111, iter.getTotalRecordCount());
    assertEquals(0, iter.getCurrentPosition());
    assertEquals(10, iter.getInnerRecordCount());
    assertEquals(101, iter.getOuterPosition());
    assertEquals(3, iter.cachedBatches().size());
    for (int i = 0; i < 10; i++) {
      checkValues(vectors, i);
      iter.next();
    }

    // BATCH 4 : 10000, starting outerposition: 111
    assertFalse(iter.finished());
    assertEquals(10111, iter.getTotalRecordCount());
    assertEquals(0, iter.getCurrentPosition(), 0);
    assertEquals(10000, iter.getInnerRecordCount());
    assertEquals(111, iter.getOuterPosition());
    assertEquals(4, iter.cachedBatches().size());
    for (int i = 0; i < 10000; i++) {
      checkValues(vectors, i);
      iter.next();
    }

    // BATCH 5 : 1, starting outerposition: 10111
    assertFalse(iter.finished());
    assertEquals(10112, iter.getTotalRecordCount());
    assertEquals(0, iter.getCurrentPosition());
    assertEquals(1, iter.getInnerRecordCount());
    assertEquals(10111, iter.getOuterPosition());
    assertEquals(5, iter.cachedBatches().size());
    checkValues(vectors, 0);
    iter.next();

    // BATCH 6 : 1000, starting outerposition: 10112
    assertFalse(iter.finished());
    assertEquals(11112, iter.getTotalRecordCount());
    assertEquals(0, iter.getCurrentPosition());
    assertEquals(1000, iter.getInnerRecordCount());
    assertEquals(10112, iter.getOuterPosition());
    assertEquals(6, iter.cachedBatches().size());
    for (int i = 0; i < 1000; i++) {
      checkValues(vectors, i);
      iter.next();
    }
    assertTrue(iter.finished());
    assertEquals(6, iter.cachedBatches().size());

    // back to batch 1
    iter.reset();
    assertFalse(iter.finished());
    assertEquals(iter.getTotalRecordCount(), 11112);
    assertEquals(6, iter.cachedBatches().size());
    assertEquals(iter.getCurrentPosition(), 0);
    assertEquals(1, iter.getInnerRecordCount());
    checkValues(vectors, 0);

    iter.next();
    // mark start of batch 2
    iter.mark();
    assertFalse(iter.finished());
    assertEquals(iter.getTotalRecordCount(), 11112);
    assertEquals(5, iter.cachedBatches().size());
    assertEquals(iter.getCurrentPosition(), 0);
    assertEquals(100, iter.getInnerRecordCount());
    for (int i = 0; i < 100; i++) {
      iter.next();
    }

    // mark start of batch 3
    iter.mark();
    assertFalse(iter.finished());
    assertEquals(iter.getTotalRecordCount(), 11112);
    assertEquals(4, iter.cachedBatches().size());
    assertEquals(iter.getCurrentPosition(), 0);
    assertEquals(10, iter.getInnerRecordCount());
    for (int i = 0; i < 10; i++) {
      iter.next();
    }

    // jump into middle of largest batch #4.
    for (int i = 0; i < 5000; i++) {
      iter.next();
    }
    assertEquals(4, iter.cachedBatches().size());
    iter.mark();
    assertEquals(3, iter.cachedBatches().size());
    for (int i = 0; i < 5000; i++) {
      iter.next();
    }

    // mark start of batch 5
    iter.mark();
    assertFalse(iter.finished());
    assertEquals(iter.getTotalRecordCount(), 11112);
    assertEquals(2, iter.cachedBatches().size());
    assertEquals(iter.getCurrentPosition(), 0);
    assertEquals(1, iter.getInnerRecordCount());

    // move to last batch
    iter.next();
    // skip to the middle of last batch
    for (int i = 0; i < 500; i++) {
      iter.next();
    }
    checkValues(vectors, 499);
    checkValues(vectors, 500);
    iter.reset();
    checkValues(vectors, 0);
    assertFalse(iter.finished());
    assertEquals(iter.getTotalRecordCount(), 11112);
    assertEquals(2, iter.cachedBatches().size());
    assertEquals(iter.getCurrentPosition(), 0);
    assertEquals(1, iter.getInnerRecordCount());
    // move to last batch
    iter.next();
    assertEquals(0, iter.getCurrentPosition());
    for (int i = 0; i < 500; i++) {
      iter.next();
    }
    // This should free 5th batch.
    iter.mark();
    assertFalse(iter.finished());
    assertEquals(iter.getTotalRecordCount(), 11112);
    assertEquals(1, iter.cachedBatches().size());
    assertEquals(500, iter.getCurrentPosition());
    assertEquals(1000, iter.getInnerRecordCount());
    // go to the end of iterator
    for (int i = 0; i < 500; i++) {
      iter.next();
    }
    assertTrue(iter.finished());
    iter.reset();
    assertFalse(iter.finished());
    assertEquals(iter.getTotalRecordCount(), 11112);
    assertEquals(1, iter.cachedBatches().size());
    assertEquals(500, iter.getCurrentPosition());
    assertEquals(1000, iter.getInnerRecordCount());
    iter.close();
    assertEquals(0, iter.cachedBatches().size());
  }

  private static boolean checkValues(List<ValueVector> vectors, int position) {
    boolean result = true;
    final int expected = (position % 2 == 0)? Integer.MIN_VALUE : Integer.MAX_VALUE;
    for (ValueVector vv : vectors) {
      final Object o = vv.getAccessor().getObject(position);
      if (o instanceof Integer) {
        final Integer v = (Integer)o;
        result &= (v == expected);
      } else {
        logger.error("Found wrong type {} at position {}", o.getClass(), position);
        result = false;
        break;
      }
    }
    return result;
  }
}
