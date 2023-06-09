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
package org.apache.drill.metastore.components.tables;

import org.apache.drill.categories.MetastoreTest;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(MetastoreTest.class)
public class TestMetastoreTableInfo extends BaseTest {

  @Test
  public void testAbsentTable() {
    MetastoreTableInfo metadata = MetastoreTableInfo.of(TableInfo.UNKNOWN_TABLE_INFO, null, 1);
    assertFalse(metadata.isExists());
  }

  @Test
  public void testExistingTable() {
    TableMetadataUnit unit = TableMetadataUnit.builder().lastModifiedTime(System.currentTimeMillis()).build();
    MetastoreTableInfo metadata = MetastoreTableInfo.of(TableInfo.UNKNOWN_TABLE_INFO, unit, 1);
    assertTrue(metadata.isExists());
  }

  @Test
  public void testHasChangedTwoAbsentTables() {
    MetastoreTableInfo metadata = MetastoreTableInfo.of(TableInfo.UNKNOWN_TABLE_INFO, null, 1);
    assertFalse(metadata.hasChanged(false, null));
  }

  @Test
  public void testHasChangedFirstAbsentTable() {
    MetastoreTableInfo metadata = MetastoreTableInfo.of(TableInfo.UNKNOWN_TABLE_INFO, null, 1);
    assertTrue(metadata.hasChanged(true, null));
  }

  @Test
  public void testHasChangedSecondAbsentTable() {
    TableMetadataUnit unit = TableMetadataUnit.builder().lastModifiedTime(System.currentTimeMillis()).build();
    MetastoreTableInfo metadata = MetastoreTableInfo.of(TableInfo.UNKNOWN_TABLE_INFO, unit, 1);
    assertTrue(metadata.hasChanged(false, null));
  }

  @Test
  public void testHasChangedSameNullModificationTime() {
    TableMetadataUnit unit = TableMetadataUnit.builder().build();
    MetastoreTableInfo metadata = MetastoreTableInfo.of(TableInfo.UNKNOWN_TABLE_INFO, unit, 1);
    assertFalse(metadata.hasChanged(true, null));
  }

  @Test
  public void testHasChangedSameDefinedModificationTime() {
    long lastModificationTime = System.currentTimeMillis();
    TableMetadataUnit unit = TableMetadataUnit.builder().lastModifiedTime(lastModificationTime).build();
    MetastoreTableInfo metadata = MetastoreTableInfo.of(TableInfo.UNKNOWN_TABLE_INFO, unit, 1);
    assertFalse(metadata.hasChanged(true, lastModificationTime));
  }

  @Test
  public void testHasChangedDifferentDefinedModificationTime() {
    TableMetadataUnit unit = TableMetadataUnit.builder().lastModifiedTime(100L).build();
    MetastoreTableInfo metadata = MetastoreTableInfo.of(TableInfo.UNKNOWN_TABLE_INFO, unit, 1);
    assertTrue(metadata.hasChanged(true, 200L));
    assertTrue(metadata.hasChanged(true, -1L));
  }

  @Test
  public void testHasChangedDefinedAndNullModificationTime() {
    TableMetadataUnit unit = TableMetadataUnit.builder().lastModifiedTime(System.currentTimeMillis()).build();
    MetastoreTableInfo metadata = MetastoreTableInfo.of(TableInfo.UNKNOWN_TABLE_INFO, unit, 1);
    assertTrue(metadata.hasChanged(true, null));
  }

  @Test
  public void testHasChangedNullAndDefinedModificationTime() {
    MetastoreTableInfo metadata = MetastoreTableInfo.of(TableInfo.UNKNOWN_TABLE_INFO, null, 1);
    assertTrue(metadata.hasChanged(true, System.currentTimeMillis()));
  }
}
