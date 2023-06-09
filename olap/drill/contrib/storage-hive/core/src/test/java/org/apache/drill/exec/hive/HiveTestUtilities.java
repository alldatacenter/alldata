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
package org.apache.drill.exec.hive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.TestTools;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.processors.CommandProcessorResponse;
import org.apache.hadoop.util.ComparableVersion;
import org.apache.hive.common.util.HiveVersionInfo;
import org.junit.AssumptionViolatedException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;

public class HiveTestUtilities {

  /**
   * Set of all posix permissions to be assigned to newly created file in
   * {@link HiveTestUtilities#createDirWithPosixPermissions(File, String)}
   */
  private static final Set<PosixFilePermission> ALL_POSIX_PERMISSIONS = EnumSet.allOf(PosixFilePermission.class);

  /**
   * Execute the give <i>query</i> on given <i>hiveDriver</i> instance.
   */
  public static void executeQuery(Driver hiveDriver, String query) {
    CommandProcessorResponse response;
    try {
      response = hiveDriver.run(query);
    } catch (Exception e) {
       throw new RuntimeException(e);
    }

    if (response.getResponseCode() != 0 ) {
      throw new RuntimeException(String.format("Failed to execute command '%s', errorMsg = '%s'",
          query, response.getErrorMessage()));
    }
  }

  /**
   * Creates desired directory structure and
   * adds all posix permissions to created directory.
   *
   * @param parentDir parent directory
   * @param dirName directory name
   * @return file representing created dir with all posix permissions
   */
  public static File createDirWithPosixPermissions(File parentDir, String dirName) {
    File dir = new File(parentDir, dirName);
    dir.mkdirs();
    Path path = dir.toPath();
    try {
      Files.setPosixFilePermissions(path, ALL_POSIX_PERMISSIONS);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to set all posix permissions for directory [%s]", dir), e);
    }
    return dir;
  }

  /**
   * Load data from test resources file into table.
   *
   * @param driver hive driver
   * @param tableName destination
   * @param relativeTestResourcePath path to test resource
   */
  public static void loadData(Driver driver, String tableName, Path relativeTestResourcePath){
    String dataAbsPath = TestTools.getResourceFile(relativeTestResourcePath).getAbsolutePath();
    String loadDataSql = String.format("LOAD DATA LOCAL INPATH '%s' OVERWRITE INTO TABLE %s", dataAbsPath, tableName);
    executeQuery(driver, loadDataSql);
  }

  /**
   * Performs insert from select.
   *
   * @param driver hive driver
   * @param srcTable source
   * @param destTable destination
   */
  public static void insertData(Driver driver, String srcTable, String destTable){
    executeQuery(driver, String.format(
        "INSERT OVERWRITE TABLE %s SELECT * FROM %s",
        destTable, srcTable
    ));
  }

  /**
   * Helper method used to ensure that native parquet scan will be used
   * for table selection.
   *
   * @param queryBuilder test query builder
   * @param table table to check
   * @throws Exception may be thrown while getting query plan
   */
  public static void assertNativeScanUsed(QueryBuilder queryBuilder, String table) throws Exception {
    String plan = queryBuilder.sql("SELECT * FROM hive.`%s`", table).explainText();
    assertThat(plan, containsString("HiveDrillNativeParquetScan"));
  }

  /**
   * Current Hive version doesn't support JDK 9+.
   * Checks if current version is supported by Hive.
   *
   * @return {@code true} if current version is supported by Hive, {@code false} otherwise
   */
  public static boolean supportedJavaVersion() {
    return System.getProperty("java.version").startsWith("1.8");
  }

  /**
   * Checks whether current version is not less than hive 3.0
   */
  public static boolean isHive3() {
    return new ComparableVersion(HiveVersionInfo.getVersion())
        .compareTo(new ComparableVersion("3.0")) >= 0;
  }

  /**
   * Checks if current version is supported by Hive.
   *
   * @throws AssumptionViolatedException if current version is not supported by Hive,
   * so unit tests may be skipped.
   */
  public static void assumeJavaVersion() throws AssumptionViolatedException {
    assumeThat("Skipping tests since Hive supports only JDK 8.", System.getProperty("java.version"), startsWith("1.8"));
  }
}
