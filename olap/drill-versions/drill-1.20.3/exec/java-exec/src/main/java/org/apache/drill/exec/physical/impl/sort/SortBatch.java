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
package org.apache.drill.exec.physical.impl.sort;

import java.util.List;

import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.exec.compile.sig.MappingSet;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.Sort;
import org.apache.drill.exec.record.AbstractRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.calcite.rel.RelFieldCollation.Direction;

import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;

public class SortBatch extends AbstractRecordBatch<Sort> {
  private static final Logger logger = LoggerFactory.getLogger(SortBatch.class);

  private final MappingSet mainMapping = new MappingSet((String) null, null,
            ClassGenerator.DEFAULT_CONSTANT_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);
  private final MappingSet leftMapping = new MappingSet("leftIndex", null,
            ClassGenerator.DEFAULT_CONSTANT_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);
  private final MappingSet rightMapping = new MappingSet("rightIndex", null,
            ClassGenerator.DEFAULT_CONSTANT_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);

  private final RecordBatch incoming;
  private final SortRecordBatchBuilder builder;
  private Sorter sorter;
  private BatchSchema schema;

  public SortBatch(Sort popConfig, FragmentContext context, RecordBatch incoming) throws OutOfMemoryException {
    super(popConfig, context);
    this.incoming = incoming;
    this.builder = new SortRecordBatchBuilder(oContext.getAllocator());
  }

  @Override
  public int getRecordCount() {
    return builder.getSv4().getCount();
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    return builder.getSv4();
  }

  @Override
  public void close() {
    builder.clear();
    builder.close();
    super.close();
  }

  @Override
  public IterOutcome innerNext() {
    if (schema != null) {
      if (getSelectionVector4().next()) {
        return IterOutcome.OK;
      }

      return IterOutcome.NONE;
    }

    outer: while (true) {
      IterOutcome upstream = incoming.next();
      switch (upstream) {
      case NONE:
        break outer;
      case NOT_YET:
        throw new UnsupportedOperationException();
      case OK_NEW_SCHEMA:
        // only change in the case that the schema truly changes.  Artificial schema changes are ignored.
        if (!incoming.getSchema().equals(schema)) {
          if (schema != null) {
            throw new UnsupportedOperationException("Sort doesn't currently support sorts with changing schemas.");
          }
          schema = incoming.getSchema();
        }
        // fall through.
      case OK:
        if (!builder.add(incoming)) {
          throw new UnsupportedOperationException("Sort doesn't currently support doing an external sort.");
        }
        break;
      default:
        throw new UnsupportedOperationException();
      }
    }

    if (schema == null || builder.isEmpty()) {
      // builder may be null at this point if the first incoming batch is empty
      return IterOutcome.NONE;
    }

    builder.build(container);
    sorter = createNewSorter();
    try {
      sorter.setup(context, getSelectionVector4(), this.container);
    } catch (SchemaChangeException e) {
      throw schemaChangeException(e, logger);
    }
    sorter.sort(getSelectionVector4(), this.container);

    return IterOutcome.OK_NEW_SCHEMA;
  }

  private Sorter createNewSorter() {
    return createNewSorter(this.context, this.popConfig.getOrderings(), this, mainMapping, leftMapping, rightMapping);
  }

  public static Sorter createNewSorter(FragmentContext context, List<Ordering> orderings, VectorAccessible batch) {
    final MappingSet mainMapping = new MappingSet( (String) null, null, ClassGenerator.DEFAULT_CONSTANT_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);
    final MappingSet leftMapping = new MappingSet("leftIndex", null, ClassGenerator.DEFAULT_CONSTANT_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);
    final MappingSet rightMapping = new MappingSet("rightIndex", null, ClassGenerator.DEFAULT_CONSTANT_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);

    return createNewSorter(context, orderings, batch, mainMapping, leftMapping, rightMapping);
  }

  public static Sorter createNewSorter(FragmentContext context, List<Ordering> orderings,
      VectorAccessible batch, MappingSet mainMapping, MappingSet leftMapping, MappingSet rightMapping) {
    CodeGenerator<Sorter> cg = CodeGenerator.get(Sorter.TEMPLATE_DEFINITION, context.getOptions());
    // This operator may be deprecated. No tests exercise it.
    // There is no way, at present, to verify if the generated code
    // works with Plain-old Java.
    // cg.plainOldJavaCapable(true);
    // Uncomment out this line to debug the generated code.
    // cg.saveCodeForDebugging(true);
    ClassGenerator<Sorter> g = cg.getRoot();
    g.setMappingSet(mainMapping);

    for(Ordering od : orderings) {
      // first, we rewrite the evaluation stack for each side of the comparison.
      ErrorCollector collector = new ErrorCollectorImpl();
      final LogicalExpression expr = ExpressionTreeMaterializer.materialize(od.getExpr(), batch, collector,context.getFunctionRegistry());
      collector.reportErrors(logger);
      g.setMappingSet(leftMapping);
      HoldingContainer left = g.addExpr(expr, ClassGenerator.BlkCreateMode.FALSE);
      g.setMappingSet(rightMapping);
      HoldingContainer right = g.addExpr(expr, ClassGenerator.BlkCreateMode.FALSE);
      g.setMappingSet(mainMapping);

      // next we wrap the two comparison sides and add the expression block for the comparison.
      LogicalExpression fh =
        FunctionGenerationHelper.getOrderingComparator(od.nullsSortHigh(), left, right,
                                                       context.getFunctionRegistry());
      HoldingContainer out = g.addExpr(fh, ClassGenerator.BlkCreateMode.FALSE);
      JConditional jc = g.getEvalBlock()._if(out.getValue().ne(JExpr.lit(0)));

      if (od.getDirection() == Direction.ASCENDING) {
        jc._then()._return(out.getValue());
      }else{
        jc._then()._return(out.getValue().minus());
      }
    }

    g.getEvalBlock()._return(JExpr.lit(0));

    return context.getImplementationClass(cg);
  }

  @Override
  public WritableBatch getWritableBatch() {
    throw new UnsupportedOperationException("A sort batch is not writable.");
  }

  @Override
  protected void cancelIncoming() {
    incoming.cancel();
  }

  @Override
  public void dump() {
    logger.error("SortBatch[popConfig={}, container={}, sorter={}, schema={}]", popConfig, container, sorter, schema);
  }
}
