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
package org.apache.drill.exec.store.phoenix;

import java.util.List;

import org.apache.drill.common.exceptions.ChildErrorContext;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework.ReaderFactory;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework.ScanFrameworkBuilder;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.server.options.OptionManager;

public class PhoenixScanBatchCreator implements BatchCreator<PhoenixSubScan> {

  @Override
  public CloseableRecordBatch getBatch(ExecutorFragmentContext context, PhoenixSubScan subScan, List<RecordBatch> children) throws ExecutionSetupException {
    try {
      ScanFrameworkBuilder builder = createBuilder(context.getOptions(), subScan);
      return builder.buildScanOperator(context, subScan);
    } catch (UserException e) {
      throw e;
    } catch (Throwable e) {
      throw new ExecutionSetupException(e);
    }
  }

  private ScanFrameworkBuilder createBuilder(OptionManager options, PhoenixSubScan subScan) {
    ScanFrameworkBuilder builder = new ScanFrameworkBuilder();
    builder.projection(subScan.getColumns());
    builder.setUserName(subScan.getUserName());
    // Phoenix reader
    ReaderFactory readerFactory = new PhoenixReaderFactory(subScan);
    builder.setReaderFactory(readerFactory);
    builder.nullType(Types.optional(MinorType.VARCHAR));
    // Add custom error context
    builder.errorContext(new ChildErrorContext(builder.errorContext()) {
      @Override
      public void addContext(UserException.Builder builder) {
        builder.addContext("sql : ", subScan.getScanSpec().getSql());
        builder.addContext("columns : ", subScan.getScanSpec().getColumns().toString());
        builder.addContext("estimate row count : ", subScan.getScanSpec().getEstimateRows());
      }
    });

    return builder;
  }

  private static class PhoenixReaderFactory implements ReaderFactory {

    private final PhoenixSubScan subScan;
    private int count;

    public PhoenixReaderFactory(PhoenixSubScan subScan) {
      this.subScan = subScan;
    }

    @Override
    public void bind(ManagedScanFramework framework) {  }

    @Override
    public ManagedReader<? extends SchemaNegotiator> next() {
      // Only a single scan
      if (count++ == 0) {
        return new PhoenixBatchReader(subScan);
      }
      return null;
    }
  }
}
