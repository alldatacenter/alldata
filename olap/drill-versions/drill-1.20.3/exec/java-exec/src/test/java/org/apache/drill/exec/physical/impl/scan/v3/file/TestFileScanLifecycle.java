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
package org.apache.drill.exec.physical.impl.scan.v3.file;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.impl.scan.RowBatchReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.file.BaseFileScanTest.DummyFileWork;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.BaseTestScanLifecycle;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.ScanLifecycle;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker.ProjectionType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(EvfTest.class)
public class TestFileScanLifecycle extends BaseTestScanLifecycle implements MockFileNames {

  /**
   * Sanity test that the file scan framework works the same as the base framework
   * when no implicit columns are present.
   */
  @Test
  public void testNoImplicit() {
    FileScanLifecycleBuilder builder = new FileScanLifecycleBuilder();
    builder.rootDir(MOCK_ROOT_PATH);
    builder.fileSplits(Collections.singletonList(new DummyFileWork(MOCK_FILE_PATH)));
    builder.readerFactory(new FileReaderFactory() {
      @Override
      public ManagedReader newReader(FileSchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());

    RowSetUtilities.verify(simpleExpected(0), fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  @Test
  public void testSingleCol() {
    FileScanLifecycleBuilder builder = new FileScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectList(FileScanUtils.FILE_NAME_COL, SchemaPath.DYNAMIC_STAR));
    builder.rootDir(MOCK_ROOT_PATH);
    builder.fileSplits(Collections.singletonList(new DummyFileWork(MOCK_FILE_PATH)));
    builder.readerFactory(new FileReaderFactory() {
      @Override
      public ManagedReader newReader(FileSchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add(FileScanUtils.FILE_NAME_COL, IMPLICIT_COL_TYPE)
        .addAll(SCHEMA)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(MOCK_FILE_NAME, 10, "fred")
        .addRow(MOCK_FILE_NAME, 20, "wilma")
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  @Test
  public void testWildcard() {
    FileScanLifecycleBuilder builder = new FileScanLifecycleBuilder();
    builder.rootDir(MOCK_ROOT_PATH);
    builder.maxPartitionDepth(3);
    builder.fileSplits(Collections.singletonList(new DummyFileWork(MOCK_FILE_PATH)));
    builder.useLegacyWildcardExpansion(true);
    builder.readerFactory(new FileReaderFactory() {
      @Override
      public ManagedReader newReader(FileSchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addAll(SCHEMA)
        .add(FileScanUtils.partitionColName(0), PARTITION_COL_TYPE)
        .add(FileScanUtils.partitionColName(1), PARTITION_COL_TYPE)
        .add(FileScanUtils.partitionColName(2), PARTITION_COL_TYPE)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, "fred", MOCK_DIR0, MOCK_DIR1, null)
        .addRow(20, "wilma", MOCK_DIR0, MOCK_DIR1, null)
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  @Test
  public void testAllColumns() {
    FileScanLifecycleBuilder builder = new FileScanLifecycleBuilder();
    builder.rootDir(MOCK_ROOT_PATH);
    builder.maxPartitionDepth(3);
    builder.projection(FileScanUtils.projectAllWithMetadata(3));
    builder.fileSplits(Collections.singletonList(new DummyFileWork(MOCK_FILE_PATH)));
    builder.useLegacyWildcardExpansion(true);
    builder.readerFactory(new FileReaderFactory() {
      @Override
      public ManagedReader newReader(FileSchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addAll(SCHEMA)
        .add(FileScanUtils.FULLY_QUALIFIED_NAME_COL, IMPLICIT_COL_TYPE)
        .add(FileScanUtils.FILE_PATH_COL, IMPLICIT_COL_TYPE)
        .add(FileScanUtils.FILE_NAME_COL, IMPLICIT_COL_TYPE)
        .add(FileScanUtils.SUFFIX_COL, IMPLICIT_COL_TYPE)
        .add(FileScanUtils.partitionColName(0), PARTITION_COL_TYPE)
        .add(FileScanUtils.partitionColName(1), PARTITION_COL_TYPE)
        .add(FileScanUtils.partitionColName(2), PARTITION_COL_TYPE)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, "fred", MOCK_FILE_FQN, MOCK_FILE_DIR_PATH, MOCK_FILE_NAME, MOCK_SUFFIX, MOCK_DIR0, MOCK_DIR1, null)
        .addRow(20, "wilma", MOCK_FILE_FQN, MOCK_FILE_DIR_PATH, MOCK_FILE_NAME, MOCK_SUFFIX, MOCK_DIR0, MOCK_DIR1, null)
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * Test the obscure case that the partition column contains two digits:
   * dir11. Also tests the obscure case that the output only has partition
   * columns.
   */
  @Test
  public void testPartitionColumnTwoDigits() {
    Path filePath = new Path("file:/w/0/1/2/3/4/5/6/7/8/9/10/d11/z.csv");
    FileScanLifecycleBuilder builder = new FileScanLifecycleBuilder();
    builder.rootDir(MOCK_ROOT_PATH);
    builder.maxPartitionDepth(11);
    builder.projection(RowSetTestUtils.projectList(
        "a", "b", FileScanUtils.partitionColName(11)));
    builder.fileSplits(Collections.singletonList(new DummyFileWork(filePath)));
    builder.useLegacyWildcardExpansion(true);
    builder.readerFactory(new FileReaderFactory() {
      @Override
      public ManagedReader newReader(FileSchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addAll(SCHEMA)
        .add(FileScanUtils.partitionColName(11), PARTITION_COL_TYPE)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, "fred", "d11")
        .addRow(20, "wilma", "d11")
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * Verify that errors thrown from file-based readers include the file name
   * in addition to the scan and reader level error contexts.
   */
  @Test
  public void testCtorUserError() {
    FileScanLifecycleBuilder builder = new FileScanLifecycleBuilder();
    builder.errorContext(b -> b.addContext("Scan context"));
    builder.rootDir(MOCK_ROOT_PATH);
    builder.maxPartitionDepth(3);
    builder.projection(FileScanUtils.projectAllWithMetadata(3));
    builder.fileSplits(Collections.singletonList(new DummyFileWork(MOCK_FILE_PATH)));
    builder.useLegacyWildcardExpansion(true);
    builder.readerFactory(new FileReaderFactory() {
      @Override
      public ManagedReader newReader(FileSchemaNegotiator negotiator) {
        return new FailingReader(negotiator, "ctor-u");
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    try {
      reader.open();
      fail();
    } catch (UserException e) {
      String msg = e.getMessage();
      assertTrue(msg.contains("Oops ctor"));
      assertTrue(msg.contains("My custom context"));
      assertTrue(msg.contains("Scan context"));
      assertTrue(msg.contains(MOCK_FILE_NAME));
      assertNull(e.getCause());
    }
    scan.close();
  }
}
