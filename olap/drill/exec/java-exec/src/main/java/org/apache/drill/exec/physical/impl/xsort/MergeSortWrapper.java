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

import java.util.List;

import org.apache.calcite.rel.RelFieldCollation.Direction;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.exec.compile.sig.MappingSet;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.config.Sort;
import org.apache.drill.exec.physical.impl.sort.RecordBatchData;
import org.apache.drill.exec.physical.impl.sort.SortRecordBatchBuilder;
import org.apache.drill.exec.physical.impl.xsort.SortImpl.SortResults;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.HyperVectorWrapper;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;

import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;

/**
 * Wrapper around the "MSorter" (in memory merge sorter). As batches have
 * arrived to the sort, they have been individually sorted and buffered
 * in memory. At the completion of the sort, we detect that no batches
 * were spilled to disk. In this case, we can merge the in-memory batches
 * using an efficient memory-based approach implemented here.
 * <p>
 * Since all batches are in memory, we don't want to use the usual merge
 * algorithm as that makes a copy of the original batches (which were read
 * from a spill file) to produce an output batch. Instead, we want to use
 * the in-memory batches as-is. To do this, we use a selection vector 4
 * (SV4) as a global index into the collection of batches. The SV4 uses
 * the upper two bytes as the batch index, and the lower two as an offset
 * of a record within the batch.
 * <p>
 * The merger ("M Sorter") populates the SV4 by scanning the set of
 * in-memory batches, searching for the one with the lowest value of the
 * sort key. The batch number and offset are placed into the SV4. The process
 * continues until all records from all batches have an entry in the SV4.
 * <p>
 * The actual implementation uses an iterative merge to perform the above
 * efficiently.
 * <p>
 * A sort can only do a single merge. So, we do not attempt to share the
 * generated class; we just generate it internally and discard it at
 * completion of the merge.
 * <p>
 * The merge sorter only makes sense when we have at least one row. The
 * caller must handle the special case of no rows.
 */

