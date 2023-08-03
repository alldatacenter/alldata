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

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.io.writer.ArcticTreeNodePosDeleteWriter;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.List;

@RunWith(Parameterized.class)
public class TestArcticTreeNodePosDeleteWriter extends TableTestBase {

  public TestArcticTreeNodePosDeleteWriter(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "{1},{2}")
  public static Object[] parameters() {
    return new Object[][] {{new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(true, true)
                           },
                           {
                               new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                               new BasicTableTestHelper(true, false)
                           }};
  }

  @Test
  public void test() throws IOException {
    UnkeyedTable table = getArcticTable().asKeyedTable().baseTable();
    GenericAppenderFactory appenderFactory =
        new GenericAppenderFactory(table.schema(), table.spec());

    StructLike partitionData = GenericRecord.create(table.spec().schema());
    partitionData.set(0, 1);

    ArcticTreeNodePosDeleteWriter<Record> writer = new ArcticTreeNodePosDeleteWriter<>(
        appenderFactory,
        FileFormat.PARQUET,
        partitionData,
        table.io(),
        table.encryption(),
        1L,
        table.location(),
        table.spec()
    );

    writer.setTreeNode(DataTreeNode.ofId(4));
    writer.delete("a", 0);

    writer.setTreeNode(DataTreeNode.ofId(5));
    writer.delete("b", 0);

    writer.setTreeNode(DataTreeNode.ofId(6));
    writer.delete("c", 0);

    writer.setTreeNode(DataTreeNode.ofId(7));
    writer.delete("d", 0);

    List<DeleteFile> complete = writer.complete();

    Assert.assertEquals(complete.size(), 4);
  }
}
