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
package org.apache.drill.exec.store.parquet;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.physical.impl.ScanBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.List;

public class ParquetScanBatchCreator extends AbstractParquetScanBatchCreator implements BatchCreator<ParquetRowGroupScan> {

  @Override
  public ScanBatch getBatch(ExecutorFragmentContext context, ParquetRowGroupScan rowGroupScan, List<RecordBatch> children) throws ExecutionSetupException {
    Preconditions.checkArgument(children.isEmpty());
    OperatorContext oContext = context.newOperatorContext(rowGroupScan);
    return getBatch(context, rowGroupScan, oContext);
  }

  @Override
  protected AbstractDrillFileSystemManager getDrillFileSystemCreator(OperatorContext operatorContext, OptionManager optionManager) {
    return new ParquetDrillFileSystemManager(operatorContext, optionManager.getOption(ExecConstants.PARQUET_PAGEREADER_ASYNC).bool_val);
  }


  /**
   * Creates file system only if it was not created before, otherwise returns already created instance.
   */
  private class ParquetDrillFileSystemManager extends AbstractDrillFileSystemManager {

    private final boolean useAsyncPageReader;
    private DrillFileSystem fs;

    ParquetDrillFileSystemManager(OperatorContext operatorContext, boolean useAsyncPageReader) {
      super(operatorContext);
      this.useAsyncPageReader = useAsyncPageReader;
    }

    @Override
    protected DrillFileSystem get(Configuration config, Path path) throws ExecutionSetupException {
      if (fs == null) {
        try {
          fs =  useAsyncPageReader ? operatorContext.newNonTrackingFileSystem(config) : operatorContext.newFileSystem(config);
        } catch (IOException e) {
          throw new ExecutionSetupException(String.format("Failed to create DrillFileSystem: %s", e.getMessage()), e);
        }
      }
      return fs;
    }
  }

}
