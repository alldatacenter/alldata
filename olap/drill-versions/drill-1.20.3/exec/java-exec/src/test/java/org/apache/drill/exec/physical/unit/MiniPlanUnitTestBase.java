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
package org.apache.drill.exec.physical.unit;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.DrillTestWrapper;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.physical.impl.ScanBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.store.RecordReader;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.parquet.ParquetDirectByteBufferAllocator;
import org.apache.drill.exec.store.parquet.ParquetReaderUtility;
import org.apache.drill.exec.store.parquet.columnreaders.ParquetRecordReader;
import org.apache.drill.exec.store.parquet.compression.DrillCompressionCodecFactory;
import org.apache.drill.test.LegacyOperatorTestBuilder;
import org.apache.drill.test.PhysicalOpUnitTestBase;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.drill.exec.physical.base.AbstractBase.INIT_ALLOCATION;
import static org.apache.drill.exec.physical.base.AbstractBase.MAX_ALLOCATION;

/**
 * A MiniPlanUnitTestBase extends PhysicalOpUnitTestBase, to construct MiniPlan (aka plan fragment).
 * in the form of physical operator tree, and verify both the expected schema and output row results.
 * Steps to construct a unit:
 * 1. Call PopBuilder / ScanPopBuilder to construct the MiniPlan
 * 2. Create a MiniPlanTestBuilder, and specify the expected schema and base line values, or if there
 * is no batch expected.
 */

public class MiniPlanUnitTestBase extends PhysicalOpUnitTestBase {
  public static class MiniPlanTestBuilder {
    protected List<Map<String, Object>> baselineRecords;
    protected RecordBatch root;
    protected Integer expectBatchNum = null;
    protected BatchSchema expectSchema;
    protected boolean expectZeroRow;

    /**
     * Specify the root operator for a MiniPlan.
     * @param root
     * @return
     */
    public MiniPlanTestBuilder root(RecordBatch root) {
      this.root = root;
      return this;
    }

    /**
     * Specify the expected batch schema.
     * @param batchSchema
     * @return
     */
    public MiniPlanTestBuilder expectSchema(BatchSchema batchSchema) {
      this.expectSchema = batchSchema;
      return this;
    }

    /**
     * Specify one row of expected values. The number of values have to be same as # of fields in expected batch schema.
     * @param baselineValues
     * @return
     */
    public MiniPlanTestBuilder baselineValues(Object... baselineValues) {
      if (baselineRecords == null) {
        baselineRecords = new ArrayList<>();
      }

      Map<String, Object> ret = new HashMap<>();
      int i = 0;
      Preconditions.checkArgument(expectSchema != null, "Expected schema should be set before specify baseline values.");
      Preconditions.checkArgument(baselineValues.length == expectSchema.getFieldCount(),
          "Must supply the same number of baseline values as columns in expected schema.");

      for (MaterializedField field : expectSchema) {
        ret.put(SchemaPath.getSimplePath(field.getName()).toExpr(), baselineValues[i]);
        i++;
      }

      this.baselineRecords.add(ret);
      return this;
    }

    /**
     * Specify one special case, where the operator tree should return 0 batch.
     * @param expectNullBatch
     * @return
     */
    public MiniPlanTestBuilder expectNullBatch(boolean expectNullBatch) {
      if (expectNullBatch) {
        this.expectBatchNum = 0;
      }
      return this;
    }

    /**
     * Specify the expected number of batches from operator tree.
     * @param
     * @return
     */
    public MiniPlanTestBuilder expectBatchNum(int expectBatchNum) {
      this.expectBatchNum = expectBatchNum;
      return this;
    }

    public MiniPlanTestBuilder expectZeroRow(boolean expectedZeroRow) {
      this.expectZeroRow = expectedZeroRow;
      return this;
    }

    public void go() throws Exception {
      final BatchIterator batchIterator = new BatchIterator(root);

      // verify case of zero batch.
      if (expectBatchNum != null && expectBatchNum == 0) {
        if (batchIterator.iterator().hasNext()) {
          throw new AssertionError("Expected zero batches from operator tree. But operators return at least 1 batch!");
        } else {
          return; // successful
        }
      }
      Map<String, List<Object>> actualSuperVectors = new TreeMap<String, List<Object>>();

      int actualBatchNum = DrillTestWrapper.addToCombinedVectorResults(batchIterator, expectSchema, null, null, actualSuperVectors, null);
      if (expectBatchNum != null) {
        if (expectBatchNum != actualBatchNum) {
          throw new AssertionError(String.format("Expected %s batches from operator tree. But operators return %s batch!", expectBatchNum, actualBatchNum));
        }
      }
      Map<String, List<Object>> expectedSuperVectors;
      if (!expectZeroRow) {
        expectedSuperVectors = DrillTestWrapper.translateRecordListToHeapVectors(baselineRecords);
      } else {
        expectedSuperVectors = new TreeMap<>();
        for (MaterializedField field : expectSchema) {
          expectedSuperVectors.put(SchemaPath.getSimplePath(field.getName()).toExpr(), new ArrayList<>());
        }
      }
      DrillTestWrapper.compareMergedVectors(expectedSuperVectors, actualSuperVectors);
    }
  }

