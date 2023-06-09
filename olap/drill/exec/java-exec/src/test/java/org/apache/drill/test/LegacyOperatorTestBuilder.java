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
package org.apache.drill.test;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.physical.impl.ScanBatch;
import org.apache.drill.exec.record.RecordBatch;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @deprecated Use {@link OperatorTestBuilder} instead.
 */
@Deprecated
public class LegacyOperatorTestBuilder {

  private PhysicalOpUnitTestBase physicalOpUnitTestBase;

  private PhysicalOperator popConfig;
  private String[] baselineColumns;
  private List<Map<String, Object>> baselineRecords;
  private List<List<String>> inputStreamsJSON;
  private long initReservation = AbstractBase.INIT_ALLOCATION;
  private long maxAllocation = AbstractBase.MAX_ALLOCATION;
  private boolean expectNoRows;
  private Long expectedBatchSize;
  private Integer expectedNumBatches;
  private Integer expectedTotalRows;

  public LegacyOperatorTestBuilder(PhysicalOpUnitTestBase physicalOpUnitTestBase) {
    this.physicalOpUnitTestBase = physicalOpUnitTestBase;
  }

  @SuppressWarnings("unchecked")
  public void go() {
    BatchCreator<PhysicalOperator> opCreator;
    RecordBatch testOperator;
    try {
      physicalOpUnitTestBase.mockOpContext(popConfig, initReservation, maxAllocation);

      opCreator = (BatchCreator<PhysicalOperator>) physicalOpUnitTestBase.opCreatorReg.getOperatorCreator(popConfig.getClass());
      List<RecordBatch> incomingStreams = Lists.newArrayList();
      if (inputStreamsJSON != null) {
        for (List<String> batchesJson : inputStreamsJSON) {
          incomingStreams.add(new ScanBatch(popConfig, physicalOpUnitTestBase.fragContext,
              physicalOpUnitTestBase.getReaderListForJsonBatches(batchesJson, physicalOpUnitTestBase.fragContext)));
        }
      }

      testOperator = opCreator.getBatch(physicalOpUnitTestBase.fragContext, popConfig, incomingStreams);

      Map<String, List<Object>> actualSuperVectors = DrillTestWrapper.addToCombinedVectorResults(new PhysicalOpUnitTestBase.BatchIterator(testOperator), expectedBatchSize, expectedNumBatches, expectedTotalRows);
      if ( expectedTotalRows != null ) { return; } // when checking total rows, don't compare actual results

      Map<String, List<Object>> expectedSuperVectors;

      if (expectNoRows) {
        expectedSuperVectors = new TreeMap<>();
        for (String column : baselineColumns) {
          expectedSuperVectors.put(column, new ArrayList<>());
        }
      } else {
        expectedSuperVectors = DrillTestWrapper.translateRecordListToHeapVectors(baselineRecords);
      }

      DrillTestWrapper.compareMergedVectors(expectedSuperVectors, actualSuperVectors);

    } catch (ExecutionSetupException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public LegacyOperatorTestBuilder physicalOperator(PhysicalOperator batch) {
    this.popConfig = batch;
    return this;
  }

  public LegacyOperatorTestBuilder initReservation(long initReservation) {
    this.initReservation = initReservation;
    return this;
  }

  public LegacyOperatorTestBuilder maxAllocation(long maxAllocation) {
    this.maxAllocation = maxAllocation;
    return this;
  }

  public LegacyOperatorTestBuilder inputDataStreamJson(List<String> jsonBatches) {
    this.inputStreamsJSON = new ArrayList<>();
    this.inputStreamsJSON.add(jsonBatches);
    return this;
  }

  public LegacyOperatorTestBuilder inputDataStreamsJson(List<List<String>> childStreams) {
    this.inputStreamsJSON = childStreams;
    return this;
  }

  public LegacyOperatorTestBuilder baselineColumns(String... columns) {
    for (int i = 0; i < columns.length; i++) {
      LogicalExpression ex = physicalOpUnitTestBase.parseExpr(columns[i]);
      if (ex instanceof SchemaPath) {
        columns[i] = ((SchemaPath)ex).toExpr();
      } else {
        throw new IllegalStateException("Schema path is not a valid format.");
      }
    }
    this.baselineColumns = columns;
    return this;
  }

  public LegacyOperatorTestBuilder baselineValues(Object... baselineValues) {
    if (baselineRecords == null) {
      baselineRecords = new ArrayList<>();
    }
    Map<String, Object> ret = new HashMap<>();
    int i = 0;
    Preconditions.checkArgument(baselineValues.length == baselineColumns.length,
        "Must supply the same number of baseline values as columns.");
    for (String s : baselineColumns) {
      ret.put(s, baselineValues[i]);
      i++;
    }
    this.baselineRecords.add(ret);
    return this;
  }

  public LegacyOperatorTestBuilder expectZeroRows() {
    this.expectNoRows = true;
    return this;
  }

  public LegacyOperatorTestBuilder expectedNumBatches(Integer expectedNumBatches) {
    this.expectedNumBatches = expectedNumBatches;
    return this;
  }

  public LegacyOperatorTestBuilder expectedBatchSize(Long batchSize) {
    this.expectedBatchSize = batchSize;
    return this;
  }

  public LegacyOperatorTestBuilder expectedTotalRows(Integer expectedTotalRows) {
    this.expectedTotalRows = expectedTotalRows;
    return this;
  }
}
