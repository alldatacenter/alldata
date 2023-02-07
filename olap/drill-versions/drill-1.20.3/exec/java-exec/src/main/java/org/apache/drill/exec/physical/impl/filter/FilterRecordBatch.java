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
package org.apache.drill.exec.physical.impl.filter;

import java.util.List;

import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.Filter;
import org.apache.drill.exec.record.AbstractSingleRecordBatch;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterRecordBatch extends AbstractSingleRecordBatch<Filter> {
  private static final Logger logger = LoggerFactory.getLogger(FilterRecordBatch.class);

  private SelectionVector2 sv2;
  private SelectionVector4 sv4;
  private Filterer filter;

  public FilterRecordBatch(Filter pop, RecordBatch incoming, FragmentContext context) throws OutOfMemoryException {
    super(pop, context, incoming);
  }

  @Override
  public FragmentContext getContext() {
    return context;
  }

  @Override
  public int getRecordCount() {
    return sv2 != null ? sv2.getCount() : sv4.getCount();
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
  protected IterOutcome doWork() {
    try {
      container.zeroVectors();
      int recordCount = incoming.getRecordCount();
      filter.filterBatch(recordCount);
      // The container needs the actual number of values in
      // its contained vectors (not the filtered count)
      // Not sure the SV4 case is actually supported...
      container.setRecordCount(
          sv2 != null ? sv2.getBatchActualRecordCount() : recordCount);
      return getFinalOutcome(false);
    } catch (SchemaChangeException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  @Override
  public void close() {
    clearSv();
    super.close();
  }

  private void clearSv() {
    if (sv2 != null) {
      sv2.clear();
    }
    if (sv4 != null) {
      sv4.clear();
    }
  }

  @Override
  protected boolean setupNewSchema()  {
    clearSv();

    switch (incoming.getSchema().getSelectionVectorMode()) {
      case NONE:
        if (sv2 == null) {
          sv2 = new SelectionVector2(oContext.getAllocator());
        }
        filter = generateSV2Filterer();
        break;
      case TWO_BYTE:
        sv2 = new SelectionVector2(oContext.getAllocator());
        filter = generateSV2Filterer();
        break;
      case FOUR_BYTE:
        /*
         * Filter does not support SV4 handling. There are couple of minor issues in the
         * logic that handles SV4 + filter should always be pushed beyond sort so disabling
         * it in FilterPrel.
         */
      default:
        throw new UnsupportedOperationException();
    }

    if (container.isSchemaChanged()) {
      container.buildSchema(SelectionVectorMode.TWO_BYTE);
      return true;
    } else {
      return false;
    }
  }

  protected Filterer generateSV4Filterer() throws SchemaChangeException {
    final ErrorCollector collector = new ErrorCollectorImpl();
    final List<TransferPair> transfers = Lists.newArrayList();
    final ClassGenerator<Filterer> cg = CodeGenerator.getRoot(Filterer.TEMPLATE_DEFINITION4, context.getOptions());

    final LogicalExpression expr = ExpressionTreeMaterializer.materialize(popConfig.getExpr(), incoming, collector, context.getFunctionRegistry());
    if (collector.hasErrors()) {
      throw new SchemaChangeException(String.format("Failure while trying to materialize incoming schema.  Errors:\n %s.", collector.toErrorString()));
    }

    cg.addExpr(new ReturnValueExpression(expr), ClassGenerator.BlkCreateMode.FALSE);

    for (final VectorWrapper<?> vw : incoming) {
      for (final ValueVector vv : vw.getValueVectors()) {
        final TransferPair pair = vv.getTransferPair(oContext.getAllocator());
        container.add(pair.getTo());
        transfers.add(pair);
      }
    }

    // allocate outgoing sv4
    container.buildSchema(SelectionVectorMode.FOUR_BYTE);

    final TransferPair[] tx = transfers.toArray(new TransferPair[transfers.size()]);
    final Filterer filter = context.getImplementationClass(cg);
    filter.setup(context, incoming, this, tx);
    return filter;
  }

  protected Filterer generateSV2Filterer() {
    final ErrorCollector collector = new ErrorCollectorImpl();
    final List<TransferPair> transfers = Lists.newArrayList();
    final ClassGenerator<Filterer> cg = CodeGenerator.getRoot(Filterer.TEMPLATE_DEFINITION2, context.getOptions());
    cg.getCodeGenerator().plainJavaCapable(true);
    // Uncomment the following line to enable saving generated code file for debugging
    // cg.getCodeGenerator().saveCodeForDebugging(true);

    final LogicalExpression expr = ExpressionTreeMaterializer.materialize(popConfig.getExpr(), incoming, collector,
            context.getFunctionRegistry(), false, unionTypeEnabled);
    collector.reportErrors(logger);

    cg.addExpr(new ReturnValueExpression(expr), ClassGenerator.BlkCreateMode.FALSE);

    for (final VectorWrapper<?> v : incoming) {
      final TransferPair pair = v.getValueVector().makeTransferPair(container.addOrGet(v.getField(), callBack));
      transfers.add(pair);
    }

    final TransferPair[] tx = transfers.toArray(new TransferPair[transfers.size()]);
    CodeGenerator<Filterer> codeGen = cg.getCodeGenerator();
    codeGen.plainJavaCapable(true);
    final Filterer filter = context.getImplementationClass(codeGen);
    try {
      filter.setup(context, incoming, this, tx);
    } catch (SchemaChangeException e) {
      throw schemaChangeException(e, logger);
    }
    return filter;
  }

  @Override
  public void dump() {
    logger.error("FilterRecordBatch[container={}, selectionVector2={}, filter={}, popConfig={}]", container, sv2, filter, popConfig);
  }
}
