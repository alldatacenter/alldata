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
package org.apache.drill.exec.physical.impl.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.codemodel.JExpr;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.FunctionCallFactory;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.ValueVectorWriteExpression;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.PhysicalOperatorUtil;
import org.apache.drill.exec.physical.config.StatisticsAggregate;
import org.apache.drill.exec.physical.impl.aggregate.StreamingAggBatch;
import org.apache.drill.exec.physical.impl.aggregate.StreamingAggTemplate;
import org.apache.drill.exec.physical.impl.aggregate.StreamingAggregator;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.FieldIdUtil;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * TODO: This needs cleanup. Currently the key values are constants and we compare the constants
 * for every record. Seems unnecessary.
 *
 * Example input and output:
 * Schema of incoming batch: region_id (VARCHAR), sales_city (VARCHAR), cnt (BIGINT)
 * Schema of outgoing batch:
 *    "columns"       : MAP - Column names
 *       "region_id"  : VARCHAR
 *       "sales_city" : VARCHAR
 *       "cnt"        : VARCHAR
 *    "statscount" : MAP
 *       "region_id"  : BIGINT - statscount(region_id) - aggregation over all values of region_id
 *                      in incoming batch
 *       "sales_city" : BIGINT - statscount(sales_city)
 *       "cnt"        : BIGINT - statscount(cnt)
 *    "nonnullstatcount" : MAP
 *       "region_id"  : BIGINT - nonnullstatcount(region_id)
 *       "sales_city" : BIGINT - nonnullstatcount(sales_city)
 *       "cnt"        : BIGINT - nonnullstatcount(cnt)
 *   .... another map for next stats function ....
 */

public class StatisticsAggBatch extends StreamingAggBatch {
  private static final Logger logger = LoggerFactory.getLogger(StatisticsAggBatch.class);

  // List of statistics functions e.g. rowcount, ndv output by StatisticsAggBatch
  private final List<String> functions;
  // List of implicit columns for which we do NOT want to compute statistics
  private final Map<String, ColumnExplorer.ImplicitFileColumns> implicitFileColumnsMap;

  public StatisticsAggBatch(StatisticsAggregate popConfig, RecordBatch incoming,
      FragmentContext context) throws OutOfMemoryException {
    super(popConfig, incoming, context);
    // Get the list from the physical operator configuration
    functions = popConfig.getFunctions();
    implicitFileColumnsMap = ColumnExplorer.initImplicitFileColumns(context.getOptions());
  }

  /*
   * Returns whether the given column is an implicit column
   */
  private boolean isImplicitFileOrPartitionColumn(MaterializedField mf, OptionManager optionManager) {
    return implicitFileColumnsMap.get(SchemaPath.getSimplePath(mf.getName()).toString()) != null ||
       ColumnExplorer.isPartitionColumn(optionManager, SchemaPath.getSimplePath(mf.getName()));
  }

  /*
   * Create the field id for the value vector corresponding to the materialized expression
   */
  private TypedFieldId createVVFieldId(LogicalExpression mle, String name, MapVector parent) {
    Class<? extends ValueVector> vvc =
            TypeHelper.getValueVectorClass(mle.getMajorType().getMinorType(),
                    mle.getMajorType().getMode());
    ValueVector vv = parent.addOrGet(name, mle.getMajorType(), vvc);
    TypedFieldId pfid = container.getValueVectorId(SchemaPath.getSimplePath(parent.getField().getName()));
    assert pfid.getFieldIds().length == 1;
    TypedFieldId.Builder builder = TypedFieldId.newBuilder();
    builder.addId(pfid.getFieldIds()[0]);
    TypedFieldId id =
        FieldIdUtil.getFieldIdIfMatches(parent, builder, true,
            SchemaPath.getSimplePath(vv.getField().getName()).getRootSegment());
    return id;
  }

  /*
   * Creates the key column within the parent value vector
   */
  private void createNestedKeyColumn(MapVector parent, String name, LogicalExpression expr,
      List<LogicalExpression> keyExprs, List<TypedFieldId> keyOutputIds) {
    LogicalExpression mle = PhysicalOperatorUtil.materializeExpression(expr, incoming, context);
    TypedFieldId id = createVVFieldId(mle, name, parent);
    keyExprs.add(mle);
    keyOutputIds.add(id);
  }

  /*
   * Creates the value vector within the parent value vector. The map vector key is
   * is the column name and value is the statistic expression e.g. "salary" : NDV(emp.salary)
   */
  private void addMapVector(String name, MapVector parent, LogicalExpression expr,
      List<LogicalExpression> valueExprs) {
    LogicalExpression mle = PhysicalOperatorUtil.materializeExpression(expr, incoming, context);
    TypedFieldId id = createVVFieldId(mle, name, parent);
    valueExprs.add(new ValueVectorWriteExpression(id, mle, true));
  }

