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
package org.apache.drill.exec.store.openTSDB;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.physical.impl.ScanBatch;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.store.RecordReader;

import java.util.LinkedList;
import java.util.List;

public class OpenTSDBBatchCreator implements BatchCreator<OpenTSDBSubScan> {

  @Override
  public CloseableRecordBatch getBatch(ExecutorFragmentContext context, OpenTSDBSubScan subScan,
                                       List<RecordBatch> children) throws ExecutionSetupException {
    List<RecordReader> readers = new LinkedList<>();
    List<SchemaPath> columns;

    for (OpenTSDBSubScan.OpenTSDBSubScanSpec scanSpec : subScan.getTabletScanSpecList()) {
      try {
        if ((columns = subScan.getColumns()) == null) {
          columns = GroupScan.ALL_COLUMNS;
        }
        readers.add(new OpenTSDBRecordReader(subScan.getStorageEngine().getClient(), scanSpec, columns));
      } catch (Exception e) {
        throw new ExecutionSetupException(e);
      }
    }
    return new ScanBatch(subScan, context, readers);
  }
}
