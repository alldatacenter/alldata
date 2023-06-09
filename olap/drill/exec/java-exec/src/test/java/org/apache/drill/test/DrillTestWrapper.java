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

import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.HyperVectorValueIterator;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.HyperVectorWrapper;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.util.Text;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the options for a Drill unit test, as well as the execution
 * methods to perform the tests and validation of results.
 * <p>
 * To construct an instance easily, look at the TestBuilder class. From an
 * implementation of the BaseTestQuery class, and instance of the builder is
 * accessible through the testBuilder() method.
 */
public class DrillTestWrapper {

  private static final Logger logger = LoggerFactory.getLogger(DrillTestWrapper.class);

  public interface TestServices {
    BufferAllocator allocator();

    void test(String query) throws Exception;

    List<QueryDataBatch> testRunAndReturn(QueryType type, Object query) throws Exception;
  }

  // TODO - when in JSON, read baseline in all text mode to avoid precision loss for decimal values

  // This flag will enable all of the values that are validated to be logged. For large validations this is time consuming
  // so this is not exposed in a way that it can be enabled for an individual test. It can be changed here while debugging
  // a test to see all of the output, but as this framework is doing full validation, there is no reason to keep it on as
  // it will only make the test slower.
  private static boolean VERBOSE_DEBUG = false;

  // Unit test doesn't expect any specific batch count
  public static final int EXPECTED_BATCH_COUNT_NOT_SET = -1;
  public static final int EXPECTED_NUM_RECORDS_NOT_SET = - 1;

  // The motivation behind the TestBuilder was to provide a clean API for test writers. The model is mostly designed to
  // prepare all of the components necessary for running the tests, before the TestWrapper is initialized. There is however
  // one case where the setup for the baseline is driven by the test query results, and this is implicit type enforcement
  // for the baseline data. In this case there needs to be a call back into the TestBuilder once we know the type information
  // from the test query.
  private final TestBuilder testBuilder;
  /**
   * Test query to run. Type of object depends on the {@link #queryType}
   */
  private final Object query;
  // The type of query provided
  private final UserBitShared.QueryType queryType;
  // The type of query provided for the baseline
  private final UserBitShared.QueryType baselineQueryType;
  // should ordering be enforced in the baseline check
  private final boolean ordered;
  private final TestServices services;
  // queries to run before the baseline or test queries, can be used to set options
  private final String baselineOptionSettingQueries;
  private final String testOptionSettingQueries;
  // allow approximate equality tests for number types
  private final boolean approximateEquality;
  // tolerance for approximate equality tests defined as |Expected - Actual|/|Expected| <= Tolerance
  private final double tolerance;
  // two different methods are available for comparing ordered results, the default reads all of the records
  // into giant lists of objects, like one giant on-heap batch of 'vectors'
  // this flag enables the other approach which iterates through a hyper batch for the test query results and baseline
  // while this does work faster and use less memory, it can be harder to debug as all of the elements are not in a
  // single list
  private final boolean highPerformanceComparison;
  // if the baseline is a single option test writers can provide the baseline values and columns
  // without creating a file, these are provided to the builder in the baselineValues() and baselineColumns() methods
  // and translated into a map in the builder
  private final String[] baselineColumns;
  private final List<Map<String, Object>> baselineRecords;

  private final int expectedNumBatches;
  private final int expectedNumRecords;

  public DrillTestWrapper(TestBuilder testBuilder, TestServices services, Object query, QueryType queryType,
      String baselineOptionSettingQueries, String testOptionSettingQueries,
      QueryType baselineQueryType, boolean ordered, boolean approximateEquality, double tolerance,
      boolean highPerformanceComparison,
      String[] baselineColumns, List<Map<String, Object>> baselineRecords, int expectedNumBatches,
      int expectedNumRecords) {
    this.testBuilder = testBuilder;
    this.services = services;
    this.query = query;
    this.queryType = queryType;
    this.baselineQueryType = baselineQueryType;
    this.ordered = ordered;
    this.approximateEquality = approximateEquality;
    this.tolerance = tolerance;
    this.baselineOptionSettingQueries = baselineOptionSettingQueries;
    this.testOptionSettingQueries = testOptionSettingQueries;
    this.highPerformanceComparison = highPerformanceComparison;
    this.baselineColumns = baselineColumns;
    this.baselineRecords = baselineRecords;
    this.expectedNumBatches = expectedNumBatches;
    this.expectedNumRecords = expectedNumRecords;

    Preconditions.checkArgument(!(baselineRecords != null && !ordered && highPerformanceComparison));
    Preconditions.checkArgument((baselineRecords != null && expectedNumRecords == DrillTestWrapper.EXPECTED_NUM_RECORDS_NOT_SET) || baselineRecords == null,
      "Cannot define both baselineRecords and the expectedNumRecords.");
    Preconditions.checkArgument((baselineQueryType != null && expectedNumRecords == DrillTestWrapper.EXPECTED_NUM_RECORDS_NOT_SET) || baselineQueryType == null,
      "Cannot define both a baselineQueryType and the expectedNumRecords.");
  }

