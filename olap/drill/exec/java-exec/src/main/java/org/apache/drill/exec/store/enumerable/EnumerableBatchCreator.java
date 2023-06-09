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
package org.apache.drill.exec.store.enumerable;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.physical.impl.scan.framework.BasicScanFactory;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class EnumerableBatchCreator implements BatchCreator<EnumerableSubScan> {

  @Override
  public CloseableRecordBatch getBatch(ExecutorFragmentContext context,
      EnumerableSubScan subScan, List<RecordBatch> children) throws ExecutionSetupException {
    Preconditions.checkArgument(children.isEmpty());

    try {
      ManagedScanFramework.ScanFrameworkBuilder builder = createBuilder(subScan);
      return builder.buildScanOperator(context, subScan);
    } catch (UserException e) {
      // Rethrow user exceptions directly
      throw e;
    } catch (Throwable e) {
      // Wrap all others
      throw new ExecutionSetupException(e);
    }
  }

  private ManagedScanFramework.ScanFrameworkBuilder createBuilder(EnumerableSubScan subScan) {
    ManagedScanFramework.ScanFrameworkBuilder builder = new ManagedScanFramework.ScanFrameworkBuilder();
    builder.projection(subScan.getColumns());
    builder.setUserName(subScan.getUserName());
    builder.providedSchema(subScan.getSchema());

    ManagedReader<SchemaNegotiator> reader = new EnumerableRecordReader(subScan.getColumns(),
        subScan.getFieldsMap(), subScan.getCode(), subScan.getSchemaPath(), subScan.getConverterFactoryProvider());
    ManagedScanFramework.ReaderFactory readerFactory = new BasicScanFactory(Collections.singletonList(reader).iterator());
    builder.setReaderFactory(readerFactory);
    builder.nullType(Types.optional(TypeProtos.MinorType.VARCHAR));
    return builder;
  }
}
