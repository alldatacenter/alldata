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

package org.apache.drill.exec.store.jdbc;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.drill.exec.ExecTest;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import static org.apache.drill.test.ClusterTest.dirTestWatcher;

public class JdbcTestUtils {

  private final static Logger logger = LoggerFactory.getLogger(JdbcTestUtils.class);

  /**
   * Generates a random string of length n
   * @param length Length of desired string
   * @return Random string
   */
  public static String generateRandomString(int length) {
    return RandomStringUtils.random(length, true, false);
  }

  /**
   * Generates a CSV file of random data with the desired number of columns and rows. The
   * data will be a mix of numbers and text
   * @param columnCount Number of columns
   * @param rowCount Number of rows
   */
  public static Path generateCsvFile(String fileName, int columnCount, int rowCount) throws IOException {
    final String COLUMN_NAME_TEMPLATE = "col_";
    StringBuilder row = new StringBuilder();
    Random random = new Random();

    FileSystem fs = ExecTest.getLocalFileSystem();
    Path outFile = new Path(dirTestWatcher.getRootDir().getAbsolutePath(), fileName);

    FileWriter fileWriter = new FileWriter(String.valueOf(outFile));
    PrintWriter printWriter = new PrintWriter(fileWriter);

    for (int i = 0; i <= rowCount; i++) {
      for (int j = 0; j < columnCount; j++) {
        // Generate header row
        if (i == 0) {
          row.append(COLUMN_NAME_TEMPLATE).append(j);
        } else {
          if (j % 2 == 0) {
            row.append(generateRandomString(8));
          } else {
            row.append(random.nextInt(1_000_000));
          }
        }
        if (j < columnCount -1) {
          row.append(",");
        }
      }
      printWriter.println(row);
      row = new StringBuilder();
    }
    printWriter.close();
    return outFile;
  }

  /**
   * Deletes the file
   * @param filepath The file to be deleted
   * @return True if the file was successfully deleted, false if not
   */
  public static boolean deleteCsvFile(String filepath) {
    File tempFile = new File(filepath);
    return tempFile.delete();
  }
}
