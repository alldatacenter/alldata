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
import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Category(MetastoreTest.class)
public class TestBasicTablesRequestsRequestMetadata extends BaseTest {

  @Test
  public void testRequestMetadataWithoutRequestColumns() {
    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
      .column("col")
      .metadataKeys(Arrays.asList("a", "b", "c"))
      .build();

    assertTrue(requestMetadata.columns().isEmpty());
  }

  @Test
  public void testRequestMetadataWithRequestColumns() {
    List<MetastoreColumn> requestColumns = Arrays.asList(MetastoreColumn.STORAGE_PLUGIN, MetastoreColumn.SCHEMA);
    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
      .column("col")
      .metadataKeys(Arrays.asList("a", "b", "c"))
      .requestColumns(requestColumns)
      .build();

    assertEquals(requestColumns, requestMetadata.columns());
  }

  @Test
  public void testRequestMetadataWithEmptyRequestColumns() {
    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
      .column("col")
      .metadataKeys(Arrays.asList("a", "b", "c"))
      .requestColumns()
      .build();

    assertEquals(Collections.emptyList(), requestMetadata.columns());
  }

  @Test
  public void testRequestMetadataNoFilter() {
    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder().build();
    assertNull(requestMetadata.filter());
  }

  @Test
  public void testRequestMetadataOneFilter() {
    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
      .column("col")
      .build();

    FilterExpression expected = FilterExpression.equal(MetastoreColumn.COLUMN, "col");

    assertEquals(expected.toString(), requestMetadata.filter().toString());
  }

  @Test
  public void testRequestMetadataWithAndFilter() {
    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
      .location("/tmp/dir")
      .column("col")
      .build();

    FilterExpression expected = FilterExpression.and(
      FilterExpression.equal(MetastoreColumn.LOCATION, "/tmp/dir"),
      FilterExpression.equal(MetastoreColumn.COLUMN, "col"));

    assertEquals(expected.toString(), requestMetadata.filter().toString());
  }

  @Test
  public void testRequestMetadataWithInFilter() {
    List<String> locations = Arrays.asList("/tmp/dir0", "/tmp/dir1");
    List<String> metadataKeys = Arrays.asList("a", "b", "c");

    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
      .locations(locations)
      .metadataKeys(metadataKeys)
      .build();

    FilterExpression expected = FilterExpression.and(
      FilterExpression.in(MetastoreColumn.LOCATION, locations),
      FilterExpression.in(MetastoreColumn.METADATA_KEY, metadataKeys));

    assertEquals(expected.toString(), requestMetadata.filter().toString());
  }

  @Test
  public void testRequestMetadataWithCustomFilter() {
    String column = "col";
    List<String> metadataKeys = Arrays.asList("a", "b", "c");
    FilterExpression customFilter = FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs");

    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
      .column(column)
      .metadataKeys(metadataKeys)
      .customFilter(customFilter)
      .build();

    FilterExpression expected = FilterExpression.and(
      FilterExpression.equal(MetastoreColumn.COLUMN, column),
      FilterExpression.in(MetastoreColumn.METADATA_KEY, metadataKeys),
      customFilter);

    assertEquals(expected.toString(), requestMetadata.filter().toString());
  }

  @Test
  public void testRequestMetadataWithMetadataType() {
    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
      .metadataType(MetadataType.TABLE)
      .build();

    assertEquals(1, requestMetadata.metadataTypes().size());
    assertEquals(MetadataType.TABLE, requestMetadata.metadataTypes().iterator().next());
  }

  @Test
  public void testRequestMetadataWithMetadataTypes() {
    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
      .metadataTypes(MetadataType.TABLE, MetadataType.SEGMENT, MetadataType.PARTITION)
      .metadataTypes(MetadataType.PARTITION, MetadataType.FILE)
      .build();

    assertEquals(4, requestMetadata.metadataTypes().size());
  }
}
