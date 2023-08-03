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

package com.netease.arctic.flink.write;

import com.netease.arctic.flink.FlinkTableTestBase;
import com.netease.arctic.table.ArcticTable;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public interface FlinkTaskWriterBaseTest extends FlinkTableTestBase {
  Logger LOG = LoggerFactory.getLogger(FlinkTaskWriterBaseTest.class);

  /**
   * For asserting unkeyed table records.
   */
  String getMetastoreUrl();

  /**
   * For asserting unkeyed table records.
   */
  String getCatalogName();

  default void writeAndCommit(RowData expected, TaskWriter<RowData> taskWriter, ArcticTable arcticTable) throws IOException {
    taskWriter.write(expected);
    WriteResult writerResult = taskWriter.complete();
    boolean writeToBase = arcticTable.isUnkeyedTable();
    commit(arcticTable, writerResult, writeToBase);
    Assert.assertEquals(1, writerResult.dataFiles().length);
  }
}