  public void run() throws Exception {
    if (testBuilder.getExpectedSchema() != null) {
      compareSchemaOnly();
    } else {
      if (ordered) {
        compareOrderedResults();
      } else {
        compareUnorderedResults();
      }
    }
  }

  private BufferAllocator getAllocator() {
    return services.allocator();
  }

  private void compareHyperVectors(Map<String, HyperVectorValueIterator> expectedRecords,
      Map<String, HyperVectorValueIterator> actualRecords) throws Exception {
    for (String s : expectedRecords.keySet()) {
      assertNotNull("Expected column '" + s + "' not found.", actualRecords.get(s));
      assertEquals(expectedRecords.get(s).getTotalRecords(), actualRecords.get(s).getTotalRecords());
      HyperVectorValueIterator expectedValues = expectedRecords.get(s);
      HyperVectorValueIterator actualValues = actualRecords.get(s);
      int i = 0;
      while (expectedValues.hasNext()) {
        compareValuesErrorOnMismatch(expectedValues.next(), actualValues.next(), i, s);
        i++;
      }
    }
    cleanupHyperValueIterators(expectedRecords.values());
    cleanupHyperValueIterators(actualRecords.values());
  }

  private void cleanupHyperValueIterators(Collection<HyperVectorValueIterator> hyperBatches) {
    for (HyperVectorValueIterator hvi : hyperBatches) {
      for (ValueVector vv : hvi.getHyperVector().getValueVectors()) {
        vv.clear();
      }
    }
  }

  public static void compareMergedVectors(Map<String, List<Object>> expectedRecords, Map<String, List<Object>> actualRecords) throws Exception {
    for (String s : actualRecords.keySet()) {
      assertNotNull("Unexpected extra column " + s + " returned by query.", expectedRecords.get(s));
      assertEquals("Incorrect number of rows returned by query.", expectedRecords.get(s).size(), actualRecords.get(s).size());
      List<?> expectedValues = expectedRecords.get(s);
      List<?> actualValues = actualRecords.get(s);
      assertEquals("Different number of records returned", expectedValues.size(), actualValues.size());

      for (int i = 0; i < expectedValues.size(); i++) {
        try {
          compareValuesErrorOnMismatch(expectedValues.get(i), actualValues.get(i), i, s);
        } catch (Exception ex) {
          throw new Exception(ex.getMessage() + "\n\n" + printNearbyRecords(expectedRecords, actualRecords, i), ex);
        }
      }
    }
    if (actualRecords.size() < expectedRecords.size()) {
      throw new Exception(findMissingColumns(expectedRecords.keySet(), actualRecords.keySet()));
    }
  }

  private static String printNearbyRecords(Map<String, List<Object>> expectedRecords, Map<String, List<Object>> actualRecords, int offset) {
    StringBuilder expected = new StringBuilder();
    StringBuilder actual = new StringBuilder();
    expected.append("Expected Records near verification failure:\n");
    actual.append("Actual Records near verification failure:\n");
    int firstRecordToPrint = Math.max(0, offset - 5);
    List<?> expectedValuesInFirstColumn = expectedRecords.get(expectedRecords.keySet().iterator().next());
    List<?> actualValuesInFirstColumn = expectedRecords.get(expectedRecords.keySet().iterator().next());
    int numberOfRecordsToPrint = Math.min(Math.min(10, expectedValuesInFirstColumn.size()), actualValuesInFirstColumn.size());
    for (int i = firstRecordToPrint; i < numberOfRecordsToPrint; i++) {
      expected.append("Record Number: ").append(i).append(" { ");
      actual.append("Record Number: ").append(i).append(" { ");
      for (String s : actualRecords.keySet()) {
        List<?> actualValues = actualRecords.get(s);
        actual.append(s).append(" : ").append(actualValues.get(i)).append(",");
      }
      for (String s : expectedRecords.keySet()) {
        List<?> expectedValues = expectedRecords.get(s);
        expected.append(s).append(" : ").append(expectedValues.get(i)).append(",");
      }
      expected.append(" }\n");
      actual.append(" }\n");
    }

    return expected.append("\n\n").append(actual).toString();

  }

