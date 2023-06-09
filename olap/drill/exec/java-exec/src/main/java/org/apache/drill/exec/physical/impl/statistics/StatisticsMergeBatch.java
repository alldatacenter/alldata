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

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.PhysicalOperatorUtil;
import org.apache.drill.exec.physical.config.StatisticsMerge;
import org.apache.drill.exec.record.AbstractSingleRecordBatch;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.DateVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Example input and output:
 * Schema of incoming batch:<pre>
 *    "columns"       : MAP - Column names
 *       "region_id"  : VARCHAR
 *       "sales_city" : VARCHAR
 *       "cnt"        : VARCHAR
 *    "statscount" : MAP - Number of entries (rows)
 *       "region_id"  : BIGINT - statscount(region_id)
 *                      in incoming batch
 *       "sales_city" : BIGINT - statscount(sales_city)
 *       "cnt"        : BIGINT - statscount(cnt)
 *    "nonnullstatcount" : MAP - Number of non-null entries (rows)
 *       "region_id"  : BIGINT - nonnullstatcount(region_id)
 *       "sales_city" : BIGINT - nonnullstatcount(sales_city)
 *       "cnt"        : BIGINT - nonnullstatcount(cnt)
 *   .... another map for next stats function ....
 * </pre>Schema of outgoing batch:<pre>
 *    "schema" : BIGINT - Schema number. For each schema change this number is incremented.
 *    "computed" : DATE - What time is it computed?
 *    "columns"       : MAP - Column names
 *       "region_id"  : VARCHAR
 *       "sales_city" : VARCHAR
 *       "cnt"        : VARCHAR
 *    "statscount" : MAP - Number of entries (rows)
 *       "region_id"  : BIGINT - statscount(region_id) - aggregation over all values of region_id
 *                      in incoming batch
 *       "sales_city" : BIGINT - statscount(sales_city)
 *       "cnt"        : BIGINT - statscount(cnt)
 *    "nonnullstatcount" : MAP - Number of non-null entries (rows)
 *       "region_id"  : BIGINT - nonnullstatcount(region_id)
 *       "sales_city" : BIGINT - nonnullstatcount(sales_city)
 *       "cnt"        : BIGINT - nonnullstatcount(cnt)
 *   .... another map for next stats function ....
 * </pre>
 */
public class StatisticsMergeBatch extends AbstractSingleRecordBatch<StatisticsMerge> {

  private final Map<String, String> functions;
  private boolean first = true;
  private boolean finished;
  private int schema;
  private List<String> columnsList;
  private double samplePercent = 100.0;
  private final List<MergedStatistic> mergedStatisticList;

  public StatisticsMergeBatch(StatisticsMerge popConfig, RecordBatch incoming,
      FragmentContext context) throws OutOfMemoryException {
    super(popConfig, context, incoming);
    functions = popConfig.getFunctions();
    samplePercent = popConfig.getSamplePercent();
    mergedStatisticList = new ArrayList<>();
  }

  /*
   * Creates key columns for the outgoing batch e.g. `schema`, `computed`. These columns are NOT
   * table columns for which statistics will be computed.
   */
  private void createKeyColumn(String name, LogicalExpression expr) {
    LogicalExpression mle = PhysicalOperatorUtil.materializeExpression(expr, incoming, context);
    MaterializedField outputField = MaterializedField.create(name, mle.getMajorType());
    ValueVector vector = TypeHelper.getNewVector(outputField, oContext.getAllocator());
    container.add(vector);
  }

