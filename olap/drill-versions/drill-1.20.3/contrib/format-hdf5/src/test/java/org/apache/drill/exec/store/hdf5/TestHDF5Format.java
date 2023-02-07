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

package org.apache.drill.exec.store.hdf5;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.apache.drill.test.QueryTestUtil.generateCompressedFile;

@Category(RowSetTests.class)
public class TestHDF5Format extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);

    dirTestWatcher.copyResourceToRoot(Paths.get("hdf5/"));
  }

  @Test
  public void testExplicitQuery() throws Exception {
    String sql = "SELECT path, data_type, file_name FROM dfs.`hdf5/dset.h5`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("path", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("data_type", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("file_name", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("/dset", "DATASET", "dset.h5")
      .build();
    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testStarQuery() throws Exception {
    List<Integer> t1 = Arrays.asList(1, 2, 3, 4, 5, 6);
    List<Integer> t2 = Arrays.asList(7, 8, 9, 10, 11, 12);
    List<Integer> t3 = Arrays.asList(13, 14, 15, 16, 17, 18);
    List<Integer> t4 = Arrays.asList(19, 20, 21, 22, 23, 24);
    List<List<Integer>> finalList = new ArrayList<>();
    finalList.add(t1);
    finalList.add(t2);
    finalList.add(t3);
    finalList.add(t4);

    testBuilder()
      .sqlQuery("SELECT * FROM dfs.`hdf5/dset.h5`")
      .unOrdered()
      .baselineColumns("path", "data_type", "file_name", "data_size", "element_count", "dataset_data_type", "dimensions", "int_data", "is_link")
      .baselineValues("/dset", "DATASET", "dset.h5", 96L, 24L, "int", "[4, 6]", finalList, false)
      .go();
  }

  @Test
  public void testSimpleExplicitQuery() throws Exception {
    List<Integer> t1 = Arrays.asList(1, 2, 3, 4, 5, 6);
    List<Integer> t2 = Arrays.asList(7, 8, 9, 10, 11, 12);
    List<Integer> t3 = Arrays.asList(13, 14, 15, 16, 17, 18);
    List<Integer> t4 = Arrays.asList(19, 20, 21, 22, 23, 24);
    List<List<Integer>> finalList = new ArrayList<>();
    finalList.add(t1);
    finalList.add(t2);
    finalList.add(t3);
    finalList.add(t4);

    testBuilder()
      .sqlQuery("SELECT path, data_type, file_name, int_data FROM dfs.`hdf5/dset.h5`")
      .unOrdered()
      .baselineColumns("path", "data_type", "file_name", "int_data")
      .baselineValues("/dset", "DATASET", "dset.h5", finalList)
      .go();
  }

  @Test
  public void testStarQueryWithoutPreview() throws Exception {
    String sql = "SELECT * FROM table(dfs.`hdf5/dset.h5` (type => 'hdf5', showPreview => false))";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("path", MinorType.VARCHAR, DataMode.OPTIONAL)
      .add("data_type", MinorType.VARCHAR, DataMode.OPTIONAL)
      .add("file_name", MinorType.VARCHAR, DataMode.OPTIONAL)
      .add("data_size", MinorType.BIGINT, DataMode.OPTIONAL)
      .add("is_link", MinorType.BIT, DataMode.OPTIONAL)
      .add("element_count", MinorType.BIGINT, DataMode.OPTIONAL)
      .add("dataset_data_type", MinorType.VARCHAR, DataMode.OPTIONAL)
      .add("dimensions", MinorType.VARCHAR, DataMode.OPTIONAL)
      .build();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("/dset", "DATASET", "dset.h5", 96, false, 24, "int", "[4, 6]")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testFlattenColumnQuery() throws RpcException {
    String sql = "SELECT data[0] AS col1,\n" +
            "data[1] as col2,\n" +
            "data[2] as col3\n" +
            "FROM \n" +
            "(\n" +
            "SELECT FLATTEN(double_data) AS data \n" +
            "FROM dfs.`hdf5/browsing.h5` WHERE path='/groupB/dmat'\n" +
            ")";
    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("col1", MinorType.FLOAT8, DataMode.OPTIONAL)
      .add("col2", MinorType.FLOAT8, DataMode.OPTIONAL)
      .add("col3", MinorType.FLOAT8, DataMode.OPTIONAL)
      .buildSchema();
    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1.1, 2.2, 3.3)
      .addRow(4.4, 5.5, 6.6)
      .addRow(7.7, 8.8, 9.9)
      .build();
    new RowSetComparison(expected).verifyAndClearAll(results);
  }

 @Test
  public void testFilterWithNonProjectedFieldQuery() throws Exception {
    String sql = "SELECT `path` FROM dfs.`hdf5/browsing.h5` WHERE data_type='DATASET'";

   RowSet results = client.queryBuilder().sql(sql).rowSet();
   TupleMetadata expectedSchema = new SchemaBuilder()
     .add("path", MinorType.VARCHAR, DataMode.OPTIONAL)
     .buildSchema();

   RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
     .addRow("/groupA/date")
     .addRow("/groupA/string")
     .addRow("/groupB/dmat")
     .addRow("/groupB/inarr")
     .build();
   new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testFloat32ScalarQuery() throws Exception {
    String sql = "SELECT flatten(float32) AS float_col\n" +
            "FROM dfs.`hdf5/scalar.h5`\n" +
            "WHERE path='/datatype/float32'";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("float_col", TypeProtos.MinorType.FLOAT4, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-3.4028234663852886E38)
      .addRow(1.0)
      .addRow(2.0)
      .addRow(3.0)
      .addRow(4.0)
      .addRow(5.0)
      .addRow(6.0)
      .addRow(7.0)
      .addRow(8.0)
      .addRow(3.4028234663852886E38)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testFlattenFloat32ScalarQuery() throws Exception {
    String sql = "SELECT * FROM table(dfs.`hdf5/scalar.h5` (type => 'hdf5', defaultPath => '/datatype/float32'))";
    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("float32", TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-3.4028234663852886E38)
      .addRow(1.0)
      .addRow(2.0)
      .addRow(3.0)
      .addRow(4.0)
      .addRow(5.0)
      .addRow(6.0)
      .addRow(7.0)
      .addRow(8.0)
      .addRow(3.4028234663852886E38)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testFloat64ScalarQuery() throws Exception {
    String sql = "SELECT flatten(float64) AS float_col\n" +
            "FROM dfs.`hdf5/scalar.h5`\n" +
            "WHERE path='/datatype/float64'";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("float_col", TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-1.7976931348623157E308)
      .addRow(1.0)
      .addRow(2.0)
      .addRow(3.0)
      .addRow(4.0)
      .addRow(5.0)
      .addRow(6.0)
      .addRow(7.0)
      .addRow(8.0)
      .addRow(1.7976931348623157E308)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testFlattenFloat64ScalarQuery() throws Exception {
    String sql = "SELECT * FROM table(dfs.`hdf5/scalar.h5` (type => 'hdf5', defaultPath => '/datatype/float64'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("float64", TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-1.7976931348623157E308)
      .addRow(1.0)
      .addRow(2.0)
      .addRow(3.0)
      .addRow(4.0)
      .addRow(5.0)
      .addRow(6.0)
      .addRow(7.0)
      .addRow(8.0)
      .addRow(1.7976931348623157E308)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testInt32ScalarQuery() throws Exception {
    String sql = "SELECT flatten(int32) AS int_col\n" +
            "FROM dfs.`hdf5/scalar.h5`\n" +
            "WHERE path='/datatype/int32'";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("int_col", TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-2147483648)
      .addRow(1)
      .addRow(2)
      .addRow(3)
      .addRow(4)
      .addRow(5)
      .addRow(6)
      .addRow(7)
      .addRow(8)
      .addRow(2147483647)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testFlattenInt32ScalarQuery() throws Exception {
    String sql = "SELECT * FROM table(dfs.`hdf5/scalar.h5` (type => 'hdf5', defaultPath => '/datatype/int32'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("int32", TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-2147483648)
      .addRow(1)
      .addRow(2)
      .addRow(3)
      .addRow(4)
      .addRow(5)
      .addRow(6)
      .addRow(7)
      .addRow(8)
      .addRow(2147483647)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testInt64ScalarQuery() throws Exception {
    String sql = "SELECT flatten(int64) AS long_col\n" +
            "FROM dfs.`hdf5/scalar.h5`\n" +
            "WHERE path='/datatype/int64'";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("long_col", TypeProtos.MinorType.BIGINT, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-9223372036854775808L)
      .addRow(1L)
      .addRow(2L)
      .addRow(3L)
      .addRow(4L)
      .addRow(5L)
      .addRow(6L)
      .addRow(7L)
      .addRow(8L)
      .addRow(9223372036854775807L)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);

  }

  @Test
  public void testFlattenInt64ScalarQuery() throws Exception {
    String sql = "SELECT * FROM table(dfs.`hdf5/scalar.h5` (type => 'hdf5', defaultPath => '/datatype/int64'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("int64", TypeProtos.MinorType.BIGINT, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-9223372036854775808L)
      .addRow(1L)
      .addRow(2L)
      .addRow(3L)
      .addRow(4L)
      .addRow(5L)
      .addRow(6L)
      .addRow(7L)
      .addRow(8L)
      .addRow(9223372036854775807L)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testStringScalarQuery() throws Exception {
    String sql = "SELECT flatten(s10) AS string_col\n" +
            "FROM dfs.`hdf5/scalar.h5`\n" +
            "WHERE path='/datatype/s10'";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("string_col", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("a         ")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("abcdefghij")
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testFlattenStringScalarQuery() throws Exception {
    String sql = "SELECT * FROM table(dfs.`hdf5/scalar.h5` (type => 'hdf5', defaultPath => '/datatype/s10'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("s10", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("a         ")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("abcdefghij")
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testUnicodeScalarQuery() throws Exception {
    String sql = "SELECT flatten(unicode) AS string_col\n" +
            "FROM dfs.`hdf5/scalar.h5`\n" +
            "WHERE path='/datatype/unicode'";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("string_col", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("a")
      .addRow("Ελληνικά")
      .addRow("日本語")
      .addRow("العربية")
      .addRow("экземпляр")
      .addRow("סקרן")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("abcdefghij")
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testUnicodeFlattenScalarQuery() throws Exception {
    String sql = "SELECT * FROM table(dfs.`hdf5/scalar.h5` (type => 'hdf5', defaultPath => '/datatype/unicode'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("unicode", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("a")
      .addRow("Ελληνικά")
      .addRow("日本語")
      .addRow("العربية")
      .addRow("экземпляр")
      .addRow("סקרן")
      .addRow("")
      .addRow("")
      .addRow("")
      .addRow("abcdefghij")
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }


  @Test
  public void test1DScalarQuery() throws Exception {
    String sql = "SELECT int_col FROM (SELECT FLATTEN(`1D`) AS int_col\n" +
            "FROM dfs.`hdf5/scalar.h5`\n" +
            "WHERE path='/nd/1D') WHERE int_col < 5";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("int_col", TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-2147483648)
      .addRow(1)
      .addRow(2)
      .addRow(3)
      .addRow(4)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void test1DFlattenScalarQuery() throws Exception {
    String sql = "SELECT * FROM table(dfs.`hdf5/scalar.h5` (type => 'hdf5', defaultPath => '/nd/1D')) WHERE `1D` < 5";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("1D", MinorType.INT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-2147483648)
      .addRow(1)
      .addRow(2)
      .addRow(3)
      .addRow(4)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }


  @Test
  public void test2DFlattenScalarQuery() throws Exception {
    String sql = "SELECT int_col_0, int_col_1 FROM table(dfs.`hdf5/scalar.h5` (type => 'hdf5', defaultPath => '/nd/2D'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("int_col_0", TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL)
      .add("int_col_1", TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-2147483648, 1)
      .addRow(10, 11)
      .addRow(20, 21)
      .addRow(30, 31)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void test2DScalarQuery() throws Exception {
    String sql = "SELECT int_data[0] AS col1,\n" +
      "int_data[1] AS col2\n" +
      "FROM\n" +
      "(\n" +
      "SELECT flatten(int_data) AS int_data\n" +
      "FROM dfs.`hdf5/scalar.h5`\n" +
      "WHERE path='/nd/2D'\n" +
      ") AS t1";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("col1", TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL)
      .add("col2", TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-2147483648, 1)
      .addRow(10, 11)
      .addRow(20, 21)
      .addRow(30, 31)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }


  @Test
  public void test3DScalarQuery() throws Exception {
    String sql = "SELECT int_data[0] AS col1,\n" +
      "int_data[1] AS col2\n" +
      "FROM\n" +
      "(\n" +
      "SELECT flatten(int_data) AS int_data\n" +
      "FROM dfs.`hdf5/scalar.h5`\n" +
      "WHERE path='/nd/3D'\n" +
      ") AS t1";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("col1", MinorType.INT, DataMode.OPTIONAL)
      .add("col2", MinorType.INT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-2147483648, 5)
      .addRow(1, 6)
      .addRow(2, 7)
      .addRow(3, 8)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void test3DFlattenScalarQuery() throws Exception {
    String sql = "SELECT int_col_0, int_col_1 FROM table(dfs.`hdf5/scalar.h5` (type => 'hdf5', defaultPath => '/nd/3D'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("int_col_0", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_1", MinorType.INT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-2147483648, 5)
      .addRow(1, 6)
      .addRow(2, 7)
      .addRow(3, 8)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void test4DScalarQuery() throws Exception {
    String sql = "SELECT int_data[0] AS col1,\n" +
            "int_data[1] AS col2\n" +
            "FROM\n" +
            "(\n" +
            "SELECT flatten(int_data) AS int_data\n" +
            "FROM dfs.`hdf5/scalar.h5`\n" +
            "WHERE path='/nd/4D'\n" +
            ") AS t1";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("col1", MinorType.INT, DataMode.OPTIONAL)
      .add("col2", MinorType.INT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-2147483648, 5)
      .addRow(1, 6)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void test4DFlattenScalarQuery() throws Exception {
    String sql = "SELECT int_col_0, int_col_1 FROM table(dfs.`hdf5/scalar.h5` (type => 'hdf5', defaultPath => '/nd/4D')) WHERE int_col_0 <= 2";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("int_col_0", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_1", MinorType.INT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(-2147483648, 5)
      .addRow(1, 6)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testNonScalarIntQuery() throws Exception {
    String sql = "SELECT field_1 FROM( SELECT flatten(t1.compound_data.`field 1`) as field_1\n" +
            "FROM dfs.`hdf5/non-scalar.h5` AS t1) WHERE field_1 < 5";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("field_1", TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(0)
      .addRow(1)
      .addRow(2)
      .addRow(3)
      .addRow(4)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }
  @Test
  public void testNonScalarFloatQuery() throws Exception {
    String sql = "SELECT field_2 FROM (SELECT flatten(t1.compound_data.`field 2`) as field_2\n" +
            "FROM dfs.`hdf5/non-scalar.h5` AS t1) WHERE field_2 < 5.0";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("field_2", MinorType.FLOAT8, DataMode.REQUIRED)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(0.0)
      .addRow(1.0)
      .addRow(2.0)
      .addRow(3.0)
      .addRow(4.0)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

 @Test
  public void testNonScalarStringQuery() throws Exception {
    String sql = "SELECT field_3 FROM (SELECT flatten(t1.compound_data.`field 3`) as field_3\n" +
            "FROM dfs.`hdf5/non-scalar.h5` AS t1) WHERE CAST(field_3 AS INTEGER) < 5 ";

   RowSet results = client.queryBuilder().sql(sql).rowSet();
   TupleMetadata expectedSchema = new SchemaBuilder()
     .add("field_3", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
     .buildSchema();

   RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
     .addRow("0")
     .addRow("1")
     .addRow("2")
     .addRow("3")
     .addRow("4")
     .build();

   new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testAttributes() throws Exception {
    String sql = "SELECT path, file_name\n" +
            "FROM dfs.`hdf5/browsing.h5` AS t1 WHERE t1.attributes.`important` = false";

    //String sql = "SELECT path, attributes FROM dfs.`hdf5/browsing.h5`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("path", MinorType.VARCHAR, DataMode.OPTIONAL)
      .add("file_name", MinorType.VARCHAR, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("/groupB", "browsing.h5")
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testStarProjectDatasetQuery() throws Exception {
    String sql = "SELECT * \n"+
      "FROM \n" +
      "table(dfs.`hdf5/dset.h5` (type => 'hdf5', defaultPath => '/dset'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("int_col_0", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_1", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_2", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_3", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_4", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_5", MinorType.INT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1,2,3,4,5,6)
      .addRow(7,8,9,10,11,12)
      .addRow(13,14,15,16,17,18)
      .addRow(19,20,21,22,23,24)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testExplicitProjectDatasetQuery() throws Exception {
    String sql = "SELECT int_col_0, int_col_1, int_col_2, int_col_3, int_col_4\n"+
      "FROM \n" +
      "table(dfs.`hdf5/dset.h5` (type => 'hdf5', defaultPath => '/dset'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("int_col_0", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_1", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_2", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_3", MinorType.INT, DataMode.OPTIONAL)
      .add("int_col_4", MinorType.INT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1,2,3,4,5)
      .addRow(7,8,9,10,11)
      .addRow(13,14,15,16,17)
      .addRow(19,20,21,22,23)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testCompoundStarQuery() throws Exception {

    String sql = "SELECT * FROM table(dfs.`hdf5/non-scalar.h5` (type => 'hdf5', defaultPath => '/compound')) WHERE field_1 < 5";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("field_1", MinorType.INT, DataMode.OPTIONAL)
      .add("field_2", MinorType.FLOAT8, DataMode.OPTIONAL)
      .add("field_3", MinorType.VARCHAR, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(0, 0.0, "0")
      .addRow(1, 1.0, "1")
      .addRow(2, 2.0, "2")
      .addRow(3, 3.0, "3")
      .addRow(4, 4.0, "4")
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testCompoundExplicitQuery() throws Exception {

    String sql = "SELECT `field_1`, `field_3` FROM table(dfs.`hdf5/non-scalar.h5` (type => 'hdf5', defaultPath => '/compound')) WHERE field_1 < 5";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("field_1", MinorType.INT, DataMode.OPTIONAL)
      .add("field_3", MinorType.VARCHAR, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(0, "0")
      .addRow(1, "1")
      .addRow(2, "2")
      .addRow(3, "3")
      .addRow(4, "4")
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testCompoundExplicitQuery2() throws Exception {

    String sql = "SELECT `field_1` FROM table(dfs.`hdf5/non-scalar.h5` (type => 'hdf5', defaultPath => '/compound')) WHERE field_1 < 5";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("field_1", TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(0)
      .addRow(1)
      .addRow(2)
      .addRow(3)
      .addRow(4)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "SELECT COUNT(*) FROM dfs.`hdf5/dset.h5`";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();
    assertEquals("Counts should match",1L, cnt);
  }

  @Test
  public void testExplicitQueryWithCompressedFile() throws Exception {
    generateCompressedFile("hdf5/dset.h5", "zip", "hdf5/dset.h5.zip" );

    String sql = "SELECT path, data_type, file_name FROM dfs.`hdf5/dset.h5.zip`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("path", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("data_type", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("file_name", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("/dset", "DATASET", "dset.h5.zip")
      .build();
    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }

  @Test
  public void testInlineSchema() throws Exception {

    String sql = "SELECT * FROM table(dfs.`hdf5/non-scalar.h5` (type => 'hdf5', defaultPath => '/compound', schema => 'inline=(field_1 int not null, field_2 double not null, " +
      "field_3 varchar not null, fixed_field int not null default `20`)')) WHERE field_1 < 5";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    // Verify that the returned data used the schema.
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("field_1", TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED)
      .add("field_2", TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.REQUIRED)
      .add("field_3", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
      .add("fixed_field", TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(0, 0.0, "0", 20)
      .addRow(1, 1.0, "1", 20)
      .addRow(2, 2.0, "2", 20)
      .addRow(3, 3.0, "3", 20)
      .addRow(4, 4.0, "4", 20)
      .build();

    new RowSetComparison(expected).unorderedVerifyAndClearAll(results);
  }
}
