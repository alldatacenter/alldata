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
package org.apache.drill.exec.store.hive;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.Timestamp;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.exec.hive.HiveTestUtilities;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTestQuery;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.serde.serdeConstants;

import static org.apache.drill.exec.hive.HiveTestUtilities.createDirWithPosixPermissions;
import static org.apache.drill.exec.hive.HiveTestUtilities.executeQuery;

public class HiveTestDataGenerator {

  private final BaseDirTestWatcher dirTestWatcher;

  private final File baseDir;

  private final String warehouseDir;

  public HiveTestDataGenerator(BaseDirTestWatcher dirTestWatcher, File baseDir, String warehouseDir) {
    this.dirTestWatcher = dirTestWatcher;
    this.baseDir = baseDir;
    this.warehouseDir = warehouseDir;
  }

  public void generateData(Driver hiveDriver) {
    try {
      generateDataInternal(hiveDriver);
    } catch (Exception e) {
      throw new RuntimeException("Exception was thrown while generating test data", e);
    }
  }

  private void generateDataInternal(Driver hiveDriver) throws Exception {
    File part1Dir = createDirWithPosixPermissions(baseDir, "part1");
    // generate (key, value) test data
    String testDataFile = generateTestDataFile();

    // Create a (key, value) schema table with Text SerDe which is available in hive-serdes.jar
    executeQuery(hiveDriver, "CREATE TABLE IF NOT EXISTS default.kv(key INT, value STRING) " +
        "ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE");
    executeQuery(hiveDriver, "LOAD DATA LOCAL INPATH '" + testDataFile + "' OVERWRITE INTO TABLE default.kv");

    // Create a (key, value) schema table in non-default database with RegexSerDe which is available in hive-contrib.jar
    // Table with RegExSerde is expected to have columns of STRING type only.
    executeQuery(hiveDriver, "CREATE DATABASE IF NOT EXISTS db1");
    executeQuery(hiveDriver, "CREATE TABLE db1.kv_db1(key STRING, value STRING) " +
        "ROW FORMAT SERDE 'org.apache.hadoop.hive.contrib.serde2.RegexSerDe' " +
        "WITH SERDEPROPERTIES (" +
        "  \"input.regex\" = \"([0-9]*), (.*_[0-9]*)\", " +
        "  \"output.format.string\" = \"%1$s, %2$s\"" +
        ") ");
    executeQuery(hiveDriver, "INSERT INTO TABLE db1.kv_db1 SELECT * FROM default.kv");

    // Create an Avro format based table backed by schema in a separate file
    final String avroCreateQuery = String.format("CREATE TABLE db1.avro " +
        "ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.avro.AvroSerDe' " +
        "STORED AS INPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat' " +
        "OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat' " +
        "TBLPROPERTIES ('avro.schema.url'='file:///%s')",
        BaseTestQuery.getPhysicalFileFromResource("avro_test_schema.json").replace('\\', '/'));

    executeQuery(hiveDriver, avroCreateQuery);
    executeQuery(hiveDriver, "INSERT INTO TABLE db1.avro SELECT * FROM default.kv");

    executeQuery(hiveDriver, "USE default");

    // create a table with no data
    executeQuery(hiveDriver, "CREATE TABLE IF NOT EXISTS empty_table(a INT, b STRING)");
    // delete the table location of empty table
    File emptyTableLocation = new File(warehouseDir, "empty_table");
    if (emptyTableLocation.exists()) {
      FileUtils.forceDelete(emptyTableLocation);
    }

    // create a Hive table that has columns with data types which are supported for reading in Drill.
    testDataFile = generateAllTypesDataFile();
    executeQuery(hiveDriver,
        "CREATE TABLE IF NOT EXISTS readtest (" +
        "  binary_field BINARY," +
        "  boolean_field BOOLEAN," +
        "  tinyint_field TINYINT," +
        "  decimal0_field DECIMAL," +
        "  decimal9_field DECIMAL(6, 2)," +
        "  decimal18_field DECIMAL(15, 5)," +
        "  decimal28_field DECIMAL(23, 1)," +
        "  decimal38_field DECIMAL(30, 3)," +
        "  double_field DOUBLE," +
        "  float_field FLOAT," +
        "  int_field INT," +
        "  bigint_field BIGINT," +
        "  smallint_field SMALLINT," +
        "  string_field STRING," +
        "  varchar_field VARCHAR(50)," +
        "  timestamp_field TIMESTAMP," +
        "  date_field DATE," +
        "  char_field CHAR(10)" +
        ") PARTITIONED BY (" +
        // There is a regression in Hive 1.2.1 in binary type partition columns. Disable for now.
        // "  binary_part BINARY," +
        "  boolean_part BOOLEAN," +
        "  tinyint_part TINYINT," +
        "  decimal0_part DECIMAL," +
        "  decimal9_part DECIMAL(6, 2)," +
        "  decimal18_part DECIMAL(15, 5)," +
        "  decimal28_part DECIMAL(23, 1)," +
        "  decimal38_part DECIMAL(30, 3)," +
        "  double_part DOUBLE," +
        "  float_part FLOAT," +
        "  int_part INT," +
        "  bigint_part BIGINT," +
        "  smallint_part SMALLINT," +
        "  string_part STRING," +
        "  varchar_part VARCHAR(50)," +
        "  timestamp_part TIMESTAMP," +
        "  date_part DATE," +
        "  char_part CHAR(10)" +
        ") ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' " +
        "TBLPROPERTIES ('serialization.null.format'='') "
    );

    // Add a partition to table 'readtest'
    executeQuery(hiveDriver,
        "ALTER TABLE readtest ADD IF NOT EXISTS PARTITION ( " +
        // There is a regression in Hive 1.2.1 in binary type partition columns. Disable for now.
        // "  binary_part='binary', " +
        "  boolean_part='true', " +
        "  tinyint_part='64', " +
        "  decimal0_part='36.9', " +
        "  decimal9_part='36.9', " +
        "  decimal18_part='3289379872.945645', " +
        "  decimal28_part='39579334534534.35345', " +
        "  decimal38_part='363945093845093890.9', " +
        "  double_part='8.345', " +
        "  float_part='4.67', " +
        "  int_part='123456', " +
        "  bigint_part='234235', " +
        "  smallint_part='3455', " +
        "  string_part='string', " +
        "  varchar_part='varchar', " +
        "  timestamp_part='2013-07-05 17:01:00', " +
        "  date_part='2013-07-05', " +
        "  char_part='char')"
    );

    // Add a second partition to table 'readtest' which contains the same values as the first partition except
    // for tinyint_part partition column
    executeQuery(hiveDriver,
        "ALTER TABLE readtest ADD IF NOT EXISTS PARTITION ( " +
            // There is a regression in Hive 1.2.1 in binary type partition columns. Disable for now.
            // "  binary_part='binary', " +
            "  boolean_part='true', " +
            "  tinyint_part='65', " +
            "  decimal0_part='36.9', " +
            "  decimal9_part='36.9', " +
            "  decimal18_part='3289379872.945645', " +
            "  decimal28_part='39579334534534.35345', " +
            "  decimal38_part='363945093845093890.9', " +
            "  double_part='8.345', " +
            "  float_part='4.67', " +
            "  int_part='123456', " +
            "  bigint_part='234235', " +
            "  smallint_part='3455', " +
            "  string_part='string', " +
            "  varchar_part='varchar', " +
            "  timestamp_part='2013-07-05 17:01:00', " +
            "  date_part='2013-07-05', " +
            "  char_part='char')"
    );

    // Load data into table 'readtest'
    executeQuery(hiveDriver,
        String.format("LOAD DATA LOCAL INPATH '%s' INTO TABLE default.readtest PARTITION (" +
        // There is a regression in Hive 1.2.1 in binary type partition columns. Disable for now.
        // "  binary_part='binary', " +
        "  boolean_part='true', " +
        "  tinyint_part='64', " +
        "  decimal0_part='36.9', " +
        "  decimal9_part='36.9', " +
        "  decimal18_part='3289379872.945645', " +
        "  decimal28_part='39579334534534.35345', " +
        "  decimal38_part='363945093845093890.9', " +
        "  double_part='8.345', " +
        "  float_part='4.67', " +
        "  int_part='123456', " +
        "  bigint_part='234235', " +
        "  smallint_part='3455', " +
        "  string_part='string', " +
        "  varchar_part='varchar', " +
        "  timestamp_part='2013-07-05 17:01:00', " +
        "  date_part='2013-07-05'," +
        "  char_part='char'" +
            ")", testDataFile));

    // create a table that has all Hive types. This is to test how hive tables metadata is populated in
    // Drill's INFORMATION_SCHEMA.
    executeQuery(hiveDriver,
        "CREATE TABLE IF NOT EXISTS infoschematest(" +
        "booleanType BOOLEAN, " +
        "tinyintType TINYINT, " +
        "smallintType SMALLINT, " +
        "intType INT, " +
        "bigintType BIGINT, " +
        "floatType FLOAT, " +
        "doubleType DOUBLE, " +
        "dateType DATE, " +
        "timestampType TIMESTAMP, " +
        "binaryType BINARY, " +
        "decimalType DECIMAL(38, 2), " +
        "stringType STRING, " +
        "varCharType VARCHAR(20), " +
        "listType ARRAY<STRING>, " +
        "mapType MAP<STRING,INT>, " +
        "structType STRUCT<sint:INT,sboolean:BOOLEAN,sstring:STRING>, " +
        "uniontypeType UNIONTYPE<int, double, array<string>>, " +
        "charType CHAR(10))"
    );

    /*
     * Create a PARQUET table with all supported types.
     */
    executeQuery(hiveDriver,
        "CREATE TABLE readtest_parquet (" +
            "  binary_field BINARY, " +
            "  boolean_field BOOLEAN, " +
            "  tinyint_field TINYINT," +
            "  decimal0_field DECIMAL," +
            "  decimal9_field DECIMAL(6, 2)," +
            "  decimal18_field DECIMAL(15, 5)," +
            "  decimal28_field DECIMAL(23, 1)," +
            "  decimal38_field DECIMAL(30, 3)," +
            "  double_field DOUBLE," +
            "  float_field FLOAT," +
            "  int_field INT," +
            "  bigint_field BIGINT," +
            "  smallint_field SMALLINT," +
            "  string_field STRING," +
            "  varchar_field VARCHAR(50)," +
            "  timestamp_field TIMESTAMP," +
            "  char_field CHAR(10)" +
            ") PARTITIONED BY (" +
            // There is a regression in Hive 1.2.1 in binary type partition columns. Disable for now.
            // "  binary_part BINARY," +
            "  boolean_part BOOLEAN," +
            "  tinyint_part TINYINT," +
            "  decimal0_part DECIMAL," +
            "  decimal9_part DECIMAL(6, 2)," +
            "  decimal18_part DECIMAL(15, 5)," +
            "  decimal28_part DECIMAL(23, 1)," +
            "  decimal38_part DECIMAL(30, 3)," +
            "  double_part DOUBLE," +
            "  float_part FLOAT," +
            "  int_part INT," +
            "  bigint_part BIGINT," +
            "  smallint_part SMALLINT," +
            "  string_part STRING," +
            "  varchar_part VARCHAR(50)," +
            "  timestamp_part TIMESTAMP," +
            "  date_part DATE," +
            "  char_part CHAR(10)" +
            ") STORED AS parquet "
    );

    executeQuery(hiveDriver, "INSERT OVERWRITE TABLE readtest_parquet " +
        "PARTITION (" +
        // There is a regression in Hive 1.2.1 in binary type partition columns. Disable for now.
        // "  binary_part='binary', " +
        "  boolean_part='true', " +
        "  tinyint_part='64', " +
        "  decimal0_part='36.9', " +
        "  decimal9_part='36.9', " +
        "  decimal18_part='3289379872.945645', " +
        "  decimal28_part='39579334534534.35345', " +
        "  decimal38_part='363945093845093890.9', " +
        "  double_part='8.345', " +
        "  float_part='4.67', " +
        "  int_part='123456', " +
        "  bigint_part='234235', " +
        "  smallint_part='3455', " +
        "  string_part='string', " +
        "  varchar_part='varchar', " +
        "  timestamp_part='2013-07-05 17:01:00', " +
        "  date_part='2013-07-05', " +
        "  char_part='char'" +
        ") " +
        " SELECT " +
        "  binary_field," +
        "  boolean_field," +
        "  tinyint_field," +
        "  decimal0_field," +
        "  decimal9_field," +
        "  decimal18_field," +
        "  decimal28_field," +
        "  decimal38_field," +
        "  double_field," +
        "  float_field," +
        "  int_field," +
        "  bigint_field," +
        "  smallint_field," +
        "  string_field," +
        "  varchar_field," +
        "  timestamp_field," +
        "  char_field" +
        " FROM readtest WHERE tinyint_part = 64");

    // Add a second partition to table 'readtest_parquet' which contains the same values as the first partition except
    // for tinyint_part partition column
    executeQuery(hiveDriver,
        "ALTER TABLE readtest_parquet ADD PARTITION ( " +
            // There is a regression in Hive 1.2.1 in binary type partition columns. Disable for now.
            // "  binary_part='binary', " +
            "  boolean_part='true', " +
            "  tinyint_part='65', " +
            "  decimal0_part='36.9', " +
            "  decimal9_part='36.9', " +
            "  decimal18_part='3289379872.945645', " +
            "  decimal28_part='39579334534534.35345', " +
            "  decimal38_part='363945093845093890.9', " +
            "  double_part='8.345', " +
            "  float_part='4.67', " +
            "  int_part='123456', " +
            "  bigint_part='234235', " +
            "  smallint_part='3455', " +
            "  string_part='string', " +
            "  varchar_part='varchar', " +
            "  timestamp_part='2013-07-05 17:01:00', " +
            "  date_part='2013-07-05', " +
            "  char_part='char')"
    );

    executeQuery(hiveDriver, "CREATE TABLE IF NOT EXISTS " +
        "partition_pruning_test_loadtable(a DATE, b TIMESTAMP, c INT, d INT, e INT) " +
        "ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE");
    executeQuery(hiveDriver,
        String.format("LOAD DATA LOCAL INPATH '%s' INTO TABLE partition_pruning_test_loadtable",
        generateTestDataFileForPartitionInput()));

    // create partitioned hive table to test partition pruning
    executeQuery(hiveDriver,
        "CREATE TABLE IF NOT EXISTS partition_pruning_test(a DATE, b TIMESTAMP) "+
        "partitioned by (c INT, d INT, e INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE");
    executeQuery(hiveDriver, "INSERT OVERWRITE TABLE partition_pruning_test PARTITION(c, d, e) " +
        "SELECT a, b, c, d, e FROM partition_pruning_test_loadtable");

    executeQuery(hiveDriver,
      "CREATE TABLE IF NOT EXISTS partition_with_few_schemas(a DATE, b TIMESTAMP) "+
        "partitioned by (c INT, d INT, e INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE");
    executeQuery(hiveDriver, "INSERT OVERWRITE TABLE partition_with_few_schemas PARTITION(c, d, e) " +
      "SELECT a, b, c, d, e FROM partition_pruning_test_loadtable");
    executeQuery(hiveDriver,"alter table partition_with_few_schemas partition(c=1, d=1, e=1) change a a1 INT");
    executeQuery(hiveDriver,"alter table partition_with_few_schemas partition(c=1, d=1, e=2) change a a1 INT");
    executeQuery(hiveDriver,"alter table partition_with_few_schemas partition(c=2, d=2, e=2) change a a1 INT");

    // Add a partition with custom location
    executeQuery(hiveDriver,
        String.format("ALTER TABLE partition_pruning_test ADD PARTITION (c=99, d=98, e=97) LOCATION '%s'",
          part1Dir.getAbsolutePath()));
    executeQuery(hiveDriver,
        String.format("INSERT INTO TABLE partition_pruning_test PARTITION(c=99, d=98, e=97) " +
                "SELECT '%s', '%s' FROM kv LIMIT 1",
        new Date(System.currentTimeMillis()).toString(), new Timestamp(System.currentTimeMillis()).toString()));

    executeQuery(hiveDriver, "DROP TABLE partition_pruning_test_loadtable");

    // Create a partitioned parquet table (DRILL-3938)
    executeQuery(hiveDriver,
        "CREATE TABLE kv_parquet(key INT, value STRING) PARTITIONED BY (part1 int) STORED AS PARQUET");
    executeQuery(hiveDriver, "INSERT INTO TABLE kv_parquet PARTITION(part1) SELECT key, value, key FROM default.kv");
    executeQuery(hiveDriver, "ALTER TABLE kv_parquet ADD COLUMNS (newcol string)");

    // Create a StorageHandler based table (DRILL-3739)
    executeQuery(hiveDriver, "CREATE TABLE kv_sh(key INT, value STRING) STORED BY " +
        "'org.apache.hadoop.hive.ql.metadata.DefaultStorageHandler'");
    // Insert fails if the table directory already exists for tables with DefaultStorageHandlers. Its a known
    // issue in Hive. So delete the table directory created as part of the CREATE TABLE
    FileUtils.deleteQuietly(new File(warehouseDir, "kv_sh"));
    //executeQuery(hiveDriver, "INSERT OVERWRITE TABLE kv_sh SELECT * FROM kv");

    // Create text tables with skip header and footer table property
    executeQuery(hiveDriver, "create database if not exists skipper");
    executeQuery(hiveDriver, createTableWithHeaderFooterProperties("skipper.kv_text_small", "textfile", "1", "1", false));
    executeQuery(hiveDriver, generateTestDataWithHeadersAndFooters("skipper.kv_text_small", 5, 1, 1));

    executeQuery(hiveDriver, createTableWithHeaderFooterProperties("skipper.kv_text_large", "textfile", "2", "2", false));
    executeQuery(hiveDriver, generateTestDataWithHeadersAndFooters("skipper.kv_text_large", 5000, 2, 2));

    executeQuery(hiveDriver, createTableWithHeaderFooterProperties("skipper.kv_incorrect_skip_header", "textfile", "A", "1", false));
    executeQuery(hiveDriver, generateTestDataWithHeadersAndFooters("skipper.kv_incorrect_skip_header", 5, 1, 1));

    executeQuery(hiveDriver, createTableWithHeaderFooterProperties("skipper.kv_incorrect_skip_footer", "textfile", "1", "A", false));
    executeQuery(hiveDriver, generateTestDataWithHeadersAndFooters("skipper.kv_incorrect_skip_footer", 5, 1, 1));

    executeQuery(hiveDriver, createTableWithHeaderFooterProperties("skipper.kv_text_header_only", "textfile", "5", "0", false));
    executeQuery(hiveDriver, generateTestDataWithHeadersAndFooters("skipper.kv_text_header_only", 0, 5, 0));

    executeQuery(hiveDriver, createTableWithHeaderFooterProperties("skipper.kv_text_footer_only", "textfile", "0", "5", false));
    executeQuery(hiveDriver, generateTestDataWithHeadersAndFooters("skipper.kv_text_footer_only", 0, 0, 5));

    executeQuery(hiveDriver, createTableWithHeaderFooterProperties("skipper.kv_text_header_footer_only", "textfile", "5", "5", false));
    executeQuery(hiveDriver, generateTestDataWithHeadersAndFooters("skipper.kv_text_header_footer_only", 0, 5, 5));

    executeQuery(hiveDriver, createTableWithHeaderFooterProperties("skipper.kv_text_with_part", "textfile", "5", "5", true));
    executeQuery(hiveDriver, "insert overwrite table skipper.kv_text_with_part partition (part) " +
    "select key, value, key % 2 as part from skipper.kv_text_large");

    // Create a table based on json file
    executeQuery(hiveDriver, "create table default.simple_json(json string)");
    final String loadData = "load data local inpath '" +
        Resources.getResource("simple.json") + "' into table default.simple_json";
    executeQuery(hiveDriver, loadData);

    createTestDataForDrillNativeParquetReaderTests(hiveDriver);

    createSubDirTable(hiveDriver, testDataFile);

    // hive views
    executeQuery(hiveDriver, "CREATE OR REPLACE VIEW readtest_view AS SELECT * FROM readtest");
    executeQuery(hiveDriver, "CREATE VIEW IF NOT EXISTS hive_view AS SELECT * FROM kv");
    executeQuery(hiveDriver, "CREATE OR REPLACE VIEW kv_native_view AS SELECT * FROM kv_native");
    String disableRewrite = HiveTestUtilities.isHive3() ? "DISABLE REWRITE" : "";
    executeQuery(hiveDriver, String.format("CREATE MATERIALIZED VIEW IF NOT EXISTS hive_view_m %s AS SELECT * FROM kv WHERE key = 1", disableRewrite));
    executeQuery(hiveDriver, "CREATE OR REPLACE VIEW view_over_hive_view AS SELECT * FROM hive_view WHERE key BETWEEN 2 AND 3");
    executeQuery(hiveDriver, "CREATE OR REPLACE VIEW db1.two_table_view AS SELECT COUNT(dk.key) dk_key_count FROM db1.avro dk " +
        "INNER JOIN kv ON kv.key = dk.key");

    createTableWithEmptyParquet(hiveDriver);
  }

