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
package org.apache.drill.exec.store.parquet;

import static org.apache.drill.exec.store.parquet.TestFileGenerator.populateFieldInfoMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import io.netty.buffer.DrillBuf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.proto.BitControl;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.CachedSingleFileSystem;
import org.apache.drill.exec.store.TestOutputMutator;
import org.apache.drill.exec.store.parquet.columnreaders.ParquetRecordReader;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.util.Text;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.bytes.BytesInput;
import org.apache.parquet.column.page.DataPageV1;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.column.page.PageReader;
import org.apache.drill.exec.store.parquet.compression.DrillCompressionCodecFactory;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.hadoop.Footer;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.schema.MessageType;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

@Ignore
public class ParquetRecordReaderTest extends BaseTestQuery {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParquetRecordReaderTest.class);

  static final boolean VERBOSE_DEBUG = false;

  private static final int numberRowGroups = 1;
  private static final int recordsPerRowGroup = 300;
  private static int DEFAULT_BYTES_PER_PAGE = 1024 * 1024 * 1;
  private static final String fileName = "tmp/parquet_test_file_many_types";

  @BeforeClass
  public static void generateFile() throws Exception {
    final File f = new File(fileName);
    final ParquetTestProperties props =
        new ParquetTestProperties(numberRowGroups, recordsPerRowGroup, DEFAULT_BYTES_PER_PAGE, new HashMap<String, FieldInfo>());
    populateFieldInfoMap(props);
    if (!f.exists()) {
      TestFileGenerator.generateParquetFile(fileName, props);
    }
  }

  @Test
  public void testMultipleRowGroupsAndReads3() throws Exception {
    final String planName = "parquet/parquet_scan_screen.json";
    testParquetFullEngineLocalPath(planName, fileName, 2, numberRowGroups, recordsPerRowGroup);
  }

  public String getPlanForFile(String pathFileName, String parquetFileName) throws IOException {
    return Files.asCharSource(DrillFileUtils.getResourceAsFile(pathFileName), Charsets.UTF_8).read()
        .replaceFirst("&REPLACED_IN_PARQUET_TEST&", parquetFileName);
  }

  @Test
  public void testMultipleRowGroupsAndReads2() throws Exception {
    final StringBuilder readEntries = new StringBuilder();
    // number of times to read the file
    int i = 3;
    for (int j = 0; j < i; j++) {
      readEntries.append('"');
      readEntries.append(fileName);
      readEntries.append('"');
      if (j < i - 1) {
        readEntries.append(',');
      }
    }

    final String planText = Files.asCharSource(DrillFileUtils.getResourceAsFile(
        "parquet/parquet_scan_screen_read_entry_replace.json"), Charsets.UTF_8).read().replaceFirst(
            "&REPLACED_IN_PARQUET_TEST&", readEntries.toString());
    testParquetFullEngineLocalText(planText, fileName, i, numberRowGroups, recordsPerRowGroup, true);
  }

  @Test
  public void testDictionaryError() throws Exception {
    testFull(QueryType.SQL, "select L_RECEIPTDATE from dfs.`tmp/lineitem_null_dict.parquet`", "", 1, 1, 100000, false);
  }

  @Test
  public void testNullableAgg() throws Exception {
    final List<QueryDataBatch> result = testSqlWithResults(
        "select sum(a) as total_sum from dfs.`tmp/parquet_with_nulls_should_sum_100000_nulls_first.parquet`");
    assertEquals("Only expected one batch with data, and then the empty finishing batch.", 2, result.size());
    final RecordBatchLoader loader = new RecordBatchLoader(getDrillbitContext().getAllocator());

    final QueryDataBatch b = result.get(0);
    loader.load(b.getHeader().getDef(), b.getData());

    final VectorWrapper vw = loader.getValueAccessorById(
        NullableBigIntVector.class,
        loader.getValueVectorId(SchemaPath.getCompoundPath("total_sum")).getFieldIds()
    );
    assertEquals(4999950000l, vw.getValueVector().getAccessor().getObject(0));
    b.release();
    loader.clear();
  }

  @Test
  public void testNullableFilter() throws Exception {
    final List<QueryDataBatch> result = testSqlWithResults(
        "select count(wr_return_quantity) as row_count from dfs.`tmp/web_returns` where wr_return_quantity = 1");
    assertEquals("Only expected one batch with data, and then the empty finishing batch.", 2, result.size());
    final RecordBatchLoader loader = new RecordBatchLoader(getDrillbitContext().getAllocator());

    final QueryDataBatch b = result.get(0);
    loader.load(b.getHeader().getDef(), b.getData());

    final VectorWrapper vw = loader.getValueAccessorById(
        BigIntVector.class,
        loader.getValueVectorId(SchemaPath.getCompoundPath("row_count")).getFieldIds()
    );
    assertEquals(3573l, vw.getValueVector().getAccessor().getObject(0));
    b.release();
    loader.clear();
  }


  @Test
  public void testFixedBinary() throws Exception {
    final String readEntries = "\"tmp/drilltest/fixed_binary.parquet\"";
    final String planText = Files.asCharSource(DrillFileUtils.getResourceAsFile(
        "parquet/parquet_scan_screen_read_entry_replace.json"), Charsets.UTF_8).read()
          .replaceFirst( "&REPLACED_IN_PARQUET_TEST&", readEntries);
    testParquetFullEngineLocalText(planText, fileName, 1, 1, 1000000, false);
  }

  @Test
  public void testNonNullableDictionaries() throws Exception {
    testFull(QueryType.SQL,
        "select * from dfs.`tmp/drilltest/non_nullable_dictionary.parquet`", "", 1, 1, 30000000, false);
  }

  @Test
  public void testNullableVarCharMemory() throws Exception {
    testFull(QueryType.SQL,
        "select s_comment,s_suppkey from dfs.`tmp/sf100_supplier.parquet`", "", 1, 1, 1000, false);
  }

  @Test
  public void testReadVoter() throws Exception {
    testFull(QueryType.SQL, "select * from dfs.`tmp/voter.parquet`", "", 1, 1, 1000, false);
  }

  @Test
  public void testDrill_1314() throws Exception {
    testFull(QueryType.SQL, "select l_partkey " +
        "from dfs.`tmp/drill_1314.parquet`", "", 1, 1, 10000, false);
  }

  @Test
  public void testDrill_1314_all_columns() throws Exception {
    testFull(QueryType.SQL, "select * from dfs.`tmp/drill_1314.parquet`", "", 1, 1, 10000, false);
  }

  @Test
  public void testDictionaryError_419() throws Exception {
    testFull(QueryType.SQL,
        "select c_address from dfs.`tmp/customer_snappyimpala_drill_419.parquet`", "", 1, 1, 150000, false);
  }

  @Test
  public void testNonExistentColumn() throws Exception {
    testFull(QueryType.SQL,
        "select non_existent_column from cp.`tpch/nation.parquet`", "", 1, 1, 150000, false);
  }


  @Test

  public void testNonExistentColumnLargeFile() throws Exception {
    testFull(QueryType.SQL,
        "select non_existent_column, non_existent_col_2 from dfs.`tmp/customer.dict.parquet`", "", 1, 1, 150000, false);
  }

  @Test

  public void testNonExistentColumnsSomePresentColumnsLargeFile() throws Exception {
    testFull(QueryType.SQL,
        "select cust_key, address,  non_existent_column, non_existent_col_2 from dfs.`tmp/customer.dict.parquet`",
        "", 1, 1, 150000, false);
  }

  @Ignore // ignored for now for performance
  @Test
  public void testTPCHPerformace_SF1() throws Exception {
    testFull(QueryType.SQL,
        "select * from dfs.`tmp/orders_part-m-00001.parquet`", "", 1, 1, 150000, false);
  }

  @Test
  public void testLocalDistributed() throws Exception {
    final String planName = "parquet/parquet_scan_union_screen_physical.json";
    testParquetFullEngineLocalTextDistributed(planName, fileName, 1, numberRowGroups, recordsPerRowGroup);
  }

  @Test
  @Ignore
  public void testRemoteDistributed() throws Exception {
    final String planName = "parquet/parquet_scan_union_screen_physical.json";
    testParquetFullEngineRemote(planName, fileName, 1, numberRowGroups, recordsPerRowGroup);
  }


  public void testParquetFullEngineLocalPath(String planFileName, String filename,
      int numberOfTimesRead /* specified in json plan */,
      int numberOfRowGroups, int recordsPerRowGroup) throws Exception {
    testParquetFullEngineLocalText(Files.asCharSource(DrillFileUtils.getResourceAsFile(planFileName), Charsets.UTF_8).read(), filename,
        numberOfTimesRead, numberOfRowGroups, recordsPerRowGroup, true);
  }

  //specific tests should call this method, but it is not marked as a test itself intentionally
  public void testParquetFullEngineLocalText(String planText, String filename,
      int numberOfTimesRead /* specified in json plan */,
      int numberOfRowGroups, int recordsPerRowGroup, boolean testValues) throws Exception {
    testFull(QueryType.LOGICAL, planText, filename, numberOfTimesRead, numberOfRowGroups, recordsPerRowGroup, testValues);
  }

  private void testFull(QueryType type, String planText, String filename,
      int numberOfTimesRead /* specified in json plan */,
      int numberOfRowGroups, int recordsPerRowGroup, boolean testValues) throws Exception {

    // final RecordBatchLoader batchLoader = new RecordBatchLoader(getAllocator());
    final HashMap<String, FieldInfo> fields = new HashMap<>();
    final ParquetTestProperties props =
        new ParquetTestProperties(numberRowGroups, recordsPerRowGroup, DEFAULT_BYTES_PER_PAGE, fields);
    TestFileGenerator.populateFieldInfoMap(props);
    final ParquetResultListener resultListener =
        new ParquetResultListener(getAllocator(), props, numberOfTimesRead, testValues);
    final Stopwatch watch = Stopwatch.createStarted();
    testWithListener(type, planText, resultListener);
    resultListener.getResults();
  }

  //use this method to submit physical plan
  public void testParquetFullEngineLocalTextDistributed(String planName, String filename,
      int numberOfTimesRead /* specified in json plan */,
      int numberOfRowGroups, int recordsPerRowGroup) throws Exception {
    String planText = Files.asCharSource(DrillFileUtils.getResourceAsFile(planName), Charsets.UTF_8).read();
    testFull(QueryType.PHYSICAL, planText, filename, numberOfTimesRead, numberOfRowGroups, recordsPerRowGroup, true);
  }

  public String pad(String value, int length) {
    return pad(value, length, " ");
  }

  public String pad(String value, int length, String with) {
    final StringBuilder result = new StringBuilder(length);
    result.append(value);

    while (result.length() < length) {
      result.insert(0, with);
    }

    return result.toString();
  }

  public void testParquetFullEngineRemote(String plan, String filename,
      int numberOfTimesRead /* specified in json plan */,
      int numberOfRowGroups, int recordsPerRowGroup) throws Exception {
    final HashMap<String, FieldInfo> fields = new HashMap<>();
    final ParquetTestProperties props =
        new ParquetTestProperties(numberRowGroups, recordsPerRowGroup, DEFAULT_BYTES_PER_PAGE, fields);
    TestFileGenerator.populateFieldInfoMap(props);
    final ParquetResultListener resultListener =
        new ParquetResultListener(getAllocator(), props, numberOfTimesRead, true);
    testWithListener(QueryType.PHYSICAL, Files.asCharSource(DrillFileUtils.getResourceAsFile(plan), Charsets.UTF_8).read(), resultListener);
    resultListener.getResults();
  }

 private static class MockOutputMutator implements OutputMutator {
    private final List<MaterializedField> removedFields = Lists.newArrayList();
    private final List<ValueVector> addFields = Lists.newArrayList();

    List<MaterializedField> getRemovedFields() {
      return removedFields;
    }

    List<ValueVector> getAddFields() {
      return addFields;
    }

    @Override
    public <T extends ValueVector> T addField(MaterializedField field, Class<T> clazz) throws SchemaChangeException {
      return null;
    }

    @Override
    public void allocate(int recordCount) {
    }

    @Override
    public boolean isNewSchema() {
      return false;
    }

    @Override
    public DrillBuf getManagedBuffer() {
      return allocator.buffer(255);
    }

   @Override
   public CallBack getCallBack() {
     return null;
   }

   @Override
   public void clear() {
     // Nothing to do!
   }
 }

  private void validateFooters(final List<Footer> metadata) {
    logger.debug(metadata.toString());
    assertEquals(3, metadata.size());
    for (Footer footer : metadata) {
      final File file = new File(footer.getFile().toUri());
      assertTrue(file.getName(), file.getName().startsWith("part"));
      assertTrue(file.getPath(), file.exists());
      final ParquetMetadata parquetMetadata = footer.getParquetMetadata();
      assertEquals(2, parquetMetadata.getBlocks().size());
      final Map<String, String> keyValueMetaData = parquetMetadata.getFileMetaData().getKeyValueMetaData();
      assertEquals("bar", keyValueMetaData.get("foo"));
      assertEquals(footer.getFile().getName(), keyValueMetaData.get(footer.getFile().getName()));
    }
  }

  private void validateContains(MessageType schema, PageReadStore pages, String[] path, int values, BytesInput bytes)
      throws IOException {
    PageReader pageReader = pages.getPageReader(schema.getColumnDescription(path));
    DataPageV1 page = (DataPageV1) pageReader.readPage();
    assertEquals(values, page.getValueCount());
    assertArrayEquals(bytes.toByteArray(), page.getBytes().toByteArray());
  }

  @Test
  public void testMultipleRowGroups() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(2, 300, DEFAULT_BYTES_PER_PAGE, fields);
    populateFieldInfoMap(props);
    testParquetFullEngineEventBased(true, "parquet/parquet_scan_screen.json", "/tmp/test.parquet", 1, props);
  }

  // TODO - Test currently marked ignore to prevent breaking of the build process, requires a binary file that was
  // generated using pig. Will need to find a good place to keep files like this.
  // For now I will upload it to the JIRA as an attachment.
  @Test
  public void testNullableColumns() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(1, 1500000, DEFAULT_BYTES_PER_PAGE, fields);
    Object[] boolVals = {true, null, null};
    props.fields.put("a", new FieldInfo("boolean", "a", 1, boolVals, TypeProtos.MinorType.BIT, props));
    testParquetFullEngineEventBased(false, "parquet/parquet_nullable.json", "/tmp/nullable_test.parquet", 1, props);
  }


  @Test
  /**
   * Tests the reading of nullable var length columns, runs the tests twice, once on a file that has
   * a converted type of UTF-8 to make sure it can be read
   */
  public void testNullableColumnsVarLen() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(1, 300000, DEFAULT_BYTES_PER_PAGE, fields);
    byte[] val = {'b'};
    byte[] val2 = {'b', '2'};
    byte[] val3 = {'b', '3'};
    byte[] val4 = { 'l','o','n','g','e','r',' ','s','t','r','i','n','g'};
    Object[] byteArrayVals = { val, val2, val4};
    props.fields.put("a", new FieldInfo("boolean", "a", 1, byteArrayVals, TypeProtos.MinorType.BIT, props));
    testParquetFullEngineEventBased(false, "parquet/parquet_nullable_varlen.json", "/tmp/nullable_varlen.parquet", 1, props);
    HashMap<String, FieldInfo> fields2 = new HashMap<>();
    // pass strings instead of byte arrays
    Object[] textVals = { new org.apache.drill.exec.util.Text("b"), new org.apache.drill.exec.util.Text("b2"),
        new org.apache.drill.exec.util.Text("b3") };
    ParquetTestProperties props2 = new ParquetTestProperties(1, 30000, DEFAULT_BYTES_PER_PAGE, fields2);
    props2.fields.put("a", new FieldInfo("boolean", "a", 1, textVals, TypeProtos.MinorType.BIT, props2));
    testParquetFullEngineEventBased(false, "parquet/parquet_scan_screen_read_entry_replace.json",
        "\"/tmp/varLen.parquet/a\"", "unused", 1, props2);

  }


  @Test
  public void testFileWithNulls() throws Exception {
    HashMap<String, FieldInfo> fields3 = new HashMap<>();
    ParquetTestProperties props3 = new ParquetTestProperties(1, 3000, DEFAULT_BYTES_PER_PAGE, fields3);
    // actually include null values
    Object[] valuesWithNull = {new Text(""), new Text("longer string"), null};
    props3.fields.put("a", new FieldInfo("boolean", "a", 1, valuesWithNull, TypeProtos.MinorType.BIT, props3));
    testParquetFullEngineEventBased(false, "parquet/parquet_scan_screen_read_entry_replace.json",
        "\"/tmp/nullable_with_nulls.parquet\"", "unused", 1, props3);

  }


  @Test
  public void testDictionaryEncoding() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(1, 25, DEFAULT_BYTES_PER_PAGE, fields);
    Object[] boolVals = null;
    props.fields.put("n_name", null);
    props.fields.put("n_nationkey", null);
    props.fields.put("n_regionkey", null);
    props.fields.put("n_comment", null);
    testParquetFullEngineEventBased(false, false, "parquet/parquet_scan_screen_read_entry_replace.json",
        "\"/tmp/nation_dictionary_fail.parquet\"", "unused", 1, props, QueryType.LOGICAL);

    fields = new HashMap<>();
    props = new ParquetTestProperties(1, 5, DEFAULT_BYTES_PER_PAGE, fields);
    props.fields.put("employee_id", null);
    props.fields.put("name", null);
    props.fields.put("role", null);
    props.fields.put("phone", null);
    props.fields.put("password_hash", null);
    props.fields.put("gender_male", null);
    props.fields.put("height", null);
    props.fields.put("hair_thickness", null);
    testParquetFullEngineEventBased(false, false, "parquet/parquet_scan_screen_read_entry_replace.json",
        "\"/tmp/employees_5_16_14.parquet\"", "unused", 1, props, QueryType.LOGICAL);
  }

  @Test
  public void testMultipleRowGroupsAndReads() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(4, 3000, DEFAULT_BYTES_PER_PAGE, fields);
    populateFieldInfoMap(props);
    String readEntries = "";
    // number of times to read the file
    int i = 3;
    for (int j = 0; j < i; j++) {
      readEntries += "\"/tmp/test.parquet\"";
      if (j < i - 1) {
        readEntries += ",";
      }
    }
    testParquetFullEngineEventBased(true, "parquet/parquet_scan_screen_read_entry_replace.json", readEntries,
        "/tmp/test.parquet", i, props);
  }


  @Test
  public void testReadError_Drill_901() throws Exception {
    // select cast( L_COMMENT as varchar) from  dfs.`/tmp/drilltest/employee_parquet`
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(1, 60175, DEFAULT_BYTES_PER_PAGE, fields);
    testParquetFullEngineEventBased(false, false, "parquet/par_writer_test.json", null,
        "unused, no file is generated", 1, props, QueryType.PHYSICAL);
  }


  @Test
  public void testReadError_Drill_839() throws Exception {
    // select cast( L_COMMENT as varchar) from  dfs.`/tmp/drilltest/employee_parquet`
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(1, 150000, DEFAULT_BYTES_PER_PAGE, fields);
    String readEntries = "\"/tmp/customer_nonull.parquet\"";
    testParquetFullEngineEventBased(false, false, "parquet/parquet_scan_screen_read_entry_replace.json", readEntries,
        "unused, no file is generated", 1, props, QueryType.LOGICAL);
  }


  @Test
  public void testReadBug_Drill_418() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(1, 150000, DEFAULT_BYTES_PER_PAGE, fields);
    TestFileGenerator.populateDrill_418_fields(props);
    String readEntries = "\"/tmp/customer.plain.parquet\"";
    testParquetFullEngineEventBased(false, false, "parquet/parquet_scan_screen_read_entry_replace.json", readEntries,
        "unused, no file is generated", 1, props, QueryType.LOGICAL);
  }

  // requires binary file generated by pig from TPCH data, also have to disable assertion where data is coming in

  @Test
  public void testMultipleRowGroupsAndReadsPigError() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(1, 1500000, DEFAULT_BYTES_PER_PAGE, fields);
    TestFileGenerator.populatePigTPCHCustomerFields(props);
    String readEntries = "\"/tmp/tpc-h/customer\"";
    testParquetFullEngineEventBased(false, false, "parquet/parquet_scan_screen_read_entry_replace.json", readEntries,
        "unused, no file is generated", 1, props, QueryType.LOGICAL);

    fields = new HashMap<>();
    props = new ParquetTestProperties(1, 100000, DEFAULT_BYTES_PER_PAGE, fields);
    TestFileGenerator.populatePigTPCHSupplierFields(props);
    readEntries = "\"tmp/tpc-h/supplier\"";
    testParquetFullEngineEventBased(false, false, "parquet/parquet_scan_screen_read_entry_replace.json", readEntries,
        "unused, no file is generated", 1, props, QueryType.LOGICAL);
  }

  @Test
  public void test958_sql() throws Exception {
    // testFull(QueryType.SQL, "select ss_ext_sales_price from dfs.`/tmp/store_sales`", "", 1, 1, 30000000, false);
    testFull(QueryType.SQL, "select * from dfs.`tmp/store_sales`", "", 1, 1, 30000000, false);
  }

  @Test
  public void drill_958bugTest() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(1, 2880404, DEFAULT_BYTES_PER_PAGE, fields);
    TestFileGenerator.populatePigTPCHCustomerFields(props);
    String readEntries = "\"tmp/store_sales\"";
    testParquetFullEngineEventBased(false, false, "parquet/parquet_scan_screen_read_entry_replace.json", readEntries,
        "unused, no file is generated", 1, props, QueryType.LOGICAL);
  }

  @Test
  public void testMultipleRowGroupsEvent() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(2, 300, DEFAULT_BYTES_PER_PAGE, fields);
    populateFieldInfoMap(props);
    testParquetFullEngineEventBased(true, "parquet/parquet_scan_screen.json", "tmp/test.parquet", 1, props);
  }


  /**
   * Tests the attribute in a scan node to limit the columns read by a scan.
   *
   * The functionality of selecting all columns is tested in all of the other tests that leave out the attribute.
   * @throws Exception
   */
  @Test
  public void testSelectColumnRead() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(4, 3000, DEFAULT_BYTES_PER_PAGE, fields);
    // generate metatdata for a series of test columns, these columns are all generated in the test file
    populateFieldInfoMap(props);
    TestFileGenerator.generateParquetFile("/tmp/test.parquet", props);
    fields.clear();
    // create a new object to describe the dataset expected out of the scan operation
    // the fields added below match those requested in the plan specified in parquet_selective_column_read.json
    // that is used below in the test query
    props = new ParquetTestProperties(4, 3000, DEFAULT_BYTES_PER_PAGE, fields);
    props.fields.put("integer", new FieldInfo("int32", "integer", 32, TestFileGenerator.intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("bigInt", new FieldInfo("int64", "bigInt", 64, TestFileGenerator.longVals, TypeProtos.MinorType.BIGINT, props));
    props.fields.put("bin", new FieldInfo("binary", "bin", -1, TestFileGenerator.binVals, TypeProtos.MinorType.VARBINARY, props));
    props.fields.put("bin2", new FieldInfo("binary", "bin2", -1, TestFileGenerator.bin2Vals, TypeProtos.MinorType.VARBINARY, props));
    testParquetFullEngineEventBased(true, false, "/parquet/parquet_selective_column_read.json", null, "/tmp/test.parquet", 1, props, QueryType.PHYSICAL);
  }

  @Test
  @Ignore
  public void testPerformance() throws Exception {
    final DrillbitContext bitContext = mock(DrillbitContext.class);
    final UserClientConnection connection = mock(UserClientConnection.class);

    final DrillConfig c = DrillConfig.create();
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, BitControl.PlanFragment.getDefaultInstance(), connection, registry);

    final Path fileName = new Path("/tmp/parquet_test_performance.parquet");
    final HashMap<String, FieldInfo> fields = new HashMap<>();
    final ParquetTestProperties props = new ParquetTestProperties(1, 20 * 1000 * 1000, DEFAULT_BYTES_PER_PAGE, fields);
    populateFieldInfoMap(props);

    final Configuration dfsConfig = new Configuration();
    final List<Footer> footers = ParquetFileReader.readFooters(dfsConfig, fileName);
    final Footer f = footers.iterator().next();

    final List<SchemaPath> columns = Lists.newArrayList();
    columns.add(new SchemaPath("_MAP.integer", ExpressionPosition.UNKNOWN));
    columns.add(new SchemaPath("_MAP.bigInt", ExpressionPosition.UNKNOWN));
    columns.add(new SchemaPath("_MAP.f", ExpressionPosition.UNKNOWN));
    columns.add(new SchemaPath("_MAP.d", ExpressionPosition.UNKNOWN));
    columns.add(new SchemaPath("_MAP.b", ExpressionPosition.UNKNOWN));
    columns.add(new SchemaPath("_MAP.bin", ExpressionPosition.UNKNOWN));
    columns.add(new SchemaPath("_MAP.bin2", ExpressionPosition.UNKNOWN));
    int totalRowCount = 0;

    final FileSystem fs = new CachedSingleFileSystem(fileName);
    final BufferAllocator allocator = RootAllocatorFactory.newRoot(c);
    for(int i = 0; i < 25; i++) {
      CompressionCodecFactory ccf = DrillCompressionCodecFactory.createDirectCodecFactory(
        dfsConfig,
        new ParquetDirectByteBufferAllocator(allocator),
        0
      );
      final ParquetRecordReader rr = new ParquetRecordReader(context, fileName, 0, fs,
          ccf,
          f.getParquetMetadata(), columns, ParquetReaderUtility.DateCorruptionStatus.META_SHOWS_CORRUPTION);
      final TestOutputMutator mutator = new TestOutputMutator(allocator);
      rr.setup(null, mutator);
      final Stopwatch watch = Stopwatch.createStarted();

      int rowCount = 0;
      while ((rowCount = rr.next()) > 0) {
        totalRowCount += rowCount;
      }
      rr.close();
    }

    allocator.close();
  }

  // specific tests should call this method, but it is not marked as a test itself intentionally
  public void testParquetFullEngineEventBased(boolean generateNew, String plan, String readEntries, String filename,
      int numberOfTimesRead /* specified in json plan */, ParquetTestProperties props) throws Exception {
    testParquetFullEngineEventBased(true, generateNew, plan, readEntries,filename,
        numberOfTimesRead /* specified in json plan */, props, QueryType.LOGICAL);
  }


  // specific tests should call this method, but it is not marked as a test itself intentionally
  public void testParquetFullEngineEventBased(boolean generateNew, String plan, String filename,
      int numberOfTimesRead /* specified in json plan */, ParquetTestProperties props) throws Exception {
    testParquetFullEngineEventBased(true, generateNew, plan, null, filename, numberOfTimesRead, props, QueryType.LOGICAL);
  }

  // specific tests should call this method, but it is not marked as a test itself intentionally
  public void testParquetFullEngineEventBased(boolean testValues, boolean generateNew, String plan,
      String readEntries, String filename,
      int numberOfTimesRead /* specified in json plan */, ParquetTestProperties props,
      QueryType queryType) throws Exception {
    if (generateNew) {
      TestFileGenerator.generateParquetFile(filename, props);
    }

    final ParquetResultListener resultListener = new ParquetResultListener(getAllocator(), props, numberOfTimesRead, testValues);
    final long C = System.nanoTime();
    String planText = Files.asCharSource(DrillFileUtils.getResourceAsFile(plan), Charsets.UTF_8).read();
    // substitute in the string for the read entries, allows reuse of the plan file for several tests
    if (readEntries != null) {
      planText = planText.replaceFirst( "&REPLACED_IN_PARQUET_TEST&", readEntries);
    }
    testWithListener(queryType, planText, resultListener);
    resultListener.getResults();
  }

  @Test
  public void testLimit() throws Exception {
    List<QueryDataBatch> results = testSqlWithResults("SELECT * FROM cp.`parquet/tpch/nation/01.parquet` LIMIT 1");

    int recordsInOutput = 0;
    for (QueryDataBatch batch : results) {
      recordsInOutput += batch.getHeader().getDef().getRecordCount();
      batch.release();
    }

    assertTrue(String.format("Number of records in output is wrong: expected=%d, actual=%s", 1, recordsInOutput), 1 == recordsInOutput);
  }

  @Test
  public void testLimitBeyondRowCount() throws Exception {
    List<QueryDataBatch> results = testSqlWithResults("SELECT * FROM cp.`parquet/tpch/nation/01.parquet` LIMIT 100");

    int recordsInOutput = 0;
    for (QueryDataBatch batch : results) {
      recordsInOutput += batch.getHeader().getDef().getRecordCount();
      batch.release();
    }

    assertTrue(String.format("Number of records in output is wrong: expected=%d, actual=%s", 9, recordsInOutput), 9 == recordsInOutput);
  }

  @Test
  public void testLimitMultipleRowGroups() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(3, 100, 1024 * 1024, fields);
    populateFieldInfoMap(props);
    TestFileGenerator.generateParquetFile("/tmp/testLimit.parquet", props);

    List<QueryDataBatch> results = testSqlWithResults("SELECT * FROM dfs.`/tmp/testLimit.parquet` LIMIT 225");

    int recordsInOutput = 0;
    for (QueryDataBatch batch : results) {
      recordsInOutput += batch.getHeader().getDef().getRecordCount();
      batch.release();
    }

    assertTrue(String.format("Number of records in output is wrong: expected=%d, actual=%s", 225, recordsInOutput), 225 == recordsInOutput);
  }

  @Test
  public void testLimitMultipleRowGroupsBeyondRowCount() throws Exception {
    HashMap<String, FieldInfo> fields = new HashMap<>();
    ParquetTestProperties props = new ParquetTestProperties(3, 100, 1024 * 1024, fields);
    populateFieldInfoMap(props);
    TestFileGenerator.generateParquetFile("/tmp/testLimit.parquet", props);

    List<QueryDataBatch> results = testSqlWithResults("SELECT * FROM dfs.`/tmp/testLimit.parquet` LIMIT 500");

    int recordsInOutput = 0;
    for (QueryDataBatch batch : results) {
      recordsInOutput += batch.getHeader().getDef().getRecordCount();
      batch.release();
    }

    assertTrue(String.format("Number of records in output is wrong: expected=%d, actual=%s", 300, recordsInOutput), 300 == recordsInOutput);
  }

}