  private Map<String, HyperVectorValueIterator> addToHyperVectorMap(final List<QueryDataBatch> records,
      final RecordBatchLoader loader)
      throws SchemaChangeException, UnsupportedEncodingException {
    // TODO - this does not handle schema changes
    Map<String, HyperVectorValueIterator> combinedVectors = new TreeMap<>();

    long totalRecords = 0;
    QueryDataBatch batch;
    int size = records.size();
    for (int i = 0; i < size; i++) {
      batch = records.get(i);
      loader.load(batch.getHeader().getDef(), batch.getData());
      logger.debug("reading batch with " + loader.getRecordCount() + " rows, total read so far " + totalRecords);
      totalRecords += loader.getRecordCount();
      for (VectorWrapper<?> w : loader) {
        String field = SchemaPath.getSimplePath(w.getField().getName()).toExpr();
        if (!combinedVectors.containsKey(field)) {
          MaterializedField mf = w.getField();
          ValueVector[] vvList = (ValueVector[]) Array.newInstance(mf.getValueClass(), 1);
          vvList[0] = w.getValueVector();
          combinedVectors.put(field, new HyperVectorValueIterator(mf, new HyperVectorWrapper<>(mf, vvList)));
        } else {
          combinedVectors.get(field).getHyperVector().addVector(w.getValueVector());
        }

      }
    }
    for (HyperVectorValueIterator hvi : combinedVectors.values()) {
      hvi.determineTotalSize();
    }
    return combinedVectors;
  }

  private static class BatchIterator implements Iterable<VectorAccessible>, AutoCloseable {
    private final List<QueryDataBatch> dataBatches;
    private final RecordBatchLoader batchLoader;

    public BatchIterator(List<QueryDataBatch> dataBatches, RecordBatchLoader batchLoader) {
      this.dataBatches = dataBatches;
      this.batchLoader = batchLoader;
    }

    @Override
    public Iterator<VectorAccessible> iterator() {
      return new Iterator<VectorAccessible>() {

        int index = -1;

        @Override
        public boolean hasNext() {
          return index < dataBatches.size() - 1;
        }

        @Override
        public VectorAccessible next() {
          index++;
          if (index == dataBatches.size()) {
            throw new RuntimeException("Tried to call next when iterator had no more items.");
          }
          batchLoader.clear();
          QueryDataBatch batch = dataBatches.get(index);
          batchLoader.load(batch.getHeader().getDef(), batch.getData());
          return batchLoader;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException("Removing is not supported");
        }
      };
    }

    @Override
    public void close() throws Exception {
      batchLoader.clear();
    }

  }

  /**
   * Iterate over batches, and combine the batches into a map, where key is schema path, and value is
   * the list of column values across all the batches.
   * @param batches
   * @param expectedTotalRecords
   * @return
   * @throws SchemaChangeException
   * @throws UnsupportedEncodingException
   */
  public static Map<String, List<Object>> addToCombinedVectorResults(Iterable<VectorAccessible> batches,
                                                                     Long expectedBatchSize, Integer expectedNumBatches, Integer expectedTotalRecords)
      throws SchemaChangeException, UnsupportedEncodingException {
    Map<String, List<Object>> combinedVectors = new TreeMap<>();
    addToCombinedVectorResults(batches, null, expectedBatchSize, expectedNumBatches, combinedVectors, expectedTotalRecords);
    return combinedVectors;
  }