  private void createTestDataForDrillNativeParquetReaderTests(Driver hiveDriver) {
    // Hive managed table that has data qualified for Drill native filter push down
    executeQuery(hiveDriver, "create table kv_native(key int, int_key int, var_key varchar(10), dec_key decimal(5, 2)) stored as parquet");
    // each insert is created in separate file
    executeQuery(hiveDriver, "insert into table kv_native values (1, 1, 'var_1', 1.11), (1, 2, 'var_2', 2.22)");
    executeQuery(hiveDriver, "insert into table kv_native values (1, 3, 'var_3', 3.33), (1, 4, 'var_4', 4.44)");
    executeQuery(hiveDriver, "insert into table kv_native values (2, 5, 'var_5', 5.55), (2, 6, 'var_6', 6.66)");
    executeQuery(hiveDriver, "insert into table kv_native values (null, 7, 'var_7', 7.77), (null, 8, 'var_8', 8.88)");

    // Hive external table which has three partitions

    // copy external table with data from test resources
    dirTestWatcher.copyResourceToRoot(Paths.get("external"));

    File external = new File (baseDir, "external");
    String tableLocation = new File(external, "kv_native_ext").toURI().getPath();

    executeQuery(hiveDriver, String.format("create external table kv_native_ext(key int) " +
        "partitioned by (part_key int) " +
        "stored as parquet location '%s'",
        tableLocation));

    /*
      DATA:
      key, part_key
      1, 1
      2, 1
      3, 2
      4, 2
     */

    // add partitions

    // partition in the same location as table
    String firstPartition = new File(tableLocation, "part_key=1").toURI().getPath();
    executeQuery(hiveDriver, String.format("alter table kv_native_ext add partition (part_key = '1') " +
      "location '%s'", firstPartition));

    // partition in different location with table
    String secondPartition = new File(external, "part_key=2").toURI().getPath();
    executeQuery(hiveDriver, String.format("alter table kv_native_ext add partition (part_key = '2') " +
      "location '%s'", secondPartition));

    // add empty partition
    String thirdPartition = new File(dirTestWatcher.makeSubDir(Paths.get("empty_part")), "part_key=3").toURI().getPath();
    executeQuery(hiveDriver, String.format("alter table kv_native_ext add partition (part_key = '3') " +
      "location '%s'", thirdPartition));
  }

