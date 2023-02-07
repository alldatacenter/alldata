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
package org.apache.drill.exec.physical.rowSet;

import org.apache.drill.exec.physical.resultSet.model.ReaderIndex;
import org.apache.drill.exec.record.selection.SelectionVector2;

/**
 * Reader index that points to each row indirectly through the
 * selection vector. The {@link #offset()} method points to the
 * actual data row, while the {@link #logicalIndex()} method gives
 * the position relative to the indirection vector. That is,
 * the position increases monotonically, but the index jumps
 * around as specified by the indirection vector.
 */

public class IndirectRowIndex extends ReaderIndex {

  private final SelectionVector2 sv2;

  public IndirectRowIndex(SelectionVector2 sv2) {
    super(sv2::getCount);
    this.sv2 = sv2;
  }

  @Override
  public int offset() { return sv2.getIndex(position); }

  @Override
  public int hyperVectorIndex() { return 0; }
}
