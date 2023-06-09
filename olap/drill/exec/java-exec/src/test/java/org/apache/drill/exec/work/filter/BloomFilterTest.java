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
package org.apache.drill.exec.work.filter;

import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.util.function.CheckedFunction;
import org.apache.drill.exec.exception.ClassTransformationException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ValueVectorReadExpression;
import org.apache.drill.exec.expr.fn.impl.ValueVectorHashHelper;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.test.SubOperatorTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

public class BloomFilterTest extends SubOperatorTest {

  private static class TestRecordBatch implements RecordBatch {
    private final VectorContainer container;

    public TestRecordBatch(VectorContainer container) {
      this.container = container;
    }

    @Override
    public int getRecordCount() {
      return 0;
    }

    @Override
    public SelectionVector2 getSelectionVector2() {
      return null;
    }

    @Override
    public SelectionVector4 getSelectionVector4() {
      return null;
    }

    @Override
    public FragmentContext getContext() {
      return null;
    }

    @Override
    public BatchSchema getSchema() {
      return null;
    }

    @Override
    public void cancel() {
    }

    @Override
    public VectorContainer getOutgoingContainer() {
      return null;
    }

    @Override
    public VectorContainer getContainer() {
      return null;
    }

    @Override
    public TypedFieldId getValueVectorId(SchemaPath path) {
      return container.getValueVectorId(path);
    }

    @Override
    public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... ids) {
      return container.getValueAccessorById(clazz, ids);
    }

    @Override
    public IterOutcome next() {
      return null;
    }

    @Override
    public WritableBatch getWritableBatch() {
      return null;
    }

    @Override
    public Iterator<VectorWrapper<?>> iterator() {
      return null;
    }

    @Override
    public void dump() { }
  }

  @Test
  public void testNotExist() throws Exception {
    RowSet.SingleRowSet probeRowSet = fixture.rowSetBuilder(getTestSchema())
        .addRow("f")
        .build();

    checkBloomFilterResult(probeRowSet, BloomFilterTest::getSimpleBloomFilter, false);
  }

  @Test
  public void testExist() throws Exception {
    RowSet.SingleRowSet probeRowSet = fixture.rowSetBuilder(getTestSchema())
        .addRow("a")
        .build();

    checkBloomFilterResult(probeRowSet, BloomFilterTest::getSimpleBloomFilter, true);
  }

  @Test
  public void testMerged() throws Exception {
    RowSet.SingleRowSet probeRowSet = fixture.rowSetBuilder(getTestSchema())
        .addRow("a")
        .build();

    checkBloomFilterResult(probeRowSet, this::getDisjunctionBloomFilter, true);
  }

  private BloomFilter getDisjunctionBloomFilter(ValueVectorHashHelper.Hash64 hash64) throws SchemaChangeException {
    int numBytes = BloomFilter.optimalNumOfBytes(3, 0.03);
    BloomFilter bloomFilter = new BloomFilter(numBytes, fixture.allocator());
    int valueCount = 3;
    for (int i = 0; i < valueCount; i++) {
      long hashCode = hash64.hash64Code(i, 0, 0);
      bloomFilter.insert(hashCode);
    }

    BloomFilter disjunctionBloomFilter = getSimpleBloomFilter(hash64);
    disjunctionBloomFilter.or(bloomFilter);

    bloomFilter.getContent().close();

    return disjunctionBloomFilter;
  }

  private static BloomFilter getSimpleBloomFilter(ValueVectorHashHelper.Hash64 hash64) throws SchemaChangeException {
    int numBytes = BloomFilter.optimalNumOfBytes(3, 0.03);

    BloomFilter bloomFilter = new BloomFilter(numBytes, fixture.allocator());

    int valueCount = 3;
    for (int i = 0; i < valueCount; i++) {
      long hashCode = hash64.hash64Code(i, 0, 0);
      bloomFilter.insert(hashCode);
    }
    return bloomFilter;
  }

  private void checkBloomFilterResult(RowSet.SingleRowSet probeRowSet,
      CheckedFunction<ValueVectorHashHelper.Hash64, BloomFilter, SchemaChangeException> bloomFilterProvider,
      boolean matches) throws ClassTransformationException, IOException, SchemaChangeException {
    try (FragmentContext context = fixture.getFragmentContext()) {
      // create build side batch
      RowSet.SingleRowSet batchRowSet = fixture.rowSetBuilder(getTestSchema())
          .addRow("a")
          .addRow("b")
          .addRow("c")
          .build();

      // create build side Hash64
      ValueVectorHashHelper.Hash64 hash64 = getHash64(context, batchRowSet);

      // construct BloomFilter
      BloomFilter bloomFilter = bloomFilterProvider.apply(hash64);

      // create probe side Hash64
      ValueVectorHashHelper.Hash64 probeHash64 = getHash64(context, probeRowSet);

      long hashCode = probeHash64.hash64Code(0, 0, 0);

      Assert.assertEquals(matches, bloomFilter.find(hashCode));

      bloomFilter.getContent().close();
      batchRowSet.clear();
      probeRowSet.clear();
    }
  }

  private static TupleMetadata getTestSchema() {
    return new SchemaBuilder()
        .add("a", TypeProtos.MinorType.VARCHAR)
        .build();
  }

  private static ValueVectorHashHelper.Hash64 getHash64(FragmentContext context,
      RowSet.SingleRowSet probeRowSet) throws ClassTransformationException, IOException, SchemaChangeException {

    RecordBatch probeRecordBatch = new TestRecordBatch(probeRowSet.container());
    TypedFieldId probeFieldId = probeRecordBatch.getValueVectorId(SchemaPath.getSimplePath("a"));
    ValueVectorReadExpression probExp = new ValueVectorReadExpression(probeFieldId);
    LogicalExpression[] probExpressions = new LogicalExpression[1];
    probExpressions[0] = probExp;
    TypedFieldId[] probeFieldIds = new TypedFieldId[1];
    probeFieldIds[0] = probeFieldId;
    ValueVectorHashHelper probeValueVectorHashHelper = new ValueVectorHashHelper(probeRecordBatch, context);
    return probeValueVectorHashHelper.getHash64(probExpressions, probeFieldIds);
  }
}