  private void createSubDirTable(Driver hiveDriver, String testDataFile) {
    String tableName = "sub_dir_table";
    dirTestWatcher.copyResourceToRoot(Paths.get(testDataFile), Paths.get(tableName, "sub_dir", "data.txt"));

    String tableLocation = Paths.get(dirTestWatcher.getRootDir().toURI().getPath(), tableName).toUri().getPath();

    String tableDDL = String.format("create external table sub_dir_table (key int, value string) " +
        "row format delimited fields terminated by ',' stored as textfile location '%s'", tableLocation);
    executeQuery(hiveDriver, tableDDL);
  }

  private void createTableWithEmptyParquet(Driver hiveDriver) {
    String location = dirTestWatcher.makeSubDir(Paths.get("table_with_empty_parquet")).toURI().getPath();
    String emptyParquet = "empty.parquet";
    dirTestWatcher.copyResourceToRoot(Paths.get(Resources.getResource(emptyParquet).getPath()),
      Paths.get(location, emptyParquet));

    String tableDDL = String.format("create external table table_with_empty_parquet (id bigint, name string) " +
      "stored as parquet location '%s'", location);
    executeQuery(hiveDriver, tableDDL);
  }

  private File getTempFile() throws Exception {
    return java.nio.file.Files.createTempFile("drill-hive-test", ".txt").toFile();
  }