  /*
   * Generates the code for the statistics aggregate which is subclassed from StreamingAggregator
   */
  private StreamingAggregator codegenAggregator(List<LogicalExpression> keyExprs,
      List<LogicalExpression> valueExprs, List<TypedFieldId> keyOutputIds) {

    ClassGenerator<StreamingAggregator> cg = CodeGenerator.getRoot(StreamingAggTemplate.TEMPLATE_DEFINITION, context.getOptions());
    cg.getCodeGenerator().plainJavaCapable(true);
    // Uncomment out this line to debug the generated code.
    // cg.getCodeGenerator().saveCodeForDebugging(true);

    LogicalExpression[] keyExprsArray = new LogicalExpression[keyExprs.size()];
    LogicalExpression[] valueExprsArray = new LogicalExpression[valueExprs.size()];
    TypedFieldId[] keyOutputIdsArray = new TypedFieldId[keyOutputIds.size()];

    keyExprs.toArray(keyExprsArray);
    valueExprs.toArray(valueExprsArray);
    keyOutputIds.toArray(keyOutputIdsArray);

    setupIsSame(cg, keyExprsArray);
    setupIsSameApart(cg, keyExprsArray);
    addRecordValues(cg, valueExprsArray);
    outputRecordKeys(cg, keyOutputIdsArray, keyExprsArray);
    outputRecordKeysPrev(cg, keyOutputIdsArray, keyExprsArray);

    cg.getBlock("resetValues")._return(JExpr.TRUE);
    getIndex(cg);

    container.buildSchema(SelectionVectorMode.NONE);
    StreamingAggregator agg = context.getImplementationClass(cg);
    try {
      agg.setup(oContext, incoming, this, ValueVector.MAX_ROW_COUNT);
    } catch (SchemaChangeException e) {
      throw schemaChangeException(e, logger);
    }
    return agg;
  }

  @Override
  protected StreamingAggregator createAggregatorInternal() {
    List<LogicalExpression> keyExprs = Lists.newArrayList();
    List<LogicalExpression> valueExprs = Lists.newArrayList();
    List<TypedFieldId> keyOutputIds = Lists.newArrayList();
    String [] colMeta = new String [] {Statistic.COLNAME, Statistic.COLTYPE};
    container.clear();
    // Generate the `column` map containing the columns in the incoming schema. Ignore
    // the implicit columns
    for (String col : colMeta) {
      MapVector parent = new MapVector(col, oContext.getAllocator(), null);
      container.add(parent);
      for (MaterializedField mf : incoming.getSchema()) {
        LogicalExpression expr;
        if (col.equals(colMeta[0])) {
          expr = ValueExpressions.getChar(SchemaPath.getSimplePath(mf.getName()).toString(), 0);
        } else {
          try {
            expr = ValueExpressions.getChar(DrillStatsTable.getMapper().writeValueAsString(mf.getType()), 0);
          } catch (JsonProcessingException e) {
            throw UserException.dataWriteError(e)
                .addContext("Failed to write statistics to JSON")
                .build();
          }
        }
        // Ignore implicit columns
        if (!isImplicitFileOrPartitionColumn(mf, incoming.getContext().getOptions())) {
          createNestedKeyColumn(
              parent,
              SchemaPath.getSimplePath(mf.getName()).toString(),
              expr,
              keyExprs,
              keyOutputIds
          );
        }
      }
    }
    // Iterate over the list of statistics and generate a MAP whose key is the column
    // and the value is the statistic for the column e.g.
    // NDV <<"employee_id" : 500>, <"salary" : 10>> represents a MAP of NDVs (# distinct values)
    // employee NDV = 500, salary NDV = 10
    for (String func : functions) {
      MapVector parent = new MapVector(func, oContext.getAllocator(), null);
      container.add(parent);

      for (MaterializedField mf : incoming.getSchema()) {
        // Check stats collection is only being done for supported data-types. Complex types
        // such as MAP, LIST are not supported!
        if (isColMinorTypeValid(mf) && !isImplicitFileOrPartitionColumn(mf, incoming.getContext().getOptions())) {
          List<LogicalExpression> args = Lists.newArrayList();
          args.add(SchemaPath.getSimplePath(mf.getName()));
          LogicalExpression call = FunctionCallFactory.createExpression(func, args);
          addMapVector(SchemaPath.getSimplePath(mf.getName()).toString(), parent, call, valueExprs);
        }
      }
    }
    // Now generate the code for the statistics aggregate
    return codegenAggregator(keyExprs, valueExprs, keyOutputIds);
  }

  private boolean isColMinorTypeValid(MaterializedField mf) throws UnsupportedOperationException {
    switch (mf.getType().getMinorType()) {
      case GENERIC_OBJECT:
      case LATE:
      case LIST:
      case MAP:
      case DICT:
      case UNION:
        return false;
      default:
        return true;
    }
  }
}
