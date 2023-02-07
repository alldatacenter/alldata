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

import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.nio.file.Path;

import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOf;

@Category({ParquetTest.class, UnlikelyTest.class})
public class TestParquetScan extends BaseTestQuery {
  @Test
  public void testSuccessFile() throws Exception {
    final byte[] bytes = Resources.toByteArray(Resources.getResource("tpch/nation.parquet"));

    final Path rootPath = dirTestWatcher.getRootDir().toPath();
    final File scanFile = rootPath.resolve("nation_test_parquet_scan").toFile();
    final File successFile = rootPath.resolve("_SUCCESS").toFile();
    final File logsFile = rootPath.resolve("_logs").toFile();

    FileUtils.writeByteArrayToFile(scanFile, bytes);
    successFile.createNewFile();
    logsFile.createNewFile();

    testBuilder()
        .sqlQuery("select count(*) c from dfs.nation_test_parquet_scan where 1 = 1")
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(25L)
        .build()
        .run();
  }

  // DRILL-7934: Fix NullPointerException error when reading parquet files
  @Test
  public void testTypeNull() throws Exception {
    /* the `features` schema is:
    optional group features {
      required int32 type (INTEGER(8,true));
      optional int32 size;
      optional group indices (LIST) {
        repeated group list {
          required int32 element;
        }
      }
      optional group values (LIST) {
        repeated group list {
          required double element;
        }
      }
    }
    */
    String sql = "SELECT * FROM cp.`parquet/test_type_null.parquet`";
    testBuilder()
            .sqlQuery(sql)
            .unOrdered()
            .baselineColumns("label", "features")
            .baselineValues(0.0d,
                    mapOf("type", 1,
                    "indices", listOf(),
                    "values", listOf(112.0d, 213.0d, 213.0d)))
            .baselineValues(0.0d,
                    mapOf("type", 1,
                    "indices", listOf(),
                    "values", listOf(213.0d, 123.0d, 123.0d)))
            .baselineValues(2.0d, mapOf(
                    "type", 1,
                    "indices", listOf(),
                    "values", listOf(333.0d, 333.0d, 333.0d)))
            .go();
  }
}
