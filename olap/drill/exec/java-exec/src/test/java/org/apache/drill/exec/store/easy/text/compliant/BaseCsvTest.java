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
package org.apache.drill.exec.store.easy.text.compliant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin.TextFormatConfig;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;

public class BaseCsvTest extends ClusterTest {

  protected final int BIG_COL_SIZE = 70_000;

  protected static final String PART_DIR = "root";
  protected static final String NESTED_DIR = "nested";
  protected static final String ROOT_FILE = "first.csv";
  protected static final String NESTED_FILE = "second.csv";
  protected static final String EMPTY_FILE = "empty.csv";

  /**
   * The scan operator can return an empty schema batch as
   * the first batch. But, this broke multiple operators that
   * do not handle this case. So, it is turned off for now.
   * Tests that verified the empty batch use this flag to
   * disable that checking.
   */

  protected static boolean SCHEMA_BATCH_ENABLED = false;

  protected static String validHeaders[] = {
      "a,b,c",
      "10,foo,bar"
  };

  protected static String secondFile[] = {
      "a,b,c",
      "20,fred,wilma"
  };

  protected static File testDir;

  protected static void setup(boolean skipFirstLine, boolean extractHeader) throws Exception {
    setup(skipFirstLine, extractHeader, 1);
  }

  protected static void setup(boolean skipFirstLine, boolean extractHeader,
      int maxParallelization) throws Exception {
    startCluster(
        ClusterFixture.builder(dirTestWatcher)
        .maxParallelization(maxParallelization));

    // Set up CSV storage plugin using headers.
    TextFormatConfig csvFormat = new TextFormatConfig(
        null,
        null,  // line delimiter
        null,  // field delimiter
        null,  // quote
        null,  // escape
        null,  // comment
        skipFirstLine,
        extractHeader
        );

    testDir = cluster.makeDataDir("data", "csv", csvFormat);
  }

  protected static void buildNestedTable() throws IOException {

    // Two-level partitioned table

    File rootDir = new File(testDir, PART_DIR);
    rootDir.mkdir();
    buildFile(new File(rootDir, ROOT_FILE), validHeaders);
    File nestedDir = new File(rootDir, NESTED_DIR);
    nestedDir.mkdir();
    buildFile(new File(nestedDir, NESTED_FILE), secondFile);
  }

  protected void enableMultiScan() {

    // Special test-only feature to force even small scans
    // to use more than one thread. Requires that the max
    // parallelization option be set when starting the cluster.

    client.alterSession(ExecConstants.MIN_READER_WIDTH_KEY, 2);
  }

  protected void resetMultiScan() {
    client.resetSession(ExecConstants.MIN_READER_WIDTH_KEY);
  }

  protected void enableSchema(boolean enable) {
    client.alterSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE, enable);
  }

  protected void resetSchema() {
    client.resetSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE);
  }

  protected static void buildFile(String fileName, String[] data) throws IOException {
    buildFile(new File(testDir, fileName), data);
  }

  protected static void buildFile(File file, String[] data) throws IOException {
    try(PrintWriter out = new PrintWriter(new FileWriter(file))) {
      for (String line : data) {
        out.println(line);
      }
    }
  }

  protected String buildBigColFile(boolean withHeader) throws IOException {
    String fileName = "hugeCol.csv";
    try(PrintWriter out = new PrintWriter(new FileWriter(new File(testDir, fileName)))) {
      if (withHeader) {
        out.println("id,big,n");
      }
      for (int i = 0; i < 10; i++) {
        out.print(i + 1);
        out.print(",");
        for (int j = 0; j < BIG_COL_SIZE; j++) {
          out.print((char) ((j + i) % 26 + 'A'));
        }
        out.print(",");
        out.println((i + 1) * 10);
      }
    }
    return fileName;
  }

  protected static final String FILE_N_NAME = "file%d.csv";

  protected static String buildTable(String tableName, String[]...fileContents) throws IOException {
    File rootDir = new File(testDir, tableName);
    rootDir.mkdir();
    for (int i = 0; i < fileContents.length; i++) {
      String fileName = String.format(FILE_N_NAME, i);
      buildFile(new File(rootDir, fileName), fileContents[i]);
    }
    return "`dfs.data`.`" + tableName + "`";
  }

  protected void enableSchemaSupport() {
    enableSchema(true);
  }

  protected void resetSchemaSupport() {
    resetSchema();
  }
}
