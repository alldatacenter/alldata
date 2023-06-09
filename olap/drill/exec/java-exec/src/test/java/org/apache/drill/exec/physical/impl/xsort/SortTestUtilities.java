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
package org.apache.drill.exec.physical.impl.xsort;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.config.Sort;
import org.apache.drill.exec.physical.impl.xsort.PriorityQueueCopierWrapper.BatchMerger;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.test.OperatorFixture;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class SortTestUtilities {

  private SortTestUtilities() { }

  public static TupleMetadata makeSchema(MinorType type, boolean nullable) {
    return new SchemaBuilder()
        .add("key", type, nullable ? DataMode.OPTIONAL : DataMode.REQUIRED)
        .add("value", MinorType.VARCHAR)
        .buildSchema();
  }

  public static TupleMetadata nonNullSchema() {
    return makeSchema(MinorType.INT, false);
  }

  public static TupleMetadata nullableSchema() {
    return makeSchema(MinorType.INT, true);
  }

  public static Sort makeCopierConfig(String sortOrder, String nullOrder) {
    FieldReference expr = FieldReference.getWithQuotedRef("key");
    Ordering ordering = new Ordering(sortOrder, expr, nullOrder);
    return new Sort(null, Lists.newArrayList(ordering), false);
  }

  public static class CopierTester {
    List<SingleRowSet> rowSets = new ArrayList<>();
    List<SingleRowSet> expected = new ArrayList<>();
    String sortOrder = Ordering.ORDER_ASC;
    String nullOrder = Ordering.NULLS_UNSPECIFIED;
    private final OperatorFixture fixture;

    public CopierTester(OperatorFixture fixture) {
      this.fixture = fixture;
    }

    public void addInput(SingleRowSet input) {
      rowSets.add(input);
    }

    public void addOutput(SingleRowSet output) {
      expected.add(output);
    }

    public void run() throws Exception {
      Sort popConfig = SortTestUtilities.makeCopierConfig(sortOrder, nullOrder);
      OperatorContext opContext = fixture.newOperatorContext(popConfig);
      PriorityQueueCopierWrapper copier = new PriorityQueueCopierWrapper(opContext);
      try {
        List<BatchGroup> batches = new ArrayList<>();
        TupleMetadata schema = null;
        for (SingleRowSet rowSet : rowSets) {
          batches.add(new InputBatch(rowSet.container(), rowSet.getSv2(),
                      fixture.allocator(), rowSet.size()));
          if (schema == null) {
            schema = rowSet.schema();
          }
        }
        int rowCount = outputRowCount();
        VectorContainer dest = new VectorContainer();
        BatchMerger merger = copier.startMerge(new BatchSchema(SelectionVectorMode.NONE, schema.toFieldList()),
                                               batches, dest, rowCount, null);

        verifyResults(merger, dest);
        dest.clear();
        merger.close();
      } finally {
        opContext.close();
      }
    }

    public int outputRowCount() {
      if (! expected.isEmpty()) {
        return expected.get(0).rowCount();
      }
      return 10;
    }

    protected void verifyResults(BatchMerger merger, VectorContainer dest) {
      for (RowSet expectedSet : expected) {
        assertTrue(merger.next());
        RowSet rowSet = DirectRowSet.fromContainer(dest);
        RowSetUtilities.verify(expectedSet, rowSet);
      }
      assertFalse(merger.next());
    }
  }

}
