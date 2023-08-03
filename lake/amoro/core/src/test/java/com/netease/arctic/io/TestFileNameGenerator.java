/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.io;

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.data.FileNameRules;
import com.netease.arctic.io.writer.TaskWriterKey;
import org.apache.iceberg.FileFormat;
import org.junit.Assert;
import org.junit.Test;

public class TestFileNameGenerator {

  @Test
  public void newBaseFileName() {
    FileNameRules fileNameGenerator = new FileNameRules(FileFormat.PARQUET, 0, 1L, 2L);
    TaskWriterKey writerKey = new TaskWriterKey(null, DataTreeNode.of(3, 3), DataFileType.BASE_FILE);
    String fileName = fileNameGenerator.fileName(writerKey);
    System.out.println(fileName);
    DefaultKeyedFile.FileMeta fileMeta = FileNameRules.parseBase(fileName);
    Assert.assertEquals(fileMeta.node(), DataTreeNode.of(3, 3));
    Assert.assertEquals(fileMeta.type(), DataFileType.BASE_FILE);
    Assert.assertEquals(fileMeta.transactionId(), 2L);
  }

  @Test
  public void hiveFileName() {
    DefaultKeyedFile.FileMeta fileMeta = FileNameRules.parseBase("a");
    Assert.assertEquals(fileMeta.node(), DataTreeNode.of(0, 0));
    Assert.assertEquals(fileMeta.type(), DataFileType.BASE_FILE);
    Assert.assertEquals(fileMeta.transactionId(), 0L);
  }

  @Test
  public void flinkChangeFile2Base() {
    FileNameRules fileNameGenerator = new FileNameRules(FileFormat.PARQUET, 0, 1L, null);
    TaskWriterKey writerKey = new TaskWriterKey(null, DataTreeNode.of(3, 3), DataFileType.INSERT_FILE);
    String fileName = fileNameGenerator.fileName(writerKey);
    DefaultKeyedFile.FileMeta fileMeta = FileNameRules.parseBase(fileName);
    Assert.assertEquals(fileMeta.node(), DataTreeNode.of(3, 3));
    Assert.assertEquals(fileMeta.type(), DataFileType.BASE_FILE);
    Assert.assertEquals(fileMeta.transactionId(), 0L);
  }

  @Test
  public void flinkChangeFile() {
    FileNameRules fileNameGenerator = new FileNameRules(FileFormat.PARQUET, 0, 1L, null);
    TaskWriterKey writerKey = new TaskWriterKey(null, DataTreeNode.of(3, 3), DataFileType.INSERT_FILE);
    String fileName = fileNameGenerator.fileName(writerKey);
    DefaultKeyedFile.FileMeta fileMeta = FileNameRules.parseChange(fileName, 5L);
    Assert.assertEquals(fileMeta.node(), DataTreeNode.of(3, 3));
    Assert.assertEquals(fileMeta.type(), DataFileType.INSERT_FILE);
    Assert.assertEquals(fileMeta.transactionId(), 5L);
  }

  @Test
  public void sparkChangeFile() {
    FileNameRules fileNameGenerator = new FileNameRules(FileFormat.PARQUET, 0, 1L, 5L);
    TaskWriterKey writerKey = new TaskWriterKey(null, DataTreeNode.of(3, 3), DataFileType.INSERT_FILE);
    String fileName = fileNameGenerator.fileName(writerKey);
    DefaultKeyedFile.FileMeta fileMeta = FileNameRules.parseChange(fileName, 6L);
    Assert.assertEquals(fileMeta.node(), DataTreeNode.of(3, 3));
    Assert.assertEquals(fileMeta.type(), DataFileType.INSERT_FILE);
    Assert.assertEquals(fileMeta.transactionId(), 5L);
  }

  @Test
  public void adaptOldFileName() {
    DefaultKeyedFile.FileMeta fileMeta = FileNameRules.parseChange(
        "hdfs://easyops-sloth/user/warehouse/animal_partition_two/base/5-I-2-00000-941953957-0000000001.parquet", 6L);
    Assert.assertEquals(fileMeta.node(), DataTreeNode.of(3, 1));
    Assert.assertEquals(fileMeta.type(), DataFileType.INSERT_FILE);
    Assert.assertEquals(fileMeta.transactionId(), 2L);
  }
}
