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
package org.apache.drill.exec.physical.impl.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.file.FileMetadata;
import org.apache.drill.exec.physical.impl.scan.file.FileMetadataColumn;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager.ImplicitColumnOptions;
import org.apache.drill.exec.physical.impl.scan.file.MetadataColumn;
import org.apache.drill.exec.physical.impl.scan.file.PartitionColumn;
import org.apache.drill.exec.physical.impl.scan.project.AbstractUnresolvedColumn.UnresolvedColumn;
import org.apache.drill.exec.physical.impl.scan.project.ColumnProjection;
import org.apache.drill.exec.physical.impl.scan.project.ExplicitSchemaProjection;
import org.apache.drill.exec.physical.impl.scan.project.NullColumnBuilder;
import org.apache.drill.exec.physical.impl.scan.project.NullColumnBuilder.NullBuilderBuilder;
import org.apache.drill.exec.physical.impl.scan.project.ResolvedColumn;
import org.apache.drill.exec.physical.impl.scan.project.ResolvedTuple.ResolvedRow;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.physical.impl.scan.project.ScanLevelProjection;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.SubOperatorTest;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestImplicitColumnProjection extends SubOperatorTest {

  @Test
  public void testImplicitColumnBuilder() {
    {
      // Degenerate case: no file or root
      FileMetadata md = new FileMetadata(null, null);
      assertFalse(md.isSet());
      assertNull(md.filePath());
      assertEquals(0, md.dirPathLength());
      assertNull(md.partition(0));
    }

    {
      // Degenerate case: no file path, but with as selection root
      // Should never occur in practice.
      Path root = new Path("hdfs://a/b");
      FileMetadata md = new FileMetadata(null, root);
      assertFalse(md.isSet());
      assertNull(md.filePath());
      assertEquals(0, md.dirPathLength());
      assertNull(md.partition(0));
    }

    {
      // Simple file, no selection root.
      // Should never really occur, but let's test it anyway.
      Path input = new Path("hdfs://foo.csv");
      FileMetadata md = new FileMetadata(input, null);
      assertTrue(md.isSet());
      assertSame(input, md.filePath());
      assertEquals(0, md.dirPathLength());
      assertNull(md.partition(0));
    }

    {
      // Normal file, no selection root.
      Path input = new Path("hdfs://a/b/c/foo.csv");
      FileMetadata md = new FileMetadata(input, null);
      assertTrue(md.isSet());
      assertSame(input, md.filePath());
      assertEquals(0, md.dirPathLength());
      assertNull(md.partition(0));
    }

    {
      // Normal file, resides in selection root.
      Path root = new Path("hdfs://a/b");
      Path input = new Path("hdfs://a/b/foo.csv");
      FileMetadata md = new FileMetadata(input, root);
      assertTrue(md.isSet());
      assertSame(input, md.filePath());
      assertEquals(0, md.dirPathLength());
      assertNull(md.partition(0));
    }

    {
      // Normal file, below selection root.
      Path root = new Path("hdfs://a/b");
      Path input = new Path("hdfs://a/b/c/foo.csv");
      FileMetadata md = new FileMetadata(input, root);
      assertTrue(md.isSet());
      assertSame(input, md.filePath());
      assertEquals(1, md.dirPathLength());
      assertEquals("c", md.partition(0));
      assertNull(md.partition(1));
    }

    {
      // Normal file, above selection root.
      // This is an error condition.
      Path root = new Path("hdfs://a/b");
      Path input = new Path("hdfs://a/foo.csv");
      try {
        new FileMetadata(input, root);
        fail();
      } catch (IllegalArgumentException e) {
        // Expected
      }
    }

    {
      // Normal file, disjoint with selection root.
      // This is an error condition.
      Path root = new Path("hdfs://a/b");
      Path input = new Path("hdfs://d/foo.csv");
      try {
        new FileMetadata(input, root);
        fail();
      } catch (IllegalArgumentException e) {
        // Expected
      }
    }
  }

  private ImplicitColumnOptions standardOptions(Path filePath) {
    return standardOptions(Lists.newArrayList(filePath));
  }

  private ImplicitColumnOptions standardOptions(List<Path> files) {
    ImplicitColumnOptions options = new ImplicitColumnOptions();
    options.useLegacyWildcardExpansion(false); // Don't expand partition columns for wildcard
    options.setSelectionRoot(new Path("hdfs:///w"));
    options.setFiles(files);
    return options;
  }

  /**
   * Test the file projection planner with metadata.
   */
  @Test
  public void testProjectList() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnManager metadataManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(
            ScanTestUtils.FILE_NAME_COL,
            "a",
            ScanTestUtils.partitionColName(0)),
        ScanTestUtils.parsers(metadataManager.projectionParser()));
    assertEquals(3, scanProj.columns().size());

    // Scan-level projection: defines the columns

    {
      assertTrue(scanProj.columns().get(0) instanceof FileMetadataColumn);
      FileMetadataColumn col0 = (FileMetadataColumn) scanProj.columns().get(0);
      assertTrue(col0 instanceof FileMetadataColumn);
      assertEquals(ScanTestUtils.FILE_NAME_COL, col0.name());
      assertEquals(MinorType.VARCHAR, col0.schema().getType().getMinorType());
      assertEquals(DataMode.REQUIRED, col0.schema().getType().getMode());

      ColumnProjection col1 = scanProj.columns().get(1);
      assertTrue(col1 instanceof UnresolvedColumn);
      assertEquals("a", col1.name());

      assertTrue(scanProj.columns().get(2) instanceof PartitionColumn);
      PartitionColumn col2 = (PartitionColumn) scanProj.columns().get(2);
      assertTrue(col2 instanceof PartitionColumn);
      assertEquals(ScanTestUtils.partitionColName(0), col2.name());
      assertEquals(MinorType.VARCHAR, col2.schema().getType().getMinorType());
      assertEquals(DataMode.OPTIONAL, col2.schema().getType().getMode());
    }

    // Schema-level projection, fills in values.
    TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    metadataManager.startFile(filePath);
    NullColumnBuilder builder = new NullBuilderBuilder().build();
    ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers(metadataManager));

    List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(3, columns.size());

    {
      assertTrue(columns.get(0) instanceof FileMetadataColumn);
      FileMetadataColumn col0 = (FileMetadataColumn) columns.get(0);
      assertTrue(col0 instanceof FileMetadataColumn);
      assertEquals(ScanTestUtils.FILE_NAME_COL, col0.name());
      assertEquals("z.csv", col0.value());
      assertEquals(MinorType.VARCHAR, col0.schema().getType().getMinorType());
      assertEquals(DataMode.REQUIRED, col0.schema().getType().getMode());

      ResolvedColumn col1 = columns.get(1);
      assertEquals("a", col1.name());

      assertTrue(columns.get(2) instanceof PartitionColumn);
      PartitionColumn col2 = (PartitionColumn) columns.get(2);
      assertTrue(col2 instanceof PartitionColumn);
      assertEquals(ScanTestUtils.partitionColName(0), col2.name());
      assertEquals("x", col2.value());
      assertEquals(MinorType.VARCHAR, col2.schema().getType().getMinorType());
      assertEquals(DataMode.OPTIONAL, col2.schema().getType().getMode());
    }

    // Verify that the file metadata columns were picked out

    assertEquals(2, metadataManager.metadataColumns().size());
    assertSame(columns.get(0), metadataManager.metadataColumns().get(0));
    assertSame(columns.get(2), metadataManager.metadataColumns().get(1));
  }

  /**
   * Test a query with explicit mention of file metadata columns.
   */
  @Test
  public void testFileMetadata() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnManager metadataManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(
          "a",
          ScanTestUtils.FULLY_QUALIFIED_NAME_COL,
          "filEPath", // Sic, to test case sensitivity
          ScanTestUtils.FILE_NAME_COL,
          ScanTestUtils.SUFFIX_COL),
        ScanTestUtils.parsers(metadataManager.projectionParser()));
    assertEquals(5, scanProj.columns().size());

    assertEquals(ScanTestUtils.FULLY_QUALIFIED_NAME_COL, scanProj.columns().get(1).name());
    assertEquals("filEPath", scanProj.columns().get(2).name());
    assertEquals(ScanTestUtils.FILE_NAME_COL, scanProj.columns().get(3).name());
    assertEquals(ScanTestUtils.SUFFIX_COL, scanProj.columns().get(4).name());

    // Schema-level projection, fills in values.
    TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    metadataManager.startFile(filePath);
    NullColumnBuilder builder = new NullBuilderBuilder().build();
    ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers(metadataManager));

    List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(5, columns.size());

    assertEquals("/w/x/y/z.csv", ((MetadataColumn) columns.get(1)).value());
    assertEquals("/w/x/y", ((MetadataColumn) columns.get(2)).value());
    assertEquals("z.csv", ((MetadataColumn) columns.get(3)).value());
    assertEquals("csv", ((MetadataColumn) columns.get(4)).value());
  }

  /**
   * Test the obscure case that the partition column contains two digits:
   * dir11. Also tests the obscure case that the output only has partition
   * columns.
   */
  @Test
  public void testPartitionColumnTwoDigits() {
    Path filePath = new Path("hdfs:///x/0/1/2/3/4/5/6/7/8/9/10/d11/z.csv");
    ImplicitColumnManager metadataManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("dir11"),
        ScanTestUtils.parsers(metadataManager.projectionParser()));

    TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    metadataManager.startFile(filePath);
    NullColumnBuilder builder = new NullBuilderBuilder().build();
    ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers(metadataManager));

    List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(1, columns.size());

    assertEquals("d11", ((MetadataColumn) columns.get(0)).value());
  }

  // TODO: Test more partition cols in select than are available dirs
  // TODO: Test customizing partition name, implicit col names.
  // TODO: Test a single-file scan in which the root dir is the same as the one and only file.
}