  /*
   * Identify the list of fields within a map which are generated by StatisticsMerge. Perform
   * basic sanity check i.e. all maps have the same number of columns and those columns are
   * the same in each map
   */
  private void buildColumnsList() {
    Map<String, Boolean> inputFunctions = new HashMap<>();
    // Prepare map of input functions for verifying only they appear in the incoming batch
    for (String inputFunc : functions.values()) {
      inputFunctions.put(inputFunc, false);
    }
    List<String> lastMapColumnsList = null;
    //Populate the columns list from the `columns` map
    for (VectorWrapper<?> vw : incoming) {
      String inputFunc = vw.getField().getName();
      if (vw.getField().getType().getMinorType() != TypeProtos.MinorType.MAP) {
        continue;
      }
      if (inputFunctions.get(inputFunc)) {
        throw new IllegalArgumentException (String.format("The statistic `%s` appears more than once",
            inputFunc));
      } else {
        inputFunctions.put(inputFunc, true);
      }
      if (vw.getField().getName().equals(Statistic.COLNAME)) {
        columnsList = Lists.newArrayList();
        for (ValueVector vv : vw.getValueVector()) {
          if (vv.getField().getType().getMinorType() == TypeProtos.MinorType.MAP) {
            throw new IllegalArgumentException("StatisticsMerge of nested map is not supported");
          }
          columnsList.add(vv.getField().getName());
        }
        lastMapColumnsList = columnsList;
      }
    }
    // Verify the rest of the maps have the same columns
    for (VectorWrapper<?> vw : incoming) {
      String inputFunc = vw.getField().getName();
      if (vw.getField().getType().getMinorType() != TypeProtos.MinorType.MAP) {
        continue;
      }
      if (!inputFunctions.get(inputFunc)) {
        throw new IllegalArgumentException (String.format("The statistic `%s` is not expected here",
            inputFunc));
      }
      if (columnsList.size() != lastMapColumnsList.size()
          || !lastMapColumnsList.containsAll(columnsList)) {
        // Error!! Maps with different size and/or keys. The map for each statistics (e.g. NDV)
        // should match exactly with the column map i.e. we did not run into any issues while
        // generating statistics for all the specified columns
        throw new IllegalStateException("StatisticsMerge Maps have different fields");
      }
    }
  }

  /**
   * Prepare the outgoing container. Generates the outgoing record batch schema.
   * Please look at the comments above the class definition which describes the
   * incoming/outgoing batch schema
   */
  private void buildOutputContainer() {
    // Populate the list of statistics which will be output in the schema
    for (VectorWrapper<?> vw : incoming) {
      for (String outputStatName : functions.keySet()) {
        if (functions.get(outputStatName).equals(vw.getField().getName())) {
          mergedStatisticList.add(MergedStatisticFactory.getMergedStatistic(outputStatName,
              functions.get(outputStatName), samplePercent));
        }
      }
    }
    // Configure settings/dependencies for statistics, if needed
    for (MergedStatistic statistic : mergedStatisticList) {
      if (statistic.getName().equals(Statistic.AVG_WIDTH)) {
        ((AvgWidthMergedStatistic)statistic).configure(mergedStatisticList);
      } else if (statistic.getName().equals(Statistic.NDV)) {
        NDVMergedStatistic.NDVConfiguration config =
            new NDVMergedStatistic.NDVConfiguration(context.getOptions(),
                mergedStatisticList);
        ((NDVMergedStatistic)statistic).configure(config);
      } else if (statistic.getName().equals(Statistic.SUM_DUPS)) {
        ((CntDupsMergedStatistic)statistic).configure(mergedStatisticList);
      } else if (statistic.getName().equals(Statistic.HLL_MERGE)) {
        ((HLLMergedStatistic)statistic).configure(context.getOptions());
      } else if (statistic.getName().equals(Statistic.TDIGEST_MERGE)) {
        ((TDigestMergedStatistic)statistic).configure(context.getOptions());
      }
    }
    // Create the schema number and time when computed in the outgoing vector
    createKeyColumn(Statistic.SCHEMA, ValueExpressions.getBigInt(schema++));
    GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    calendar.setTimeInMillis(System.currentTimeMillis());
    createKeyColumn(Statistic.COMPUTED, ValueExpressions.getDate(calendar));

    // Create output map vectors corresponding to each statistic (e.g. rowcount)
    for (MergedStatistic statistic : mergedStatisticList) {
      String targetTypeStatistic = statistic.getInput();
      for (VectorWrapper<?> vw : incoming) {
        if (targetTypeStatistic.equals(vw.getField().getName())) {
          addVectorToOutgoingContainer(statistic.getName(), vw);
        }
      }
    }
    container.setEmpty();
    container.buildSchema(incoming.getSchema().getSelectionVectorMode());
  }

