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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.impl.scan.columns.ColumnsArrayParser;
import org.apache.drill.exec.physical.impl.scan.columns.ColumnsScanFramework;
import org.apache.drill.exec.physical.impl.scan.columns.UnresolvedColumnsArrayColumn;
import org.apache.drill.exec.physical.impl.scan.file.FileMetadataColumn;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager.ImplicitColumnOptions;
import org.apache.drill.exec.physical.impl.scan.project.ScanLevelProjection;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.SubOperatorTest;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestColumnsArrayParser extends SubOperatorTest {

  /**
   * Test the special "columns" column that asks to return all columns
   * as an array. No need for early schema. This case is special: it actually
   * creates the one and only table column to match the desired output column.
   */
  @Test
  public void testColumnsArray() {
    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(ColumnsScanFramework.COLUMNS_COL),
        ScanTestUtils.parsers(new ColumnsArrayParser(true)));

    assertFalse(scanProj.projectAll());
    assertEquals(1, scanProj.requestedCols().size());

    assertEquals(1, scanProj.columns().size());
    assertEquals(ColumnsScanFramework.COLUMNS_COL, scanProj.columns().get(0).name());

    // Verify column type

    assertTrue(scanProj.columns().get(0) instanceof UnresolvedColumnsArrayColumn);
  }

  @Test
  public void testRequiredColumnsArray() {
    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(ColumnsScanFramework.COLUMNS_COL),
        ScanTestUtils.parsers(new ColumnsArrayParser(true)));

    assertFalse(scanProj.projectAll());
    assertEquals(1, scanProj.requestedCols().size());

    assertEquals(1, scanProj.columns().size());
    assertEquals(ColumnsScanFramework.COLUMNS_COL, scanProj.columns().get(0).name());

    // Verify column type

    assertTrue(scanProj.columns().get(0) instanceof UnresolvedColumnsArrayColumn);
  }

  @Test
  public void testRequiredWildcard() {
    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectAll(),
        ScanTestUtils.parsers(new ColumnsArrayParser(true)));

    assertTrue(scanProj.projectAll());
    assertEquals(1, scanProj.requestedCols().size());

    assertEquals(1, scanProj.columns().size());
    assertEquals(ColumnsScanFramework.COLUMNS_COL, scanProj.columns().get(0).name());

    // Verify column type

    assertTrue(scanProj.columns().get(0) instanceof UnresolvedColumnsArrayColumn);
  }

  @Test
  public void testColumnsArrayCaseInsensitive() {

    // Sic: case variation of standard name

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("Columns"),
        ScanTestUtils.parsers(new ColumnsArrayParser(true)));

    assertFalse(scanProj.projectAll());
    assertEquals(1, scanProj.requestedCols().size());

    assertEquals(1, scanProj.columns().size());
    assertEquals("Columns", scanProj.columns().get(0).name());

    // Verify column type

    assertTrue(scanProj.columns().get(0) instanceof UnresolvedColumnsArrayColumn);
  }

  @Test
  public void testColumnsElements() {

   ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(
            ColumnsScanFramework.COLUMNS_COL + "[3]",
            ColumnsScanFramework.COLUMNS_COL + "[1]"),
        ScanTestUtils.parsers(new ColumnsArrayParser(true)));

    assertFalse(scanProj.projectAll());
    assertEquals(2, scanProj.requestedCols().size());

    assertEquals(1, scanProj.columns().size());
    assertEquals(ColumnsScanFramework.COLUMNS_COL, scanProj.columns().get(0).name());

    // Verify column type

    assertTrue(scanProj.columns().get(0) instanceof UnresolvedColumnsArrayColumn);
    UnresolvedColumnsArrayColumn colsCol = (UnresolvedColumnsArrayColumn) scanProj.columns().get(0);
    boolean indexes[] = colsCol.selectedIndexes();
    assertNotNull(indexes);
    assertEquals(4, indexes.length);
    assertFalse(indexes[0]);
    assertTrue(indexes[1]);
    assertFalse(indexes[0]);
    assertTrue(indexes[1]);
  }

  /**
   * The `columns` column is special; can't include both `columns` and
   * a named column in the same project.
   * <p>
   * TODO: This should only be true for text readers, make this an option.
   */
  @Test
  public void testErrorColumnsArrayAndColumn() {
    try {
      ScanLevelProjection.build(
          RowSetTestUtils.projectList(ColumnsScanFramework.COLUMNS_COL, "a"),
          ScanTestUtils.parsers(new ColumnsArrayParser(true)));
      fail();
    } catch (UserException e) {
      // Expected
    }
  }

  /**
   * Exclude a column and `columns` (reversed order of previous test).
   */
  @Test
  public void testErrorColumnAndColumnsArray() {
    try {
      ScanLevelProjection.build(
          RowSetTestUtils.projectList("a", ColumnsScanFramework.COLUMNS_COL),
          ScanTestUtils.parsers(new ColumnsArrayParser(true)));
      fail();
    } catch (UserException e) {
      // Expected
    }
  }

  /**
   * Requesting `columns` twice: second is ignored.
   */
  @Test
  public void testTwoColumnsArray() {
    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(ColumnsScanFramework.COLUMNS_COL, ColumnsScanFramework.COLUMNS_COL),
        ScanTestUtils.parsers(new ColumnsArrayParser(false)));
    assertFalse(scanProj.projectAll());
    assertEquals(2, scanProj.requestedCols().size());
    assertEquals(1, scanProj.columns().size());
    assertEquals(ColumnsScanFramework.COLUMNS_COL, scanProj.columns().get(0).name());
  }

  @Test
  public void testErrorRequiredAndExtra() {
    try {
      ScanLevelProjection.build(
          RowSetTestUtils.projectList("a"),
          ScanTestUtils.parsers(new ColumnsArrayParser(true)));
      fail();
    } catch (UserException e) {
      // Expected
    }
  }

  @Test
  public void testColumnsIndexTooLarge() {
    try {
      ScanLevelProjection.build(
          RowSetTestUtils.projectCols(SchemaPath.parseFromString("columns[70000]")),
          ScanTestUtils.parsers(new ColumnsArrayParser(true)));
      fail();
    } catch (UserException e) {
      // Expected
    }
  }

  private ImplicitColumnOptions standardOptions(Path filePath) {
    ImplicitColumnOptions options = new ImplicitColumnOptions();
    options.useLegacyWildcardExpansion(false); // Don't expand partition columns for wildcard
    options.setSelectionRoot(new Path("hdfs:///w"));
    options.setFiles(Lists.newArrayList(filePath));
    return options;
  }

  /**
   * The `columns` column is special: can't be used with other column names.
   * Make sure that the rule <i>does not</i> apply to implicit columns.
   */
  @Test
  public void testMetadataColumnsWithColumnsArray() {
    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnManager metadataManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(ScanTestUtils.FILE_NAME_COL,
            ColumnsScanFramework.COLUMNS_COL,
            ScanTestUtils.SUFFIX_COL),
        ScanTestUtils.parsers(new ColumnsArrayParser(true),
            metadataManager.projectionParser()));

    assertFalse(scanProj.projectAll());

    assertEquals(3, scanProj.columns().size());

    assertEquals(ScanTestUtils.FILE_NAME_COL, scanProj.columns().get(0).name());
    assertEquals(ColumnsScanFramework.COLUMNS_COL, scanProj.columns().get(1).name());
    assertEquals(ScanTestUtils.SUFFIX_COL, scanProj.columns().get(2).name());

    // Verify column type

    assertTrue(scanProj.columns().get(0) instanceof FileMetadataColumn);
    assertTrue(scanProj.columns().get(1) instanceof UnresolvedColumnsArrayColumn);
    assertTrue(scanProj.columns().get(2) instanceof FileMetadataColumn);
  }

  /**
   * If a query is of the form:
   * <pre><code>
   * select * from dfs.`multilevel/csv` where columns[1] < 1000
   * </code><pre>
   * Then the projection list passed to the scan operator
   * includes both the wildcard and the `columns` array.
   * We can ignore one of them.
   */
  @Test
  public void testWildcardAndColumns() {
    ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList(
            SchemaPath.DYNAMIC_STAR,
            ColumnsScanFramework.COLUMNS_COL),
        ScanTestUtils.parsers(new ColumnsArrayParser(true)));

    assertTrue(scanProj.projectAll());
    assertEquals(2, scanProj.requestedCols().size());

    assertEquals(1, scanProj.columns().size());
    assertEquals(ColumnsScanFramework.COLUMNS_COL, scanProj.columns().get(0).name());

    // Verify column type

    assertTrue(scanProj.columns().get(0) instanceof UnresolvedColumnsArrayColumn);
  }

  @Test
  public void testColumnsAsMap() {
    try {
        ScanLevelProjection.build(
          RowSetTestUtils.projectList("columns.x"),
          ScanTestUtils.parsers(new ColumnsArrayParser(true)));
        fail();
    }
    catch (UserException e) {
      // Expected
    }
  }
}
