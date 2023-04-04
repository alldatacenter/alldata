/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.service.impl;

import com.netease.arctic.TableTestBase;
import com.netease.arctic.io.TableTrashManagers;
import com.netease.arctic.table.ArcticTable;
import org.apache.iceberg.io.OutputFile;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TrashCleanServiceTest extends TableTestBase {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  @Test
  public void clean() throws IOException {
    ArcticTable table = this.testKeyedTable;
    String trashLocation = TableTrashManagers.build(table).getTrashLocation();
    String file1 = createTrashFileInDay(table, trashLocation, 9, "test1.parquet");
    String file2 = createTrashFileInDay(table, trashLocation, 9, "test2.parquet");
    String file3 = createTrashFileInDay(table, trashLocation, 9, "test/test3.parquet");
    String file4 = createTrashFileInDay(table, trashLocation, 8, "test4.parquet");
    String file5 = createTrashFileInDay(table, trashLocation, 7, "test5.parquet");
    String file6 = createTrashFileInDay(table, trashLocation, 6, "test6.parquet");
    String file7 = createTrashFileInDay(table, trashLocation, 0, "test7.parquet");
    Assert.assertTrue(table.io().exists(file1));
    Assert.assertTrue(table.io().exists(file2));
    Assert.assertTrue(table.io().exists(file3));
    Assert.assertTrue(table.io().exists(file4));
    Assert.assertTrue(table.io().exists(file5));
    Assert.assertTrue(table.io().exists(file6));
    Assert.assertTrue(table.io().exists(file7));

    TrashCleanService.clean(table);

    Assert.assertFalse(table.io().exists(file1));
    Assert.assertFalse(table.io().exists(file2));
    Assert.assertFalse(table.io().exists(file3));
    Assert.assertFalse(table.io().exists(file4));
    Assert.assertTrue(table.io().exists(file5));
    Assert.assertTrue(table.io().exists(file6));
    Assert.assertTrue(table.io().exists(file7));
  }

  private String createTrashFileInDay(ArcticTable table, String trashLocation, int day, String fileName)
      throws IOException {
    String filePath = trashLocation + "/" + LocalDate.now().minusDays(day).format(DATE_FORMATTER) + "/" + fileName;
    OutputFile outputFile = table.io().newOutputFile(filePath);
    outputFile.createOrOverwrite().close();
    return filePath;
  }
}