public class MergeSortWrapper extends BaseSortWrapper implements SortResults {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MergeSortWrapper.class);

  public enum State { FIRST, BODY, EOF }

  private SortRecordBatchBuilder builder;
  private MSorter mSorter;
  private SelectionVector4 sv4;
  private int batchCount;
  private State state = State.FIRST;
  private final VectorContainer destContainer;

  public MergeSortWrapper(OperatorContext opContext, VectorContainer destContainer) {
    super(opContext);
    this.destContainer = destContainer;
  }

  /**
   * Merge the set of in-memory batches to produce a single logical output in the given
   * destination container, indexed by an SV4.
   *
   * @param batchGroups the complete set of in-memory batches
   * @param outputBatchSize output batch size for in-memory merge
   * @return the sv4 for this operator
   */

  public void merge(List<InputBatch> batchGroups, int outputBatchSize) {

    // Add the buffered batches to a collection that MSorter can use.
    // The builder takes ownership of the batches and will release them if
    // an error occurs.

    builder = new SortRecordBatchBuilder(context.getAllocator());
    for (InputBatch group : batchGroups) {
      RecordBatchData rbd = new RecordBatchData(group.getContainer(), context.getAllocator());
      rbd.setSv2(group.getSv2());
      builder.add(rbd);
    }
    batchGroups.clear();

    // Generate the msorter.

    try {
      builder.build(destContainer);
      sv4 = builder.getSv4();
      Sort popConfig = context.getOperatorDefn();
      mSorter = createNewMSorter(popConfig.getOrderings(), MAIN_MAPPING, LEFT_MAPPING, RIGHT_MAPPING);
      mSorter.setup(context.getFragmentContext(), context.getAllocator(), sv4, destContainer, sv4.getCount(), outputBatchSize);
    } catch (SchemaChangeException e) {
      throw UserException.unsupportedError(e)
            .message("Unexpected schema change - likely code error.")
            .build(logger);
    }

    // For testing memory-leaks, inject exception after mSorter finishes setup
    context.injectUnchecked(ExternalSortBatch.INTERRUPTION_AFTER_SETUP);
    mSorter.sort();

    // For testing memory-leak purpose, inject exception after mSorter finishes sorting
    context.injectUnchecked(ExternalSortBatch.INTERRUPTION_AFTER_SORT);
    sv4 = mSorter.getSV4();

//    destContainer.buildSchema(SelectionVectorMode.FOUR_BYTE);
  }

  private MSorter createNewMSorter(List<Ordering> orderings, MappingSet mainMapping, MappingSet leftMapping, MappingSet rightMapping) {
    CodeGenerator<MSorter> cg = CodeGenerator.get(MSorter.TEMPLATE_DEFINITION, context.getFragmentContext().getOptions());
    cg.plainJavaCapable(true);

    // Uncomment out this line to debug the generated code.
//    cg.saveCodeForDebugging(true);
    ClassGenerator<MSorter> g = cg.getRoot();
    g.setMappingSet(mainMapping);

    for (Ordering od : orderings) {
      // first, we rewrite the evaluation stack for each side of the comparison.
      ErrorCollector collector = new ErrorCollectorImpl();
      final LogicalExpression expr = ExpressionTreeMaterializer.materialize(od.getExpr(), destContainer, collector,
          context.getFragmentContext().getFunctionRegistry());
      if (collector.hasErrors()) {
        throw UserException.unsupportedError()
              .message("Failure while materializing expression. " + collector.toErrorString())
              .build(logger);
      }
      g.setMappingSet(leftMapping);
      HoldingContainer left = g.addExpr(expr, ClassGenerator.BlkCreateMode.FALSE);
      g.setMappingSet(rightMapping);
      HoldingContainer right = g.addExpr(expr, ClassGenerator.BlkCreateMode.FALSE);
      g.setMappingSet(mainMapping);

      // next we wrap the two comparison sides and add the expression block for the comparison.
      LogicalExpression fh =
          FunctionGenerationHelper.getOrderingComparator(od.nullsSortHigh(), left, right,
                                                         context.getFragmentContext().getFunctionRegistry());
      HoldingContainer out = g.addExpr(fh, ClassGenerator.BlkCreateMode.FALSE);
      JConditional jc = g.getEvalBlock()._if(out.getValue().ne(JExpr.lit(0)));

      if (od.getDirection() == Direction.ASCENDING) {
        jc._then()._return(out.getValue());
      }else{
        jc._then()._return(out.getValue().minus());
      }
      g.rotateBlock();
    }

    g.rotateBlock();
    g.getEvalBlock()._return(JExpr.lit(0));

    return getInstance(cg, logger);
  }

  /**
   * The SV4 provides a built-in iterator that returns a virtual set of record
   * batches so that the downstream operator need not consume the entire set
   * of accumulated batches in a single step.
   */

  @Override
  public boolean next() {
    switch (state) {
    case BODY:
      if (! sv4.next()) {
        state = State.EOF;
        return false;
      }
      return true;
    case EOF:
      return false;
    case FIRST:
      state = State.BODY;
      return true;
    default:
      throw new IllegalStateException( "Unexpected case: " + state );
    }
  }

  @Override
  public void close() {
    RuntimeException ex = null;
    try {
      if (builder != null) {
        builder.clear();
        builder.close();
        builder = null;
      }
    } catch (RuntimeException e) {
      ex = e;
    }
    try {
      if (mSorter != null) {
        mSorter.clear();
        mSorter = null;
      }
    } catch (RuntimeException e) {
      ex = (ex == null) ? e : ex;
    }

    // Sv4 is cleared by the builder, above.

    if (ex != null) {
      throw ex;
    }
  }

  @Override
  public int getBatchCount() { return batchCount; }

  @Override
  public int getRecordCount() { return sv4.getCount(); }

  @Override
  public SelectionVector4 getSv4() { return sv4; }

  @Override
  public void updateOutputContainer(VectorContainer container, SelectionVector4 sv4,
                                    RecordBatch.IterOutcome outcome, BatchSchema schema) {

    final VectorContainer inputDataContainer = getContainer();
    // First output batch of current schema, populate container with ValueVectors
    if (container.getNumberOfColumns() == 0) {
      for (VectorWrapper<?> w : inputDataContainer) {
        container.add(w.getValueVectors());
      }
      container.buildSchema(BatchSchema.SelectionVectorMode.FOUR_BYTE);
    } else {
      int index = 0;
      for (VectorWrapper<?> w : inputDataContainer) {
        HyperVectorWrapper<?> wrapper = (HyperVectorWrapper<?>) container.getValueVector(index++);
        wrapper.updateVectorList(w.getValueVectors());
      }
    }
    sv4.copy(getSv4());
    container.setRecordCount(getRecordCount());
  }

  @Override
  public SelectionVector2 getSv2() { return null; }

  @Override
  public VectorContainer getContainer() { return destContainer; }
}