  /**
   * Similar to {@link LegacyOperatorTestBuilder}, build a physical operator (RecordBatch) and specify its input record batches.
   * The input record batch could be a non-scan operator by calling {@link PopBuilder#addInputAsChild},
   * or a scan operator by calling {@link PopBuilder#addJsonScanAsChild()} if it's SCAN operator.
   *
   * A miniplan rooted as join operator like following could be constructed in either the following way:
   *
   * <pre><code>
   *                 Join
   *                /    \
   *          JSON_T1    Filter
   *                       \
   *                     JSON_T2
   * </code></pre>
   *
   * <pre><code>
   * new PopBuilder()
   *  .physicalOperator(joinPopConfig)
   *  .addScanAsChild()
   *      .fileSystem(..)
   *      .columnsToRead(...)
   *      .inputPath(...)
   *      .buildAddAsInput()
   *  .addInputAsChild()
   *      .physicalOperator(filterPopConfig)
   *      .addScanAsChild()
   *          .fileSystem(...)
   *          .columnsToRead(...)
   *          .inputPath(...)
   *          .buildAddAsInput()
   *      .buildAddAsInput()
   *  .build();
   * </code></pre>
   *
   * <pre><code>
   *   RecordBatch scan1 = new ScanPopBuilder()
   *                          .fileSystem(...)
   *                          .columnsToRead(..)
   *                          .inputPath(...)
   *                          .build();
   *   RecordBatch scan2 = ... ;
   *
   *   RecordBatch filter = new PopBuilder()
   *                          .physicalOperator(filterPopConfig)
   *                          .addInput(scan2);
   *   RecordBatch join = new PopBuilder()
   *                          .physicalOperator(joinPopConfig)
   *                          .addInput(scan1)
   *                          .addInput(filter)
   *                          .build();
   *
   * </pre></code>
   */

  public class PopBuilder  {
    protected PhysicalOperator popConfig;
    protected long initReservation = INIT_ALLOCATION;
    protected long maxAllocation = MAX_ALLOCATION;

    final private List<RecordBatch> inputs = Lists.newArrayList();
    final PopBuilder parent;

    public PopBuilder() {
      this.parent = null;
    }

    public PopBuilder(PopBuilder parent) {
      this.parent = parent;
    }

    public PopBuilder physicalOperator(PhysicalOperator popConfig) {
      this.popConfig = popConfig;
      return this;
    }

    /**
     * Set initial memory reservation used by this operator's allocator. Default is {@link org.apache.drill.exec.physical.base.AbstractBase#INIT_ALLOCATION}
     * @param initReservation
     * @return
     */
    public PopBuilder initReservation(long initReservation) {
      this.initReservation = initReservation;
      return this;
    }

    /**
     * Set max memory reservation used by this operator's allocator. Default is {@link org.apache.drill.exec.physical.base.AbstractBase#MAX_ALLOCATION}
     * @param maxAllocation
     * @return
     */
    public PopBuilder maxAllocation(long maxAllocation) {
      this.maxAllocation = maxAllocation;
      return this;
    }

    /**
     * Return a ScanPopBuilder to build a Scan recordBatch, which will be added as input batch after
     * call {@link PopBuilder#buildAddAsInput()}
     * @return  ScanPopBuilder
     */
    public JsonScanBuilder addJsonScanAsChild() {
      return  new JsonScanBuilder(this);
    }

    /**
     * Return a ScanPopBuilder to build a Scan recordBatch, which will be added as input batch after
     * call {@link PopBuilder#buildAddAsInput()}
     * @return  ScanPopBuilder
     */
    public ParquetScanBuilder addParquetScanAsChild() {
      return  new ParquetScanBuilder(this);
    }

    /**
     * Return a nested PopBuilder to build a non-scan recordBatch, which will be added as input batch after
     * call {@link PopBuilder#buildAddAsInput()}
     * @return a nested PopBuild for non-scan recordbatch.
     */
    public PopBuilder addInputAsChild() {
      return  new PopBuilder(this) {
      };
    }

    public PopBuilder addInput(RecordBatch batch) {
      inputs.add(batch);
      return this;
    }

    public PopBuilder buildAddAsInput() throws Exception {
      mockOpContext(popConfig, initReservation, maxAllocation);
      @SuppressWarnings("unchecked")
      BatchCreator<PhysicalOperator> opCreator =  (BatchCreator<PhysicalOperator>) getOpCreatorReg().getOperatorCreator(popConfig.getClass());
      RecordBatch batch= opCreator.getBatch(fragContext, popConfig, inputs);
      return parent.addInput(batch);
    }