  private String generateTestDataFile() throws Exception {
    File file = getTempFile();
    try (PrintWriter printWriter = new PrintWriter(file)) {
      for (int i = 1; i <= 5; i++) {
        printWriter.println(String.format("%d, key_%d", i, i));
      }
    }
    return file.getPath();
  }

  private String generateTestDataFileForPartitionInput() throws Exception {
    File file = getTempFile();
    try (PrintWriter printWriter = new PrintWriter(file)) {
      String[] partValues = {"1", "2", "null"};
      for (String partValue : partValues) {
        for (String partValue1 : partValues) {
          for (String partValue2 : partValues) {
            for (int i = 1; i <= 5; i++) {
              Date date = new Date(System.currentTimeMillis());
              Timestamp ts = new Timestamp(System.currentTimeMillis());
              printWriter.printf("%s,%s,%s,%s,%s", date.toString(), ts.toString(), partValue, partValue1, partValue2);
              printWriter.println();
            }
          }
        }
      }
    }

    return file.getPath();
  }

  private String generateAllTypesDataFile() throws Exception {
    File file = getTempFile();

    try (PrintWriter printWriter = new PrintWriter(file)) {
      printWriter.println("YmluYXJ5ZmllbGQ=,false,34,65.99,2347.923,2758725827.9999,29375892739852.7689,"+
          "89853749534593985.7834783,8.345,4.67,123456,234235,3455,stringfield,varcharfield,"+
          "2013-07-05 17:01:00,2013-07-05,charfield");
      printWriter.println(",,,,,,,,,,,,,,,,");
    }

    return file.getPath();
  }