  /**
   * Add to result vectors and compare batch schema against expected schema while iterating batches.
   * @param batches
   * @param  expectedSchema: the expected schema the batches should contain. Through SchemaChangeException
   *                       if encounter different batch schema.
   * @param combinedVectors: the vectors to hold the values when iterate the batches.
   *
   * @return number of batches
   * @throws SchemaChangeException
   * @throws UnsupportedEncodingException
   */
  public static int addToCombinedVectorResults(Iterable<VectorAccessible> batches, BatchSchema expectedSchema,
                                               Long expectedBatchSize, Integer expectedNumBatches,
                                               Map<String, List<Object>> combinedVectors, Integer expectedTotalRecords)
       throws SchemaChangeException {
    // TODO - this does not handle schema changes
    int numBatch = 0;
    long totalRecords = 0;
    BatchSchema schema = null;

    for (VectorAccessible loader : batches)  {
      numBatch++;
      if (expectedSchema != null) {
        if (! expectedSchema.isEquivalent(loader.getSchema())) {
          throw new SchemaChangeException(String.format("Batch schema does not match expected schema\n" +
                  "Actual schema: %s.  Expected schema : %s",
              loader.getSchema(), expectedSchema));
        }
      }

      if (expectedBatchSize != null) {
        RecordBatchSizer sizer = new RecordBatchSizer(loader);
        // Not checking actualSize as accounting is not correct when we do
        // split and transfer ownership across operators.
        Assert.assertTrue(sizer.getNetBatchSize() <= expectedBatchSize);
      }

      if (schema == null) {
        schema = loader.getSchema();
        for (MaterializedField mf : schema) {
          combinedVectors.put(SchemaPath.getSimplePath(mf.getName()).toExpr(), new ArrayList<>());
        }
      } else {
        // TODO - actually handle schema changes, this is just to get access to the SelectionVectorMode
        // of the current batch, the check for a null schema is used to only mutate the schema once
        // need to add new vectors and null fill for previous batches? distinction between null and non-existence important?
        schema = loader.getSchema();
      }
      logger.debug("reading batch with " + loader.getRecordCount() + " rows, total read so far " + totalRecords);
      totalRecords += loader.getRecordCount();

      for (VectorWrapper<?> w : loader) {
        String field = SchemaPath.getSimplePath(w.getField().getName()).toExpr();
        ValueVector[] vectors;
        if (w.isHyper()) {
          vectors = w.getValueVectors();
        } else {
          vectors = new ValueVector[] {w.getValueVector()};
        }
        SelectionVector2 sv2 = null;
        SelectionVector4 sv4 = null;
        switch(schema.getSelectionVectorMode()) {
          case TWO_BYTE:
            sv2 = loader.getSelectionVector2();
            break;
          case FOUR_BYTE:
            sv4 = loader.getSelectionVector4();
            break;
          default:
        }
        if (sv4 != null) {
          for (int j = 0; j < sv4.getCount(); j++) {
            int complexIndex = sv4.get(j);
            int batchIndex = complexIndex >> 16;
            int recordIndexInBatch = complexIndex & 65535;
            Object obj = vectors[batchIndex].getAccessor().getObject(recordIndexInBatch);
            if (obj != null) {
              if (obj instanceof Text) {
                obj = obj.toString();
              }
            }
            combinedVectors.get(field).add(obj);
          }
        }
        else {
          for (ValueVector vv : vectors) {
            for (int j = 0; j < loader.getRecordCount(); j++) {
              int index;
              if (sv2 != null) {
                index = sv2.getIndex(j);
              } else {
                index = j;
              }
              Object obj = vv.getAccessor().getObject(index);
              if (obj != null) {
                if (obj instanceof Text) {
                  obj = obj.toString();
                }
              }
              combinedVectors.get(field).add(obj);
            }
          }
        }
      }
    }

    if (expectedNumBatches != null) {
      // Based on how much memory is actually taken by value vectors (because of doubling stuff),
      // we have to do complex math for predicting exact number of batches.
      // Instead, check that number of batches is at least the minimum that is expected
      // and no more than twice of that.
      Assert.assertTrue(numBatch >= expectedNumBatches);
      Assert.assertTrue(numBatch <= (2*expectedNumBatches));
    }

    if ( expectedTotalRecords != null ) {
      Assert.assertEquals(expectedTotalRecords.longValue(), totalRecords);
    }
    return numBatch;
  }

