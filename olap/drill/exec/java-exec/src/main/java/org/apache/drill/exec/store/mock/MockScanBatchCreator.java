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
package org.apache.drill.exec.store.mock;

import java.util.LinkedList;
import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.physical.impl.ScanBatch;
import org.apache.drill.exec.physical.impl.protocol.OperatorRecordBatch;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorExec;
import org.apache.drill.exec.physical.impl.scan.framework.BasicScanFactory;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework.ScanFrameworkBuilder;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.store.RecordReader;
import org.apache.drill.exec.store.mock.MockTableDef.MockScanEntry;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class MockScanBatchCreator implements BatchCreator<MockSubScanPOP> {

  @Override
  public CloseableRecordBatch getBatch(ExecutorFragmentContext context, MockSubScanPOP config, List<RecordBatch> children)
      throws ExecutionSetupException {
    Preconditions.checkArgument(children.isEmpty());
    final List<MockScanEntry> entries = config.getReadEntries();
    MockScanEntry first = entries.get(0);
    if (first.isExtended()) {
      // Extended mode: use the revised, size-aware scan operator

      return extendedMockScan(context, config, entries);
    } else {
      return legacyMockScan(context, config, entries);
    }
  }

  private CloseableRecordBatch extendedMockScan(FragmentContext context,
      MockSubScanPOP config, List<MockScanEntry> entries) {
    List<SchemaPath> projList = new LinkedList<>();
    projList.add(SchemaPath.STAR_COLUMN);

    // Create batch readers up front. Handy when we know there are
    // only one or two; else use an iterator and create them on the fly.

    final List<ManagedReader<SchemaNegotiator>> readers = new LinkedList<>();
    for (final MockTableDef.MockScanEntry e : entries) {
      readers.add(new ExtendedMockBatchReader(e));
    }

    // Limit the batch size to 10 MB, or whatever the operator definition
    // specified.

    int batchSizeBytes = 10 * 1024 * 1024;
    MockTableDef.MockScanEntry first = entries.get(0);
    if (first.getBatchSize() > 0) {
      batchSizeBytes = first.getBatchSize();
    }

    // Set the scan to allow the maximum row count, allowing
    // each reader to adjust the batch size smaller if desired.

    ScanFrameworkBuilder builder = new ScanFrameworkBuilder();
    builder.batchByteLimit(batchSizeBytes);
    builder.projection(projList);
    builder.setReaderFactory(new BasicScanFactory(readers.iterator()));
    ManagedScanFramework framework = new ManagedScanFramework(builder);

    return new OperatorRecordBatch(
         context, config,
         new ScanOperatorExec(framework, false), false);
  }

  private CloseableRecordBatch legacyMockScan(FragmentContext context,
      MockSubScanPOP config,
      List<MockScanEntry> entries) throws ExecutionSetupException {
    final List<RecordReader> readers = new LinkedList<>();
    for (final MockTableDef.MockScanEntry e : entries) {
      readers.add(new MockRecordReader(context, e));
    }
    return new ScanBatch(config, context, readers);
  }
}
