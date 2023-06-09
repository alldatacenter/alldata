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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.impl.scan.file.FileMetadataColumn;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager.ImplicitColumnOptions;
import org.apache.drill.exec.physical.impl.scan.file.PartitionColumn;
import org.apache.drill.exec.physical.impl.scan.project.AbstractUnresolvedColumn.UnresolvedColumn;
import org.apache.drill.exec.physical.impl.scan.project.AbstractUnresolvedColumn.UnresolvedWildcardColumn;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.physical.impl.scan.project.ColumnProjection;
import org.apache.drill.exec.physical.impl.scan.project.ScanLevelProjection;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.SubOperatorTest;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestImplicitColumnParser extends SubOperatorTest {

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

  @Test
  public void testBasics() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnManager implictColManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    // Simulate SELECT a, b, c ...

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("a", "b", "c"),
        Lists.newArrayList(implictColManager.projectionParser()));

    // Verify

    assertFalse(scanProj.projectAll());
    assertFalse(implictColManager.hasImplicitCols());
  }

  /**
   * Test including file metadata (AKA "implicit columns") in the project
   * list.
   */
  @Test
  public void testFileMetadataColumnSelection() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnManager implictColManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    // Simulate SELECT a, fqn, filEPath, filename, suffix ...

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("a",
            ScanTestUtils.FULLY_QUALIFIED_NAME_COL,
            "filEPath", // Sic, to test case sensitivity
            ScanTestUtils.FILE_NAME_COL,
            ScanTestUtils.SUFFIX_COL),
        Lists.newArrayList(implictColManager.projectionParser()));

    assertFalse(scanProj.projectAll());
    assertEquals(5, scanProj.requestedCols().size());

    assertEquals(5, scanProj.columns().size());

    assertEquals("a", scanProj.columns().get(0).name());
    assertEquals(ScanTestUtils.FULLY_QUALIFIED_NAME_COL, scanProj.columns().get(1).name());
    assertEquals("filEPath", scanProj.columns().get(2).name());
    assertEquals(ScanTestUtils.FILE_NAME_COL, scanProj.columns().get(3).name());
    assertEquals(ScanTestUtils.SUFFIX_COL, scanProj.columns().get(4).name());

    // Verify column type

    assertTrue(scanProj.columns().get(0) instanceof UnresolvedColumn);
    assertTrue(scanProj.columns().get(1) instanceof FileMetadataColumn);
    assertTrue(scanProj.columns().get(2) instanceof FileMetadataColumn);
    assertTrue(scanProj.columns().get(3) instanceof FileMetadataColumn);
    assertTrue(scanProj.columns().get(4) instanceof FileMetadataColumn);

    assertTrue(implictColManager.hasImplicitCols());
  }

  /**
   * Verify that partition columns, in any case, work.
   */
  @Test
  public void testPartitionColumnSelection() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnManager implictColManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    String dir0 = ScanTestUtils.partitionColName(0);
    // Sic: case insensitivity, but name in project list
    // is preferred over "natural" name.
    String dir1 = "DIR1";
    String dir2 = ScanTestUtils.partitionColName(2);
    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(dir2, dir1, dir0, "a"),
        Lists.newArrayList(implictColManager.projectionParser()));

    assertEquals(4, scanProj.columns().size());
    assertEquals(dir2, scanProj.columns().get(0).name());
    assertEquals(dir1, scanProj.columns().get(1).name());
    assertEquals(dir0, scanProj.columns().get(2).name());
    assertEquals("a", scanProj.columns().get(3).name());

    // Verify column type

    assertTrue(scanProj.columns().get(0) instanceof PartitionColumn);
  }

  /**
   * Test wildcard expansion.
   */
  @Test
  public void testRevisedWildcard() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnManager implictColManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectAll(),
        Lists.newArrayList(implictColManager.projectionParser()));

    List<ColumnProjection> cols = scanProj.columns();
    assertEquals(1, cols.size());
    assertTrue(scanProj.columns().get(0) instanceof UnresolvedWildcardColumn);
  }

  /**
   * Combine wildcard and file metadata columns. The wildcard expands
   * table columns but not metadata columns.
   */
  @Test
  public void testLegacyWildcardAndFileMetadata() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnOptions options = standardOptions(filePath);
    options.useLegacyWildcardExpansion(true);
    ImplicitColumnManager implictColManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        options);

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(
            SchemaPath.DYNAMIC_STAR,
            ScanTestUtils.FILE_NAME_COL,
            ScanTestUtils.SUFFIX_COL),
        Lists.newArrayList(implictColManager.projectionParser()));

    List<ColumnProjection> cols = scanProj.columns();
    assertEquals(5, cols.size());
    assertTrue(scanProj.columns().get(0) instanceof UnresolvedWildcardColumn);
    assertTrue(scanProj.columns().get(1) instanceof FileMetadataColumn);
    assertTrue(scanProj.columns().get(2) instanceof FileMetadataColumn);
    assertTrue(scanProj.columns().get(3) instanceof PartitionColumn);
    assertTrue(scanProj.columns().get(4) instanceof PartitionColumn);
  }

  /**
   * As above, but include implicit columns before and after the
   * wildcard.
   */
  @Test
  public void testLegacyWildcardAndFileMetadataMixed() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnOptions options = standardOptions(filePath);
    options.useLegacyWildcardExpansion(true);
    ImplicitColumnManager implictColManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        options);

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(
            ScanTestUtils.FILE_NAME_COL,
            SchemaPath.DYNAMIC_STAR,
            ScanTestUtils.SUFFIX_COL),
        Lists.newArrayList(implictColManager.projectionParser()));

    List<ColumnProjection> cols = scanProj.columns();
    assertEquals(5, cols.size());
    assertTrue(scanProj.columns().get(0) instanceof FileMetadataColumn);
    assertTrue(scanProj.columns().get(1) instanceof UnresolvedWildcardColumn);
    assertTrue(scanProj.columns().get(2) instanceof FileMetadataColumn);
    assertTrue(scanProj.columns().get(3) instanceof PartitionColumn);
    assertTrue(scanProj.columns().get(4) instanceof PartitionColumn);
  }

  /**
   * Include both a wildcard and a partition column. The wildcard, in
   * legacy mode, will create partition columns for any partitions not
   * mentioned in the project list.
   * <p>
   * Tests proposed functionality: included only requested partition
   * columns.
   */
  @Test
  public void testRevisedWildcardAndPartition() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnManager implictColManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(SchemaPath.DYNAMIC_STAR,
            ScanTestUtils.partitionColName(8)),
        Lists.newArrayList(implictColManager.projectionParser()));

      List<ColumnProjection> cols = scanProj.columns();
      assertEquals(2, cols.size());
      assertTrue(scanProj.columns().get(0) instanceof UnresolvedWildcardColumn);
      assertTrue(scanProj.columns().get(1) instanceof PartitionColumn);
  }

  @Test
  public void testPreferredPartitionExpansion() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnOptions options = standardOptions(filePath);
    options.useLegacyWildcardExpansion(true);
    ImplicitColumnManager implictColManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        options);

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(SchemaPath.DYNAMIC_STAR,
            ScanTestUtils.partitionColName(8)),
        Lists.newArrayList(implictColManager.projectionParser()));

      List<ColumnProjection> cols = scanProj.columns();
      assertEquals(4, cols.size());
      assertTrue(scanProj.columns().get(0) instanceof UnresolvedWildcardColumn);
      assertTrue(scanProj.columns().get(1) instanceof PartitionColumn);
      assertEquals(8, ((PartitionColumn) cols.get(1)).partition());
      assertTrue(scanProj.columns().get(2) instanceof PartitionColumn);
      assertEquals(0, ((PartitionColumn) cols.get(2)).partition());
      assertTrue(scanProj.columns().get(3) instanceof PartitionColumn);
      assertEquals(1, ((PartitionColumn) cols.get(3)).partition());
  }

  @Test
  public void testPreferedWildcardExpansionWithOverlap() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnOptions options = standardOptions(filePath);
    options.useLegacyWildcardExpansion(true);
    ImplicitColumnManager implictColManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        options);

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(SchemaPath.DYNAMIC_STAR,
            ScanTestUtils.partitionColName(1)),
        Lists.newArrayList(implictColManager.projectionParser()));

      List<ColumnProjection> cols = scanProj.columns();
      assertEquals(3, cols.size());
      assertTrue(scanProj.columns().get(0) instanceof UnresolvedWildcardColumn);
      assertTrue(scanProj.columns().get(1) instanceof PartitionColumn);
      assertEquals(1, ((PartitionColumn) cols.get(1)).partition());
      assertTrue(scanProj.columns().get(2) instanceof PartitionColumn);
      assertEquals(0, ((PartitionColumn) cols.get(2)).partition());
  }

  /**
   * Verify that names that look like metadata columns, but appear
   * to be maps or arrays, are not interpreted as metadata. That is,
   * the projected table map or array "shadows" the metadata column.
   */
  @Test
  public void testShadowed() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnManager implictColManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(
            ScanTestUtils.FILE_NAME_COL + ".a",
            ScanTestUtils.FILE_PATH_COL + "[0]",
            ScanTestUtils.partitionColName(0) + ".b",
            ScanTestUtils.partitionColName(1) + "[0]",
            ScanTestUtils.SUFFIX_COL),
        Lists.newArrayList(implictColManager.projectionParser()));

    List<ColumnProjection> cols = scanProj.columns();
    assertEquals(5, cols.size());
    for (int i = 0; i < 4; i++) {
      assertTrue(scanProj.columns().get(i) instanceof UnresolvedColumn);
    }
    assertTrue(scanProj.columns().get(4) instanceof FileMetadataColumn);
  }
}
