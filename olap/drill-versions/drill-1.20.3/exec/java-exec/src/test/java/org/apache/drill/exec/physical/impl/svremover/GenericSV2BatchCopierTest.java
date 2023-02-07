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
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;

/**
 * Verifies optimization in SV2 such that when total record to copy is same as number of records in the
 * underlying batch for SV2 then SV2 will do transfer rather than row by row copy
 */
public class GenericSV2BatchCopierTest extends AbstractGenericCopierTest {

  @Override
  public RowSet createSrcRowSet(BufferAllocator allocator) {
    return new RowSetBuilder(allocator, createTestSchema(BatchSchema.SelectionVectorMode.TWO_BYTE))
      .addSelection(true, row1())
      .addRow(row2())
      .addSelection(true, row3())
      .withSv2()
      .build();
  }
}
