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

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.ExpandableHyperContainer;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.physical.rowSet.HyperRowSetImpl;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;

public class GenericSV4CopierTest extends AbstractGenericCopierTest {

  @Override
  public RowSet createSrcRowSet(BufferAllocator allocator) throws SchemaChangeException {
    final BatchSchema batchSchema = createTestSchema(BatchSchema.SelectionVectorMode.NONE);
    final DrillBuf drillBuf = allocator.buffer(4 * 3);
    final SelectionVector4 sv4 = new SelectionVector4(drillBuf, 3, Character.MAX_VALUE);

    final VectorContainer batch1 = new RowSetBuilder(allocator, batchSchema)
      .addRow(row1())
      .addRow(row4())
      .build()
      .container();

    final VectorContainer batch2 = new RowSetBuilder(allocator, batchSchema)
      .addRow(row2())
      .addRow(row5())
      .addRow(row3())
      .build()
      .container();

    final ExpandableHyperContainer hyperContainer = new ExpandableHyperContainer(batch1);
    hyperContainer.addBatch(batch2);
    hyperContainer.setRecordCount(5);

    sv4.set(0, 0, 0);
    sv4.set(1, 1, 0);
    sv4.set(2, 1, 2);
    sv4.setCount(3);

    return new HyperRowSetImpl(hyperContainer, sv4);
  }
}
