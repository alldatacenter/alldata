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
package org.apache.drill.test.rowSet.test;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocator;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.BaseTest;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestRowSetComparison extends BaseTest {
  private BufferAllocator allocator;

  @Before
  public void setup() {
    allocator = new RootAllocator(Long.MAX_VALUE);
  }

  @Test
  public void simpleUnorderedComparisonMatchTest() {
    final TupleMetadata schema = new SchemaBuilder()
      .add("a", TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED)
      .add("b", TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    final RowSet expected = new RowSetBuilder(allocator, schema)
      .addRow(1, 1)
      .addRow(1, 1)
      .addRow(1, 2)
      .addRow(2, 1)
      .build();

    final RowSet actual = new RowSetBuilder(allocator, schema)
      .addRow(1, 1)
      .addRow(1, 2)
      .addRow(2, 1)
      .addRow(1, 1)
      .build();

    try {
      new RowSetComparison(expected).unorderedVerify(actual);
    } finally {
      expected.clear();
      actual.clear();
    }
  }

  @Test
  public void simpleDoubleUnorderedComparisonMatchTest() {
    final TupleMetadata schema = new SchemaBuilder()
      .add("a", TypeProtos.MinorType.FLOAT4, TypeProtos.DataMode.REQUIRED)
      .add("b", TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    final RowSet expected = new RowSetBuilder(allocator, schema)
      .addRow(1.0f, 1.0)
      .addRow(1.0f, 1.0)
      .addRow(1.0f, 1.01)
      .addRow(1.01f, 1.0)
      .build();

    final RowSet actual = new RowSetBuilder(allocator, schema)
      .addRow(1.004f, .9996)
      .addRow(1.0f, 1.008)
      .addRow(1.008f, 1.0)
      .addRow(.9996f, 1.004)
      .build();

    try {
      new RowSetComparison(expected).unorderedVerify(actual);
    } finally {
      expected.clear();
      actual.clear();
    }
  }

  @Test
  public void simpleVarcharUnorderedComparisonMatchTest() {
    final TupleMetadata schema = new SchemaBuilder()
      .add("a", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    final RowSet expected = new RowSetBuilder(allocator, schema)
      .addRow("aaa")
      .addRow("bbb")
      .addRow("ccc")
      .addRow("bbb")
      .build();

    final RowSet actual = new RowSetBuilder(allocator, schema)
      .addRow("ccc")
      .addRow("aaa")
      .addRow("bbb")
      .addRow("bbb")
      .build();

    try {
      new RowSetComparison(expected).unorderedVerify(actual);
    } finally {
      expected.clear();
      actual.clear();
    }
  }

  @Test(expected = AssertionError.class)
  public void simpleUnorderedComparisonNoMatchTest() {
    final TupleMetadata schema = new SchemaBuilder()
      .add("a", TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED)
      .add("b", TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    final RowSet expected = new RowSetBuilder(allocator, schema)
      .addRow(1, 1)
      .addRow(3, 2)
      .addRow(2, 4)
      .build();

    final RowSet actual = new RowSetBuilder(allocator, schema)
      .addRow(1, 1)
      .addRow(2, 1)
      .addRow(1, 1)
      .build();

    try {
      new RowSetComparison(expected).unorderedVerify(actual);
    } finally {
      expected.clear();
      actual.clear();
    }
  }

  @Test(expected = AssertionError.class)
  public void simpleDoubleUnorderedComparisonNoMatchTest() {
    final TupleMetadata schema = new SchemaBuilder()
      .add("a", TypeProtos.MinorType.FLOAT4, TypeProtos.DataMode.REQUIRED)
      .add("b", TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    final RowSet expected = new RowSetBuilder(allocator, schema)
      .addRow(1.0f, 1.0)
      .addRow(1.0f, 1.0)
      .addRow(1.0f, 1.01)
      .addRow(1.01f, 1.0)
      .build();

    final RowSet actual = new RowSetBuilder(allocator, schema)
      .addRow(1.009f, .9996)
      .addRow(1.0f, 1.004)
      .addRow(1.008f, 1.0)
      .addRow(.9994f, 1.004)
      .build();

    try {
      new RowSetComparison(expected).unorderedVerify(actual);
    } finally {
      expected.clear();
      actual.clear();
    }
  }

  @Test(expected = AssertionError.class)
  public void simpleVarcharUnorderedComparisonNoMatchTest() {
    final TupleMetadata schema = new SchemaBuilder()
      .add("a", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    final RowSet expected = new RowSetBuilder(allocator, schema)
      .addRow("red")
      .addRow("bbb")
      .addRow("ccc")
      .addRow("bbb")
      .build();

    final RowSet actual = new RowSetBuilder(allocator, schema)
      .addRow("ccc")
      .addRow("aaa")
      .addRow("blue")
      .addRow("bbb")
      .build();

    try {
      new RowSetComparison(expected).unorderedVerify(actual);
    } finally {
      expected.clear();
      actual.clear();
    }
  }

  @After
  public void teardown() {
    allocator.close();
  }
}
