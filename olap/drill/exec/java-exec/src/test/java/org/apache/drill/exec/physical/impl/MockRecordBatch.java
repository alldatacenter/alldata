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
package org.apache.drill.exec.physical.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.IndirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class MockRecordBatch implements CloseableRecordBatch {

  // These resources are owned by this RecordBatch
  protected VectorContainer container;
  protected SelectionVector2 sv2;
  protected SelectionVector4 sv4;
  private int currentContainerIndex;
  private int currentOutcomeIndex;
  private boolean isDone;
  private boolean limitWithUnnest;

  // All the below resources are owned by caller
  private final List<RowSet> rowSets;
  private final List<IterOutcome> allOutcomes;
  private final FragmentContext context;
  protected final OperatorContext oContext;
  protected final BufferAllocator allocator;

  private MockRecordBatch(@NotNull FragmentContext context,
                          @Nullable OperatorContext oContext,
                          @NotNull List<RowSet> testRowSets,
                          @NotNull List<IterOutcome> iterOutcomes,
                          @NotNull BatchSchema schema,
                          boolean dummy) {
    Preconditions.checkNotNull(testRowSets);
    Preconditions.checkNotNull(iterOutcomes);
    Preconditions.checkNotNull(schema);

    this.context = context;
    this.oContext = oContext;
    this.rowSets = testRowSets;
    this.allocator = context.getAllocator();
    this.container = new VectorContainer(allocator, schema);
    this.allOutcomes = iterOutcomes;
  }

  public MockRecordBatch(@Nullable FragmentContext context,
                         @Nullable OperatorContext oContext,
                         @NotNull List<VectorContainer> testContainers,
                         @NotNull List<IterOutcome> iterOutcomes,
                         BatchSchema schema) {
    this(context,
         oContext,
         testContainers.stream().
           map(container -> DirectRowSet.fromContainer(container)).
           collect(Collectors.toList()),
         iterOutcomes,
         schema,
         true);
  }

  @Deprecated
  public MockRecordBatch(@Nullable FragmentContext context,
                         @Nullable OperatorContext oContext,
                         @NotNull List<VectorContainer> testContainers,
                         @NotNull List<IterOutcome> iterOutcomes,
                         @NotNull List<SelectionVector2> selectionVector2s,
                         BatchSchema schema) {
    this(context,
      oContext,
      new Supplier<List<RowSet>>() {
        @Override
        public List<RowSet> get() {
          List<RowSet> rowSets = new ArrayList<>();

          for (int index = 0; index < testContainers.size(); index++) {
            if (index >= selectionVector2s.size()) {
              rowSets.add(IndirectRowSet.fromContainer(testContainers.get(index)));
            } else {
              rowSets.add(IndirectRowSet.fromSv2(testContainers.get(index), selectionVector2s.get(index)));
            }
          }
          return rowSets;
        }
      }.get(),
      iterOutcomes,
      schema,
      true);
  }

  @Override
  public void close() {
    container.clear();
    container.setEmpty();
    currentContainerIndex = 0;
    currentOutcomeIndex = 0;
    if (sv2 != null) {
      sv2.clear();
    }
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    return sv2;
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    return sv4;
  }

  @Override
  public FragmentContext getContext() {
    return context;
  }

  @Override
  public BatchSchema getSchema() {
    return container.getSchema();
  }

  @Override
  public int getRecordCount() {
    return (sv2 == null) ? container.getRecordCount() : sv2.getCount();
  }

  @Override
  public void cancel() {
    if (!limitWithUnnest) {
      isDone = true;
    }
  }

  @Override
  public VectorContainer getOutgoingContainer() {
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

    if (isDone) {
      return IterOutcome.NONE;
    }

    IterOutcome currentOutcome;

    if (currentContainerIndex < rowSets.size()) {
      RowSet rowSet = rowSets.get(currentContainerIndex);
      VectorContainer input = rowSet.container();
      // We need to do this since the downstream operator expects vector reference to be same
      // after first next call in cases when schema is not changed
      BatchSchema inputSchema = input.getSchema();
      if (!container.getSchema().isEquivalent(inputSchema)) {
        container.clear();
        container = new VectorContainer(allocator, inputSchema);
      }

      switch (rowSet.indirectionType()) {
        case NONE:
        case TWO_BYTE:
          if (input.hasRecordCount()) { // in case special test of uninitialized input container
            container.transferIn(input);
          } else {
            // Not normally a valid condition, supported here just for testing
            container.rawTransferIn(input);
          }
          SelectionVector2 inputSv2 = ((RowSet.SingleRowSet) rowSet).getSv2();

          if (sv2 != null) {
            // Operators assume that new values for an Sv2 are transferred in.
            sv2.allocateNewSafe(inputSv2.getCount());
            for (int i=0; i < inputSv2.getCount(); ++i) {
              sv2.setIndex(i, inputSv2.getIndex(i));
            }
            sv2.setRecordCount(inputSv2.getCount());
          } else {
            sv2 = inputSv2;
          }

          break;
        case FOUR_BYTE:
          // TODO find a clean way to transfer in for this case.
          container.clear();
          container = input;
          sv4 = ((RowSet.HyperRowSet) rowSet).getSv4();
          break;
        default:
          throw new UnsupportedOperationException();
      }
    }

    if (currentOutcomeIndex < allOutcomes.size()) {
      currentOutcome = allOutcomes.get(currentOutcomeIndex);
      ++currentOutcomeIndex;
    } else {
      currentOutcome = IterOutcome.NONE;
    }

    switch (currentOutcome) {
      case OK:
      case OK_NEW_SCHEMA:
      case EMIT:
        ++currentContainerIndex;
        return currentOutcome;
      case NONE:
        isDone = true;
      case NOT_YET:
        container.setRecordCount(0);
        return currentOutcome;
      default:
        throw new UnsupportedOperationException("This state is not supported");
    }
  }

  @Override
  public WritableBatch getWritableBatch() {
    throw new UnsupportedOperationException("MockRecordBatch doesn't support gettingWritableBatch yet");
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return container.iterator();
  }

  @Override
  public VectorContainer getContainer() { return container; }

  public boolean isCompleted() {
    return isDone;
  }

  public void useUnnestKillHandlingForLimit(boolean limitWithUnnest) {
    this.limitWithUnnest = limitWithUnnest;
  }

  @Override
  public void dump() { }

  public static class Builder {
    private final List<RowSet> rowSets = new ArrayList<>();
    private final List<IterOutcome> iterOutcomes = new ArrayList<>();

    private BatchSchema batchSchema;
    private OperatorContext oContext;

    public Builder() {
    }

    private Builder sendData(RowSet rowSet, IterOutcome outcome) {
      Preconditions.checkState(batchSchema == null);
      rowSets.add(rowSet);
      iterOutcomes.add(outcome);
      return this;
    }

    public Builder sendData(RowSet rowSet) {
      IterOutcome outcome = rowSets.isEmpty()? IterOutcome.OK_NEW_SCHEMA: IterOutcome.OK;
      return sendData(rowSet, outcome);
    }

    public Builder sendDataWithNewSchema(RowSet rowSet) {
      return sendData(rowSet, IterOutcome.OK_NEW_SCHEMA);
    }

    public Builder sendDataAndEmit(RowSet rowSet) {
      return sendData(rowSet, IterOutcome.EMIT);
    }

    public Builder terminateWithError(IterOutcome errorOutcome) {
      iterOutcomes.add(errorOutcome);
      return this;
    }

    public Builder setSchema(BatchSchema batchSchema) {
      Preconditions.checkState(!rowSets.isEmpty());
      this.batchSchema = Preconditions.checkNotNull(batchSchema);
      return this;
    }

    public Builder withOperatorContext(OperatorContext oContext) {
      this.oContext = Preconditions.checkNotNull(oContext);
      return this;
    }

    public MockRecordBatch build(FragmentContext context) {
      BatchSchema tempSchema = batchSchema;

      if (tempSchema == null && !rowSets.isEmpty()) {
        tempSchema = rowSets.get(0).batchSchema();
      }

      return new MockRecordBatch(context,
        oContext,
        rowSets,
        iterOutcomes,
        tempSchema,
        true);
    }
  }
}