  protected void compareSchemaOnly() throws Exception {
    RecordBatchLoader loader = new RecordBatchLoader(getAllocator());
    List<QueryDataBatch> actual = null;
    QueryDataBatch batch = null;
    try {
      test(testOptionSettingQueries);
      actual = testRunAndReturn(queryType, query);
      batch = actual.get(0);
      loader.load(batch.getHeader().getDef(), batch.getData());

      final BatchSchema schema = loader.getSchema();
      final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = testBuilder.getExpectedSchema();
      if (schema.getFieldCount() != expectedSchema.size()) {
        throw new Exception("Expected and actual numbers of columns do not match.");
      }

      for (int i = 0; i < schema.getFieldCount(); ++i) {
        final String actualSchemaPath = schema.getColumn(i).getName();
        final TypeProtos.MajorType actualMajorType = schema.getColumn(i).getType();

        final String expectedSchemaPath = expectedSchema.get(i).getLeft().getRootSegmentPath();
        final TypeProtos.MajorType expectedMajorType = expectedSchema.get(i).getValue();

        if (! actualSchemaPath.equals(expectedSchemaPath) ||
            ! Types.isEquivalent(actualMajorType, expectedMajorType)) {
          throw new Exception(String.format("Schema path or type mismatch for column #%d:\n" +
                  "Expected schema path: %s\nActual   schema path: %s\nExpected type: %s\nActual   type: %s",
              i, expectedSchemaPath, actualSchemaPath, Types.toString(expectedMajorType),
              Types.toString(actualMajorType)));
        }
      }

    } finally {
      if (actual != null) {
        for (QueryDataBatch b : actual) {
          b.release();
        }
      }
      loader.clear();
    }
  }

  /**
   * Use this method only if necessary to validate one query against another. If
   * you are just validating against a baseline file use one of the simpler
   * interfaces that will write the validation query for you.
   *
   * @throws Exception
   */
  protected void compareUnorderedResults() throws Exception {
    RecordBatchLoader loader = new RecordBatchLoader(getAllocator());

    List<QueryDataBatch> actual = Collections.emptyList();
    List<QueryDataBatch> expected = Collections.emptyList();
    List<Map<String, Object>> expectedRecords = new ArrayList<>();
    List<Map<String, Object>> actualRecords = new ArrayList<>();

    try {
      test(testOptionSettingQueries);
      actual = testRunAndReturn(queryType, query);

      checkNumBatches(actual);

      addTypeInfoIfMissing(actual.get(0), testBuilder);
      addToMaterializedResults(actualRecords, actual, loader);

      // If actual result record number is 0,
      // and the baseline records does not exist, and baselineColumns provided,
      // compare actual column number/names with expected columns
      if (actualRecords.size() == 0
          && (baselineRecords == null || baselineRecords.size()==0)
          && baselineColumns != null) {
        checkColumnDef(loader.getSchema());
      }

      // If baseline data was not provided to the test builder directly, we must
      // run a query for the baseline, this includes
      // the cases where the baseline is stored in a file.
      if (baselineRecords == null) {
        if (expectedNumRecords != DrillTestWrapper.EXPECTED_NUM_RECORDS_NOT_SET) {
          Assert.assertEquals(expectedNumRecords, actualRecords.size());
          return;
        } else {
          test(baselineOptionSettingQueries);
          expected = testRunAndReturn(baselineQueryType, testBuilder.getValidationQuery());
          addToMaterializedResults(expectedRecords, expected, loader);
        }
      } else {
        expectedRecords = baselineRecords;
      }

      compareResults(expectedRecords, actualRecords);
    } finally {
      cleanupBatches(actual, expected);
    }
  }

  public void checkColumnDef(BatchSchema batchSchema) throws Exception{
    assert (batchSchema != null && batchSchema.getFieldCount()==baselineColumns.length);
    for (int i=0; i<baselineColumns.length; ++i) {
      if (!SchemaPath.parseFromString(baselineColumns[i]).equals(
          SchemaPath.parseFromString(batchSchema.getColumn(i).getName()))) {
        throw new Exception(i + "the expected column name is not matching: "
            + baselineColumns[i] + " is not " +
            batchSchema.getColumn(i).getName());
      }
    }
  }

  /**
   * Use this method only if necessary to validate one query against another. If you are just validating against a
   * baseline file use one of the simpler interfaces that will write the validation query for you.
   *
   * @throws Exception
   */
  protected void compareOrderedResults() throws Exception {
    if (highPerformanceComparison) {
      if (baselineQueryType == null) {
        throw new Exception("Cannot do a high performance comparison without using a baseline file");
      }
      compareResultsHyperVector();
    } else {
      compareMergedOnHeapVectors();
    }
  }

