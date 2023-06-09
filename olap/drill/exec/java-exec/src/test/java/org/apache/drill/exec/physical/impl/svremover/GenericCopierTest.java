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
package org.apache.drill.exec.physical.impl.svremover;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.vector.SchemaChangeCallBack;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;

public class GenericCopierTest extends AbstractGenericCopierTest {
  @Override
  public RowSet createSrcRowSet(BufferAllocator allocator) {
    return new RowSetBuilder(allocator, createTestSchema(BatchSchema.SelectionVectorMode.NONE))
      .addRow(row1())
      .addRow(row2())
      .addRow(row3())
      .addRow(row4())
      .addRow(row5())
      .build();
  }

  @Override
  public Copier createCopier(RecordBatch incoming, VectorContainer outputContainer,
                             SchemaChangeCallBack callback) {
    return GenericCopierFactory.createAndSetupNonSVGenericCopier(incoming, outputContainer);
  }
}
