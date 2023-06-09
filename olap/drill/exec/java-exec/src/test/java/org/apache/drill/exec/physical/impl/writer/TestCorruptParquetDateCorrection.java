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
package org.apache.drill.exec.physical.impl.writer;

import static java.lang.String.format;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import org.apache.drill.PlanTestBase;
import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.store.parquet.metadata.Metadata;
import org.apache.drill.test.TestBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests for compatibility reading old parquet files after date corruption
 * issue was fixed in DRILL-4203.
 *
 * Drill could write non-standard dates into parquet files. This issue is related to
 * all drill releases where {@link org.apache.drill.exec.store.parquet.ParquetRecordWriter#WRITER_VERSION_PROPERTY} <
 * {@link org.apache.drill.exec.store.parquet.ParquetReaderUtility#DRILL_WRITER_VERSION_STD_DATE_FORMAT}
 * The values have been read correctly by Drill, but external tools like Spark reading the files will see
 * corrupted values for all dates that have been written by Drill.
 *
 * This change corrects the behavior of the Drill parquet writer to correctly
 * store dates in the format given in the parquet specification.
 *
 * To maintain compatibility with old files, the parquet reader code has
 * been updated to check for the old format and automatically shift the
 * corrupted values into corrected ones automatically.
 *
 * The test cases included here should ensure that all files produced by
 * historical versions of Drill will continue to return the same values they
 * had in previous releases. For compatibility with external tools, any old
 * files with corrupted dates can be re-written using the CREATE TABLE AS
 * command (as the writer will now only produce the specification-compliant
 * values, even if after reading out of older corrupt files).
 *
 * While the old behavior was a consistent shift into an unlikely range
 * to be used in a modern database (over 10,000 years in the future), these are still
 * valid date values. In the case where these may have been written into
 * files intentionally, an option is included to turn off the auto-correction.
 * Use of this option is assumed to be extremely unlikely, but it is included
 * for completeness.
 */
@Category({ParquetTest.class, UnlikelyTest.class})
public class TestCorruptParquetDateCorrection extends PlanTestBase {
  private static final Path PARQUET_4203 = Paths.get("parquet", "4203_corrupt_dates");
  // 4 files are in the directory:
  //    - one created with the parquet-writer version number of "2"
  //        - files have extra meta field: parquet-writer.version = 2
  //    - one from and old version of Drill, before we put in proper created by in metadata
  //        - this is read properly by looking at a Max value in the file statistics, to see that
  //          it is way off of a typical date value
  //        - this behavior will be able to be turned off, but will be on by default
  //    - one from the 0.6 version of Drill, before files had min/max statistics
  //        - detecting corrupt values must be deferred to actual data page reading
  //    - one from 1.4, where there is a proper created-by, but the corruption is present
  private static final Path MIXED_CORRUPTED_AND_CORRECT_DATES_PATH = PARQUET_4203.resolve("mixed_drill_versions");
  // partitioned with 1.2.0, no certain metadata that these were written with Drill
  // the value will be checked to see that they look corrupt and they will be corrected
  // by default. Users can use the format plugin option autoCorrectCorruptDates to disable
  // this behavior if they have foreign parquet files with valid rare date values that are
  // in the similar range as Drill's corrupt values
  private static final Path PARTITIONED_1_2_FOLDER = Paths.get("partitioned_with_corruption_4203_1_2");
  private static final Path CORRUPTED_PARTITIONED_DATES_1_2_PATH = PARQUET_4203.resolve(PARTITIONED_1_2_FOLDER);
  // partitioned with 1.4.0, no certain metadata regarding the date corruption status.
  // The same detection approach of the corrupt date values as for the files partitioned with 1.2.0
  private static final Path PARTITIONED_1_4_FOLDER = Paths.get("partitioned_with_corruption_4203");
  private static final Path CORRUPTED_PARTITIONED_DATES_1_4_0_PATH = PARQUET_4203.resolve(PARTITIONED_1_4_FOLDER);
  private static final Path PARQUET_DATE_FILE_WITH_NULL_FILLED_COLS = PARQUET_4203.resolve("null_date_cols_with_corruption_4203.parquet");
  private static final Path PARTITIONED_1_9_FOLDER = Paths.get("1_9_0_partitioned_no_corruption");
  private static final Path CORRECT_PARTITIONED_DATES_1_9_PATH = PARQUET_4203.resolve(PARTITIONED_1_9_FOLDER);
  private static final Path VARCHAR_PARTITIONED = PARQUET_4203.resolve("fewtypes_varcharpartition");
  private static final Path DATE_PARTITIONED = PARQUET_4203.resolve("fewtypes_datepartition");
  private static final Path EXCEPTION_WHILE_PARSING_CREATED_BY_META = PARQUET_4203.resolve("hive1dot2_fewtypes_null");
  private static final Path CORRECT_DATES_1_6_0_PATH = PARQUET_4203.resolve("correct_dates_and_old_drill_parquet_writer.parquet");
  private static final Path MIXED_CORRUPTED_AND_CORRECT_PARTITIONED_FOLDER = Paths.get("mixed_partitioned");

  @BeforeClass
  public static void initFs() throws Exception {
    // Move files into temp directory, rewrite the metadata cache file to contain the appropriate absolute path
    dirTestWatcher.copyResourceToRoot(PARQUET_4203);
    dirTestWatcher.copyResourceToRoot(CORRUPTED_PARTITIONED_DATES_1_2_PATH, PARTITIONED_1_2_FOLDER);
    dirTestWatcher.copyResourceToRoot(CORRUPTED_PARTITIONED_DATES_1_4_0_PATH, MIXED_CORRUPTED_AND_CORRECT_PARTITIONED_FOLDER);
    dirTestWatcher.copyResourceToRoot(CORRUPTED_PARTITIONED_DATES_1_2_PATH, MIXED_CORRUPTED_AND_CORRECT_PARTITIONED_FOLDER.resolve(PARTITIONED_1_2_FOLDER));
    dirTestWatcher.copyResourceToRoot(CORRECT_PARTITIONED_DATES_1_9_PATH, MIXED_CORRUPTED_AND_CORRECT_PARTITIONED_FOLDER.resolve(PARTITIONED_1_9_FOLDER));
    dirTestWatcher.copyResourceToRoot(CORRUPTED_PARTITIONED_DATES_1_4_0_PATH, MIXED_CORRUPTED_AND_CORRECT_PARTITIONED_FOLDER.resolve(PARTITIONED_1_4_FOLDER));
    File metaData = dirTestWatcher.copyResourceToRoot(PARQUET_4203.resolve("drill.parquet.metadata_1_2.requires_replace.txt"),
      PARTITIONED_1_2_FOLDER.resolve(Metadata.OLD_METADATA_FILENAME));
    dirTestWatcher.replaceMetaDataContents(metaData, dirTestWatcher.getRootDir(), null);
  }

  /**
   * Test reading a directory full of partitioned parquet files with dates, these files have a drill version
   * number of "1.9.0-SNAPSHOT" and parquet-writer version number of "2" in their footers, so we can be certain
   * they do not have corruption. The option to disable the correction is passed, but it will not change the result
   * in the case where we are certain correction is NOT needed. For more info see DRILL-4203.
   */
  @Test
  public void testReadPartitionedOnCorrectDates() throws Exception {
    try {
      for (String selection : new String[]{"*", "date_col"}) {
        // for sanity, try reading all partitions without a filter
        TestBuilder builder = testBuilder()
            .sqlQuery("select %s from table(dfs.`%s` (type => 'parquet', autoCorrectCorruptDates => false))",
                selection, CORRECT_PARTITIONED_DATES_1_9_PATH)
            .unOrdered()
            .baselineColumns("date_col");
        addDateBaselineValues(builder);
        builder.go();

        String query = format("select %s from table(dfs.`%s` (type => 'parquet', autoCorrectCorruptDates => false))" +
            " where date_col = date '1970-01-01'", selection, CORRECT_PARTITIONED_DATES_1_9_PATH);
        // verify that pruning is actually taking place
        testPlanMatchingPatterns(query, new String[]{"numFiles=1"}, null);

        // read with a filter on the partition column
        testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("date_col")
            .baselineValues(LocalDate.of(1970, 1, 1))
            .go();
      }
    } finally {
      resetAllSessionOptions();
    }
  }

  @Test
  public void testVarcharPartitionedReadWithCorruption() throws Exception {
    testBuilder()
        .sqlQuery("select date_col from dfs.`%s` where length(varchar_col) = 12", VARCHAR_PARTITIONED)
        .baselineColumns("date_col")
        .unOrdered()
        .baselineValues(LocalDate.of(2039, 4, 9))
        .baselineValues(LocalDate.of(1999, 1, 8))
        .go();
  }

  @Test
  public void testDatePartitionedReadWithCorruption() throws Exception {
    testBuilder()
        .sqlQuery("select date_col from dfs.`%s` where date_col = '1999-04-08'", DATE_PARTITIONED)
        .baselineColumns("date_col")
        .unOrdered()
        .baselineValues(LocalDate.of(1999, 4, 8))
        .go();

    String query = format("select date_col from dfs.`%s` where date_col > '1999-04-08'", DATE_PARTITIONED);
    testPlanMatchingPatterns(query, new String[]{"numFiles=6"}, null);
  }

  @Test
  public void testCorrectDatesAndExceptionWhileParsingCreatedBy() throws Exception {
    testBuilder()
        .sqlQuery("select date_col from dfs.`%s` where to_date(date_col, 'yyyy-mm-dd') < '1997-01-02'",
            EXCEPTION_WHILE_PARSING_CREATED_BY_META)
        .baselineColumns("date_col")
        .unOrdered()
        .baselineValues(LocalDate.of(1996, 1, 29))
        .baselineValues(LocalDate.of(1996, 3, 1))
        .baselineValues(LocalDate.of(1996, 3, 2))
        .baselineValues(LocalDate.of(1997, 3, 1))
        .go();
  }

  // according to SQL spec. '4.4.3.5 Datetime types' year should be less than 9999
  @Test(expected = UserRemoteException.class)
  public void testQueryWithCorruptedDates() throws Exception {
    try {
      TestBuilder builder = testBuilder()
          .sqlQuery("select * from table(dfs.`%s` (type => 'parquet', autoCorrectCorruptDates => false))",
              CORRUPTED_PARTITIONED_DATES_1_2_PATH)
          .unOrdered()
          .baselineColumns("date_col");
      addCorruptedDateBaselineValues(builder);
      builder.go();

      String query = "select * from table(dfs.`%s` (type => 'parquet', " +
          "autoCorrectCorruptDates => false)) where date_col = cast('15334-03-17' as date)";

      test(query, CORRUPTED_PARTITIONED_DATES_1_2_PATH);
    } catch (UserRemoteException e) {
      Assert.assertTrue(e.getMessage()
          .contains("Year out of range"));
      throw e;
    } finally {
      resetAllSessionOptions();
    }
  }

  @Test
  public void testCorruptValueDetectionDuringPruning() throws Exception {
    try {
      for (String selection : new String[]{"*", "date_col"}) {
        for (Path table : new Path[]{CORRUPTED_PARTITIONED_DATES_1_2_PATH, CORRUPTED_PARTITIONED_DATES_1_4_0_PATH}) {
          // for sanity, try reading all partitions without a filter
          TestBuilder builder = testBuilder()
              .sqlQuery("select %s from dfs.`%s`", selection, table)
              .unOrdered()
              .baselineColumns("date_col");
          addDateBaselineValues(builder);
          builder.go();

          String query = format("select %s from dfs.`%s`" +
              " where date_col = date '1970-01-01'", selection, table);
          // verify that pruning is actually taking place
          testPlanMatchingPatterns(query, new String[]{"numFiles=1"}, null);

          // read with a filter on the partition column
          testBuilder()
              .sqlQuery(query)
              .unOrdered()
              .baselineColumns("date_col")
              .baselineValues(LocalDate.of(1970, 1, 1))
              .go();
        }
      }
    } finally {
      resetAllSessionOptions();
    }
  }

  /**
   * To fix some of the corrupted dates fixed as part of DRILL-4203 it requires
   * actually looking at the values stored in the file. A column with date values
   * actually stored must be located to check a value. Just because we find one
   * column where the all values are null does not mean we can safely avoid reading
   * date columns with auto-correction, although null values do not need fixing,
   * other columns may contain actual corrupt date values.
   *
   * This test checks the case where the first columns in the file are all null filled
   * and a later column must be found to identify that the file is corrupt.
   */
  @Test
  public void testReadCorruptDatesWithNullFilledColumns() throws Exception {
    testBuilder()
        .sqlQuery("select null_dates_1, null_dates_2, non_existent_field, date_col from dfs.`%s`",
            PARQUET_DATE_FILE_WITH_NULL_FILLED_COLS)
        .unOrdered()
        .baselineColumns("null_dates_1", "null_dates_2", "non_existent_field", "date_col")
        .baselineValues(null, null, null, LocalDate.of(1970, 1, 1))
        .baselineValues(null, null, null, LocalDate.of(1970, 1, 2))
        .baselineValues(null, null, null, LocalDate.of(1969, 12, 31))
        .baselineValues(null, null, null, LocalDate.of(1969, 12, 30))
        .baselineValues(null, null, null, LocalDate.of(1900, 1, 1))
        .baselineValues(null, null, null, LocalDate.of(2015, 1, 1))
        .go();
  }

  @Test
  public void testUserOverrideDateCorrection() throws Exception {
    // read once with the flat reader
    readFilesWithUserDisabledAutoCorrection();

    try {
      test("alter session set %s = true", ExecConstants.PARQUET_NEW_RECORD_READER);
      // read all of the types with the complex reader
      readFilesWithUserDisabledAutoCorrection();
    } finally {
      resetAllSessionOptions();
    }
  }

  /**
   * Test reading a directory full of parquet files with dates, some of which have corrupted values
   * due to DRILL-4203.
   *
   * Tests reading the files with both the vectorized and complex parquet readers.
   *
   * @throws Exception
   */
  @Test
  public void testReadMixedOldAndNewBothReaders() throws Exception {
    /// read once with the flat reader
    readMixedCorruptedAndCorrectDates();

    try {
      // read all of the types with the complex reader
      test("alter session set %s = true", ExecConstants.PARQUET_NEW_RECORD_READER);
      readMixedCorruptedAndCorrectDates();
    } finally {
      test("alter session set %s = false", ExecConstants.PARQUET_NEW_RECORD_READER);
    }
  }

  @Test
  public void testReadOldMetadataCacheFile() throws Exception {
    // for sanity, try reading all partitions without a filter
    String query = format("select date_col from dfs.`%s`", PARTITIONED_1_2_FOLDER);
    TestBuilder builder = testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("date_col");
    addDateBaselineValues(builder);
    builder.go();
    testPlanMatchingPatterns(query, new String[]{"usedMetadataFile=true"}, null);
  }

  @Test
  public void testReadOldMetadataCacheFileWithPruning() throws Exception {
    String query = format("select date_col from dfs.`%s` where date_col = date '1970-01-01'",
      PARTITIONED_1_2_FOLDER);
    // verify that pruning is actually taking place
    testPlanMatchingPatterns(query, new String[]{"numFiles=1", "usedMetadataFile=true"}, null);

    // read with a filter on the partition column
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("date_col")
        .baselineValues(LocalDate.of(1970, 1, 1))
        .go();
  }

  @Test
  public void testReadNewMetadataCacheFileOverOldAndNewFiles() throws Exception {
    File meta = dirTestWatcher.copyResourceToRoot(
       PARQUET_4203.resolve("mixed_version_partitioned_metadata.requires_replace.txt"),
       MIXED_CORRUPTED_AND_CORRECT_PARTITIONED_FOLDER.resolve(Metadata.OLD_METADATA_FILENAME));
    dirTestWatcher.replaceMetaDataContents(meta, dirTestWatcher.getRootDir(), null);
    // for sanity, try reading all partitions without a filter
    TestBuilder builder = testBuilder()
        .sqlQuery("select date_col from dfs.`%s`", MIXED_CORRUPTED_AND_CORRECT_PARTITIONED_FOLDER)
        .unOrdered()
        .baselineColumns("date_col");
    addDateBaselineValues(builder);
    addDateBaselineValues(builder);
    addDateBaselineValues(builder);
    builder.go();

    String query = format("select date_col from dfs.`%s` where date_col = date '1970-01-01'", MIXED_CORRUPTED_AND_CORRECT_PARTITIONED_FOLDER);
    // verify that pruning is actually taking place
    testPlanMatchingPatterns(query, new String[]{"numFiles=3", "usedMetadataFile=true"}, null);

    // read with a filter on the partition column
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("date_col")
        .baselineValues(LocalDate.of(1970, 1, 1))
        .baselineValues(LocalDate.of(1970, 1, 1))
        .baselineValues(LocalDate.of(1970, 1, 1))
        .go();
  }

  @Test
  public void testCorrectDateValuesGeneratedByOldVersionOfDrill() throws Exception {
    testBuilder()
        .sqlQuery("select i_rec_end_date from dfs.`%s` limit 1", CORRECT_DATES_1_6_0_PATH)
        .baselineColumns("i_rec_end_date")
        .unOrdered()
        .baselineValues(LocalDate.of(2000, 10, 26))
        .go();
  }

  /**
   * Read a directory with parquet files where some have corrupted dates, see DRILL-4203.
   * @throws Exception
   */
  private void readMixedCorruptedAndCorrectDates() throws Exception {
    // ensure that selecting the date column explicitly or as part of a star still results
    // in checking the file metadata for date columns (when we need to check the statistics
    // for bad values) to set the flag that the values are corrupt
    for (String selection : new String[] {"*", "date_col"}) {
      TestBuilder builder = testBuilder()
          .sqlQuery("select %s from dfs.`%s`", selection, MIXED_CORRUPTED_AND_CORRECT_DATES_PATH)
          .unOrdered()
          .baselineColumns("date_col");
      for (int i = 0; i < 4; i++) {
        addDateBaselineValues(builder);
      }
      builder.go();
    }
  }

  private void addDateBaselineValues(TestBuilder builder) {
    builder
        .baselineValues(LocalDate.of(1970, 1, 1))
        .baselineValues(LocalDate.of(1970, 1, 2))
        .baselineValues(LocalDate.of(1969, 12, 31))
        .baselineValues(LocalDate.of(1969, 12, 30))
        .baselineValues(LocalDate.of(1900, 1, 1))
        .baselineValues(LocalDate.of(2015, 1, 1));
  }

  /**
   * These are the same values added in the addDateBaselineValues, shifted as corrupt values
   */
  private void addCorruptedDateBaselineValues(TestBuilder builder) {
    builder
        .baselineValues(LocalDate.of(15334, 3, 17))
        .baselineValues(LocalDate.of(15334, 3, 18))
        .baselineValues(LocalDate.of(15334, 3, 15))
        .baselineValues(LocalDate.of(15334, 3, 16))
        .baselineValues(LocalDate.of(15264, 3, 16))
        .baselineValues(LocalDate.of(15379, 3, 17));
  }

  private void readFilesWithUserDisabledAutoCorrection() throws Exception {
    // ensure that selecting the date column explicitly or as part of a star still results
    // in checking the file metadata for date columns (when we need to check the statistics
    // for bad values) to set the flag that the values are corrupt
    for (String selection : new String[] {"*", "date_col"}) {
      TestBuilder builder = testBuilder()
          .sqlQuery("select %s from table(dfs.`%s` (type => 'parquet', autoCorrectCorruptDates => false))",
              selection, MIXED_CORRUPTED_AND_CORRECT_DATES_PATH)
          .unOrdered()
          .baselineColumns("date_col");
      addDateBaselineValues(builder);
      addCorruptedDateBaselineValues(builder);
      addCorruptedDateBaselineValues(builder);
      addCorruptedDateBaselineValues(builder);
      builder.go();
    }
  }
}