  public void compareMergedOnHeapVectors() throws Exception {
    RecordBatchLoader loader = new RecordBatchLoader(getAllocator());

    List<QueryDataBatch> actual = Collections.emptyList();
    List<QueryDataBatch> expected = Collections.emptyList();
    Map<String, List<Object>> actualSuperVectors;
    Map<String, List<Object>> expectedSuperVectors = null;

    try {
      test(testOptionSettingQueries);
      actual = testRunAndReturn(queryType, query);

      checkNumBatches(actual);

      // To avoid extra work for test writers, types can optionally be inferred from the test query
      addTypeInfoIfMissing(actual.get(0), testBuilder);

      BatchIterator batchIter = new BatchIterator(actual, loader);
      actualSuperVectors = addToCombinedVectorResults(batchIter, null, null, null);
      batchIter.close();

      // If baseline data was not provided to the test builder directly, we must run a query for the baseline, this includes
      // the cases where the baseline is stored in a file.
      if (baselineRecords == null) {
        if (baselineQueryType == null && baselineColumns != null) {
          checkAscendingOrdering(actualSuperVectors);
          return;
        } else {
          test(baselineOptionSettingQueries);
          expected = testRunAndReturn(baselineQueryType, testBuilder.getValidationQuery());
          BatchIterator exBatchIter = new BatchIterator(expected, loader);
          expectedSuperVectors = addToCombinedVectorResults(exBatchIter, null, null, null);
          exBatchIter.close();
        }
      } else {
        // data is built in the TestBuilder in a row major format as it is provided by the user
        // translate it here to vectorized, the representation expected by the ordered comparison
        expectedSuperVectors = translateRecordListToHeapVectors(baselineRecords);
      }

      compareMergedVectors(expectedSuperVectors, actualSuperVectors);
    } catch (Exception e) {
      throw new Exception(e.getMessage() + "\nFor query: " + query, e);
    } finally {
      cleanupBatches(expected, actual);
    }
  }

  private void checkAscendingOrdering(Map<String, List<Object>> results) {
    int numRecords = results.get(baselineColumns[0]).size();

    for (int index = 1; index < numRecords; index++) {
      int prevIndex = index - 1;

      for (String column: baselineColumns) {
        List<Object> objects = results.get(column);
        Object prevObject = objects.get(prevIndex);
        Object currentObject = objects.get(index);

        Assert.assertTrue(RowSetComparison.ObjectComparator.INSTANCE.compare(prevObject, currentObject) <= 0);
      }
    }
  }

  public static Map<String, List<Object>> translateRecordListToHeapVectors(List<Map<String, Object>> records) {
    Map<String, List<Object>> ret = new TreeMap<>();

    if (records == null) {
      return ret;
    }

    for (String s : records.get(0).keySet()) {
      ret.put(s, new ArrayList<>());
    }
    for (Map<String, Object> m : records) {
      for (String s : m.keySet()) {
        ret.get(s).add(m.get(s));
      }
    }
    return ret;
  }

  public void compareResultsHyperVector() throws Exception {
    RecordBatchLoader loader = new RecordBatchLoader(getAllocator());

    test(testOptionSettingQueries);
    List<QueryDataBatch> results = testRunAndReturn(queryType, query);

    checkNumBatches(results);

    // To avoid extra work for test writers, types can optionally be inferred from the test query
    addTypeInfoIfMissing(results.get(0), testBuilder);

    Map<String, HyperVectorValueIterator> actualSuperVectors = addToHyperVectorMap(results, loader);

    test(baselineOptionSettingQueries);
    List<QueryDataBatch> expected = testRunAndReturn(baselineQueryType, testBuilder.getValidationQuery());

    Map<String, HyperVectorValueIterator> expectedSuperVectors = addToHyperVectorMap(expected, loader);

    compareHyperVectors(expectedSuperVectors, actualSuperVectors);
    cleanupBatches(results, expected);
  }

  private void checkNumBatches(final List<QueryDataBatch> results) {
    if (expectedNumBatches != EXPECTED_BATCH_COUNT_NOT_SET) {
      final int actualNumBatches = results.size();
      assertEquals(String.format("Expected %d batches but query returned %d non empty batch(es)%n", expectedNumBatches,
          actualNumBatches), expectedNumBatches, actualNumBatches);
    }
  }

  private void addTypeInfoIfMissing(QueryDataBatch batch, TestBuilder testBuilder) {
    if (! testBuilder.typeInfoSet()) {
      Map<SchemaPath, TypeProtos.MajorType> typeMap = getTypeMapFromBatch(batch);
      testBuilder.baselineTypes(typeMap);
    }

  }

