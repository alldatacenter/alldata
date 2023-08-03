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

package com.netease.arctic.server.optimizing.scan;

import com.netease.arctic.TableTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.data.PrimaryKeyedFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.junit.Assert;

import java.util.List;

public abstract class TableFileScanHelperTestBase extends TableTestBase {
  public TableFileScanHelperTestBase(CatalogTestHelper catalogTestHelper,
                                     TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  protected void assertScanResult(List<TableFileScanHelper.FileScanResult> result, int size, Integer deleteCnt) {
    assertScanResult(result, size, null, deleteCnt);
  }

  protected void assertScanResult(List<TableFileScanHelper.FileScanResult> result, int size, Long sequence) {
    assertScanResult(result, size, sequence, null);
  }

  protected void assertScanResult(List<TableFileScanHelper.FileScanResult> result, int size) {
    assertScanResult(result, size, null, null);
  }

  protected void assertScanResult(List<TableFileScanHelper.FileScanResult> result, int size, Long sequence,
                                  Integer deleteCnt) {
    Assert.assertEquals(size, result.size());
    for (TableFileScanHelper.FileScanResult fileScanResult : result) {
      IcebergDataFile file = fileScanResult.file();
      assertDataFileClass(file);
      if (sequence != null) {
        Assert.assertEquals(sequence.longValue(), file.getSequenceNumber());
      }
      if (deleteCnt != null) {
        Assert.assertEquals(deleteCnt.intValue(), fileScanResult.deleteFiles().size());
      }
      for (IcebergContentFile<?> deleteFile : fileScanResult.deleteFiles()) {
        if (deleteFile.content() == FileContent.DATA) {
          Assert.assertTrue(deleteFile.internalFile() instanceof PrimaryKeyedFile);
          PrimaryKeyedFile primaryKeyedFile = (PrimaryKeyedFile) deleteFile.internalFile();
          Assert.assertEquals(DataFileType.EQ_DELETE_FILE, primaryKeyedFile.type());
        } else {
          Assert.assertTrue(deleteFile.internalFile() instanceof DeleteFile);
        }
      }
    }
  }

  protected void assertDataFileClass(IcebergDataFile file) {
    Assert.assertTrue(file.internalFile() instanceof PrimaryKeyedFile);
  }

  protected String getPartition() {
    return isPartitionedTable() ? "op_time_day=2022-01-01" : "";
  }

  protected abstract TableFileScanHelper buildFileScanHelper();
}