    public RecordBatch build() throws Exception {
      mockOpContext(popConfig, initReservation, maxAllocation);
      @SuppressWarnings("unchecked")
      BatchCreator<PhysicalOperator> opCreator =  (BatchCreator<PhysicalOperator>) getOpCreatorReg().getOperatorCreator(popConfig.getClass());
      return opCreator.getBatch(fragContext, popConfig, inputs);
    }
  }

  public abstract class ScanPopBuider<T extends ScanPopBuider<?>> extends PopBuilder {
    List<SchemaPath> columnsToRead = Collections.singletonList(SchemaPath.STAR_COLUMN);
    DrillFileSystem fs = null;

    public ScanPopBuider() {
      super(null); // Scan is root operator.
    }

    public ScanPopBuider(PopBuilder parent) {
      super(parent);
    }

    @SuppressWarnings("unchecked")
    public T fileSystem(DrillFileSystem fs) {
      this.fs = fs;
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T columnsToRead(SchemaPath... columnsToRead) {
      this.columnsToRead = Lists.newArrayList(columnsToRead);
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T columnsToRead(String... columnsToRead) {
      this.columnsToRead = Lists.newArrayList();

      for (String column : columnsToRead) {

        this.columnsToRead.add(SchemaPath.getSimplePath(column));
      }
      return (T) this;
    }

  }

  /**
   * Builder for Json Scan RecordBatch.
   */
  public class JsonScanBuilder extends ScanPopBuider<JsonScanBuilder> {
    List<String> jsonBatches = null;
    List<Path> inputPaths = Collections.emptyList();

    public JsonScanBuilder(PopBuilder parent) {
      super(parent);
    }

    public JsonScanBuilder() {
      super();
    }

    public JsonScanBuilder jsonBatches(List<String> jsonBatches) {
      this.jsonBatches = jsonBatches;
      return this;
    }

    public JsonScanBuilder inputPaths(List<Path> inputPaths) {
      this.inputPaths = inputPaths;
      return this;
    }

    @Override
    public PopBuilder buildAddAsInput() throws Exception {
      RecordBatch scanBatch = getScanBatch();
      return parent.addInput(scanBatch);
    }

    @Override
    public RecordBatch build() throws Exception {
      return getScanBatch();
    }

    private RecordBatch getScanBatch() throws Exception {
      Iterator<RecordReader> readers = null;

      if (jsonBatches != null) {
        readers = getJsonReadersFromBatchString(jsonBatches, fragContext, columnsToRead);
      } else {
        readers = getJsonReadersFromInputFiles(fs, inputPaths, fragContext, columnsToRead);
      }

      List<RecordReader> readerList = new LinkedList<>();
      while(readers.hasNext()) {
        readerList.add(readers.next());
      }

      RecordBatch scanBatch = new ScanBatch(new MockPhysicalOperator(), fragContext, readerList);
      return scanBatch;
    }
  }

  /**
   * Builder for parquet Scan RecordBatch.
   */
  public class ParquetScanBuilder extends ScanPopBuider<ParquetScanBuilder> {
    List<Path> inputPaths = Collections.emptyList();

    public ParquetScanBuilder() {
      super();
    }

    public ParquetScanBuilder(PopBuilder parent) {
      super(parent);
    }

    public ParquetScanBuilder inputPaths(List<Path> inputPaths) {
      this.inputPaths = inputPaths;
      return this;
    }

    @Override
    public PopBuilder buildAddAsInput() throws Exception {
      mockOpContext(popConfig, this.initReservation, this.maxAllocation);
      RecordBatch scanBatch = getScanBatch();
      return parent.addInput(scanBatch);
    }

    @Override
    public RecordBatch build() throws Exception {
      mockOpContext(popConfig, this.initReservation, this.maxAllocation);
      return getScanBatch();
    }

    private RecordBatch getScanBatch() throws Exception {
      List<RecordReader> readers = new LinkedList<>();

      for (Path path : inputPaths) {
        ParquetMetadata footer = ParquetFileReader.readFooter(fs.getConf(), path);

        for (int i = 0; i < footer.getBlocks().size(); i++) {
          CompressionCodecFactory ccf = DrillCompressionCodecFactory.createDirectCodecFactory(
            fs.getConf(),
            new ParquetDirectByteBufferAllocator(opContext.getAllocator()),
            0
          );
          readers.add(new ParquetRecordReader(fragContext,
              path,
              i,
              fs,
              ccf,
              footer,
              columnsToRead,
              ParquetReaderUtility.DateCorruptionStatus.META_SHOWS_NO_CORRUPTION));
        }
      }

      RecordBatch scanBatch = new ScanBatch(new MockPhysicalOperator(), fragContext, readers);
      return scanBatch;
    }
  } // end of ParquetScanBuilder

  @Override
  protected void mockOpContext(PhysicalOperator popConfig, long initReservation, long maxAllocation) throws Exception {
    super.mockOpContext(popConfig, initReservation, maxAllocation);
  }
}