  private String createTableWithHeaderFooterProperties(String tableName,
                                                       String format,
                                                       String headerValue,
                                                       String footerValue,
                                                       boolean hasPartitions) {
    StringBuilder sb = new StringBuilder();
    sb.append("create table ").append(tableName);
    sb.append(" (key int, value string) ");
    if (hasPartitions) {
      sb.append("partitioned by (part bigint) ");
    }
    sb.append(" stored as ").append(format);
    sb.append(" tblproperties(");
    sb.append("'").append(serdeConstants.HEADER_COUNT).append("'='").append(headerValue).append("'");
    sb.append(",");
    sb.append("'").append(serdeConstants.FOOTER_COUNT).append("'='").append(footerValue).append("'");
    sb.append(")");

    return sb.toString();
  }

  private String generateTestDataWithHeadersAndFooters(String tableName, int rowCount, int headerLines, int footerLines) {
    StringBuilder sb = new StringBuilder();
    sb.append("insert into table ").append(tableName).append(" values ");
    sb.append(StringUtils.repeat("('key_header', 'value_header')", ",", headerLines));
    if (headerLines > 0) {
      sb.append(",");
    }
    for (int i  = 1; i <= rowCount; i++) {
        sb.append("(").append(i).append(",").append("'key_").append(i).append("'),");
    }
    if (footerLines <= 0) {
      sb.deleteCharAt(sb.length() - 1);
    }
    sb.append(StringUtils.repeat("('key_footer', 'value_footer')", ",", footerLines));

    return sb.toString();
  }
}