  private Map<SchemaPath, TypeProtos.MajorType> getTypeMapFromBatch(QueryDataBatch batch) {
    Map<SchemaPath, TypeProtos.MajorType> typeMap = new HashMap<>();
    for (int i = 0; i < batch.getHeader().getDef().getFieldCount(); i++) {
      typeMap.put(SchemaPath.getSimplePath(MaterializedField.create(batch.getHeader().getDef().getField(i)).getName()),
          batch.getHeader().getDef().getField(i).getMajorType());
    }
    return typeMap;
  }

  @SafeVarargs
  private final void cleanupBatches(List<QueryDataBatch>... results) {
    for (List<QueryDataBatch> resultList : results ) {
      for (QueryDataBatch result : resultList) {
        result.release();
      }
    }
  }

  public static void addToMaterializedResults(List<Map<String, Object>> materializedRecords,
                                              List<QueryDataBatch> records, RecordBatchLoader loader) {
    long totalRecords = 0;
    QueryDataBatch batch;
    int size = records.size();
    for (int i = 0; i < size; i++) {
      batch = records.get(0);
      loader.load(batch.getHeader().getDef(), batch.getData());
      logger.debug("reading batch with " + loader.getRecordCount() + " rows, total read so far " + totalRecords);
      totalRecords += loader.getRecordCount();
      for (int j = 0; j < loader.getRecordCount(); j++) {
        Map<String, Object> record = new TreeMap<>();
        for (VectorWrapper<?> w : loader) {
          Object obj = w.getValueVector().getAccessor().getObject(j);
          if (obj != null) {
            if (obj instanceof Text) {
              obj = obj.toString();
            }
            record.put(SchemaPath.getSimplePath(w.getField().getName()).toExpr(), obj);
          }
          record.put(SchemaPath.getSimplePath(w.getField().getName()).toExpr(), obj);
        }
        materializedRecords.add(record);
      }
      records.remove(0);
      batch.release();
      loader.clear();
    }
  }

  public static boolean compareValuesErrorOnMismatch(Object expected, Object actual, int counter, String column) throws Exception {

    if (compareValues(expected, actual, counter, column)) {
      return true;
    }
    if (expected == null) {
      throw new Exception("at position " + counter + " column '" + column + "' mismatched values, expected: null " +
        "but received " + actual + "(" + actual.getClass().getSimpleName() + ")");
    }
    if (actual == null) {
      throw new Exception("unexpected null at position " + counter + " column '" + column + "' should have been:  " + expected);
    }
    if (actual instanceof byte[]) {
      throw new Exception("at position " + counter + " column '" + column + "' mismatched values, expected: "
        + new String((byte[])expected, "UTF-8") + " but received " + new String((byte[])actual, "UTF-8"));
    }
    if (!expected.equals(actual)) {
      throw new Exception("at position " + counter + " column '" + column + "' mismatched values, expected: "
        + expected + "(" + expected.getClass().getSimpleName() + ") but received " + actual + "(" + actual.getClass().getSimpleName() + ")");
    }
    return true;
  }

  public static boolean compareValues(Object expected, Object actual, int counter, String column) throws Exception {
    return compareValues(expected, actual, counter, column, false, 0);
  }

  public static boolean compareValues(Object expected, Object actual, int counter, String column,
    boolean approximateEquality, double tolerance) throws Exception {
    if (expected == null) {
      if (actual == null) {
        if (VERBOSE_DEBUG) {
          logger.debug("(1) at position " + counter + " column '" + column + "' matched value:  " + expected );
        }
        return true;
      } else {
        return false;
      }
    }
    if (actual == null) {
      return false;
    }
    if (actual instanceof byte[]) {
      if (!Arrays.equals((byte[]) expected, (byte[]) actual)) {
        return false;
      } else {
        if (VERBOSE_DEBUG) {
          logger.debug("at position " + counter + " column '" + column + "' matched value " + new String((byte[])expected, "UTF-8"));
        }
        return true;
      }
    }
    if (!expected.equals(actual)) {
      if (approximateEquality && expected instanceof Number && actual instanceof Number) {
        if (expected instanceof BigDecimal && actual instanceof BigDecimal) {
          if (((((BigDecimal) expected).subtract((BigDecimal) actual)).abs().divide((BigDecimal) expected).abs()).compareTo(BigDecimal.valueOf(tolerance)) <= 0) {
            return true;
          }
        } else if (expected instanceof BigInteger && actual instanceof BigInteger) {
          BigDecimal expBD = new BigDecimal((BigInteger)expected);
          BigDecimal actBD = new BigDecimal((BigInteger)actual);
          if ((expBD.subtract(actBD)).abs().divide(expBD.abs()).compareTo(BigDecimal.valueOf(tolerance)) <= 0) {
            return true;
          }
        } else if (!(expected instanceof BigDecimal || expected instanceof BigInteger) && !(actual instanceof BigDecimal || actual instanceof BigInteger)) {
          // For all other types cast to double and compare
          if (Math.abs((double) expected - (double) actual) / Math.abs((double) expected) <= tolerance) {
            return true;
          }
        }
      }
      return false;
    } else {
      if (VERBOSE_DEBUG) {
        logger.debug("at position " + counter + " column '" + column + "' matched value:  " + expected );
      }
    }
    return true;
  }

