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

package com.netease.arctic.op;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.TestHelpers;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.util.StructLikeMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class TestUpdatePartitionProperties extends TableTestBase {

  public TestUpdatePartitionProperties() {
    super(new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
        new BasicTableTestHelper(false, true));
  }

  @Test
  public void testUpdatePartitionProperties() {
    StructLikeMap<Map<String, String>> partitionProperties = getArcticTable().asUnkeyedTable().partitionProperty();
    Assert.assertEquals(0, partitionProperties.size());
    StructLike p0 = TestHelpers.Row.of(1200);
    getArcticTable().asUnkeyedTable().updatePartitionProperties(null).set(p0, "key", "value").commit();
    partitionProperties = getArcticTable().asUnkeyedTable().partitionProperty();
    Assert.assertEquals(1, partitionProperties.size());
    Assert.assertEquals("value", partitionProperties.get(p0).get("key"));
  }

  @Test
  public void testUpdatePartitionPropertiesInTx() {
    StructLikeMap<Map<String, String>> partitionProperties = getArcticTable().asUnkeyedTable().partitionProperty();
    Transaction transaction = getArcticTable().asUnkeyedTable().newTransaction();
    Assert.assertEquals(0, partitionProperties.size());
    StructLike p0 = TestHelpers.Row.of(1200);
    getArcticTable().asUnkeyedTable().updatePartitionProperties(transaction).set(p0, "key", "value").commit();
    partitionProperties = getArcticTable().asUnkeyedTable().partitionProperty();
    Assert.assertEquals(0, partitionProperties.size());
    transaction.commitTransaction();
    partitionProperties = getArcticTable().asUnkeyedTable().partitionProperty();
    Assert.assertEquals(1, partitionProperties.size());
    Assert.assertEquals("value", partitionProperties.get(p0).get("key"));
  }

  @Test
  public void testRemovePartitionProperties() {
    StructLikeMap<Map<String, String>> partitionProperties = getArcticTable().asUnkeyedTable().partitionProperty();
    Assert.assertEquals(0, partitionProperties.size());
    StructLike p0 = TestHelpers.Row.of(1200);
    getArcticTable().asUnkeyedTable().updatePartitionProperties(null).set(p0, "key", "value").commit();
    partitionProperties = getArcticTable().asUnkeyedTable().partitionProperty();
    Assert.assertEquals(1, partitionProperties.get(p0).size());

    getArcticTable().asUnkeyedTable().updatePartitionProperties(null).remove(p0, "key").commit();
    partitionProperties = getArcticTable().asUnkeyedTable().partitionProperty();
    Assert.assertEquals(0, partitionProperties.get(p0).size());
  }
}