  /**
   * Adds a value vector corresponding to the statistic in the outgoing record
   * batch. Determines the MajorType based on the incoming value vector. Please
   * look at the comments above the class definition which describes the
   * incoming/outgoing batch schema
   */
  private void addVectorToOutgoingContainer(String outStatName, VectorWrapper<?> vw) {
    // Input map vector
    MapVector inputVector = (MapVector) vw.getValueVector();
    assert inputVector.getPrimitiveVectors().size() > 0;
    // Proceed to create output map vector with same name e.g. statcount etc.
    MajorType mt = inputVector.getField().getType();
    MaterializedField mf = MaterializedField.create(outStatName, mt);

    ValueVector outputValueVector = TypeHelper.getNewVector(mf, oContext.getAllocator());
    container.add(outputValueVector);
    MapVector outputVector = (MapVector) outputValueVector;

    for (ValueVector vv : inputVector) {
      String columnName = vv.getField().getName();
      // Except column name, type all the rest are actual statistic functions (rely on
      // statistic calculation functions).
      if (outStatName.equals(Statistic.COLNAME)
          || outStatName.equals(Statistic.COLTYPE)) {
        outputVector.addOrGet(columnName, vv.getField().getType(), vv.getClass());
      } else {
        TypeProtos.MinorType minorType;
        if (outStatName.equals(Statistic.AVG_WIDTH)) {
          minorType = TypeProtos.MinorType.FLOAT8;
        } else if (outStatName.equals(Statistic.HLL_MERGE) ||
                   outStatName.equals(Statistic.TDIGEST_MERGE)) {
          minorType = TypeProtos.MinorType.VARBINARY;
        } else {
          minorType = TypeProtos.MinorType.BIGINT;
        }
        Class<? extends ValueVector> vvc =
                TypeHelper.getValueVectorClass(minorType,
                        TypeProtos.DataMode.OPTIONAL);
        outputVector.addOrGet(columnName, Types.optional(minorType), vvc);
      }
    }
  }

  /**
   * Prepare the outgoing container. Populates the outgoing record batch data.
   * Please look at the comments above the class definition which describes the
   * incoming/outgoing batch schema
   */
  private IterOutcome buildOutgoingRecordBatch() {
    for (VectorWrapper<?> vw : container) {
      String outputStatName = vw.getField().getName();
      // Populate the `schema` and `computed` fields
      if (outputStatName.equals(Statistic.SCHEMA)) {
        BigIntVector vv = (BigIntVector) vw.getValueVector();
        vv.allocateNewSafe();
        vv.getMutator().setSafe(0, schema);
      } else if (outputStatName.equals(Statistic.COMPUTED)) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        DateVector vv = (DateVector) vw.getValueVector();
        vv.allocateNewSafe();
        vv.getMutator().setSafe(0, cal.getTimeInMillis());
      } else {
        // Populate the rest of the merged statistics. Each statistic is a map which
        // contains <COL_NAME, STATS_VALUE> pairs
        MapVector vv = (MapVector) vw.getValueVector();
        for (MergedStatistic outputStat : mergedStatisticList) {
          if (outputStatName.equals(outputStat.getName())) {
            outputStat.setOutput(vv);
            vv.getMutator().setValueCount(columnsList.size());
            break;
          }
        }
      }
    }
    // Populate the number of records (1) inside the outgoing batch.
    container.setValueCount(1);
    return IterOutcome.OK;
  }

  @Override
  protected boolean setupNewSchema() {
    container.clear();
    // Generate the list of fields for which statistics will be merged
    buildColumnsList();
    // Generate the schema for the outgoing record batch
    buildOutputContainer();
    return true;
  }

  @Override
  protected IterOutcome doWork() {
    for (MergedStatistic outputStat : mergedStatisticList) {
      String inputStat = outputStat.getInput();
      for (VectorWrapper<?> vw : incoming) {
        MapVector vv = (MapVector) vw.getValueVector();
        if (vv.getField().getName().equals(inputStat)) {
          outputStat.merge(vv);
          break;
        }
      }
    }
    return IterOutcome.OK;
  }

  @Override
  public VectorContainer getOutgoingContainer() {
    return this.container;
  }

  @Override
  public void dump() { }

  @Override
  public IterOutcome innerNext() {
    IterOutcome outcome;
    boolean didSomeWork = false;
    if (finished) {
      return IterOutcome.NONE;
    }
    outer: while (true) {
      outcome = next(incoming);
      switch (outcome) {
        case NONE:
          break outer;
        case NOT_YET:
          return outcome;
        case OK_NEW_SCHEMA:
          if (first) {
            first = false;
            if (!setupNewSchema()) {
              outcome = IterOutcome.OK;
            }
            return outcome;
          }
          //fall through
        case OK:
          assert first == false : "First batch should be OK_NEW_SCHEMA";
          IterOutcome out = doWork();
          didSomeWork = true;
          if (out != IterOutcome.OK) {
            return out;
          }
          break;
        default:
          throw new UnsupportedOperationException("Unsupported upstream state " + outcome);
      }
    }

    // We can only get here if upstream is NONE i.e. no more batches. If we did some work prior to
    // exhausting all upstream, then return OK. Otherwise, return NONE.
    if (didSomeWork) {
      IterOutcome out = buildOutgoingRecordBatch();
      finished = true;
      return out;
    } else {
      return outcome;
    }
  }

  @Override
  public int getRecordCount() {
    return container.getRecordCount();
  }
}