  /**
   * Compare two result sets, ignoring ordering.
   *
   * @param expectedRecords - list of records from baseline
   * @param actualRecords - list of records from test query, WARNING - this list is destroyed in this method
   * @throws Exception
   */
  private void compareResults(List<Map<String, Object>> expectedRecords, List<Map<String, Object>> actualRecords) throws Exception {
    assertEquals("Different number of records returned", expectedRecords.size(), actualRecords.size());

    int i = 0;
    int counter = 0;
    boolean found;
    for (Map<String, Object> expectedRecord : expectedRecords) {
      i = 0;
      found = false;
      StringBuilder mismatchHistory = new StringBuilder();
      findMatch:
      for (Map<String, Object> actualRecord : actualRecords) {
        for (String s : actualRecord.keySet()) {
          if (!expectedRecord.containsKey(s)) {
            throw new Exception("Unexpected column '" + s + "' returned by query.");
          }
          if (! compareValues(expectedRecord.get(s), actualRecord.get(s), counter, s, approximateEquality, tolerance)) {
            i++;
            mismatchHistory.append("column: ").append(s)
                .append(" exp: |").append(expectedRecord.get(s))
                .append("| act: |").append(actualRecord.get(s)).append("|\n");
            continue findMatch;
          }
        }
        if (actualRecord.size() < expectedRecord.size()) {
          throw new Exception(findMissingColumns(expectedRecord.keySet(), actualRecord.keySet()));
        }
        found = true;
        break;
      }
      if (!found) {
        StringBuilder sb = new StringBuilder();
        for (int expectedRecordDisplayCount = 0;
             expectedRecordDisplayCount < 10 && expectedRecordDisplayCount < expectedRecords.size();
             expectedRecordDisplayCount++) {
          sb.append(printRecord(expectedRecords.get(expectedRecordDisplayCount)));
        }
        String expectedRecordExamples = sb.toString();
        sb.setLength(0);
        for (int actualRecordDisplayCount = 0;
             actualRecordDisplayCount < 10 && actualRecordDisplayCount < actualRecords.size();
             actualRecordDisplayCount++) {
          sb.append(printRecord(actualRecords.get(actualRecordDisplayCount)));
        }
        String actualRecordExamples = sb.toString();
        throw new Exception(String.format("After matching %d records, did not find expected record in result set:\n %s\n\n" +
                "Mismatch column: \n" + mismatchHistory + "\n" +
            "Some examples of expected records:\n%s\n\n Some examples of records returned by the test query:\n%s",
            counter, printRecord(expectedRecord), expectedRecordExamples, actualRecordExamples));
      } else {
        actualRecords.remove(i);
        counter++;
      }
    }
    assertEquals(0, actualRecords.size());
  }

  private static String findMissingColumns(Set<String> expected, Set<String> actual) {
    String missingCols = "";
    for (String colName : expected) {
      if (!actual.contains(colName)) {
        missingCols += colName + ", ";
      }
    }
    return "Expected column(s) " + missingCols + " not found in result set: " + actual + ".";
  }

  private String printRecord(Map<String, ?> record) {
    StringBuilder sb = new StringBuilder();
    record.keySet().stream().sorted()
        .forEach(key -> sb.append(key).append(" : ").append(record.get(key)).append(", "));
    return sb.append(System.lineSeparator()).toString();
  }

  private void test(String query) throws Exception {
    services.test(query);
  }

  private List<QueryDataBatch> testRunAndReturn(QueryType type, Object query) throws Exception {
    return services.testRunAndReturn(type, query);
  }
}
