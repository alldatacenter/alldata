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
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.vector.accessor.impl.AccessorUtilities;

/**
 * Read-only row index into the hyper row set with batch and index
 * values mapping via an SV4.
 */

public class HyperRowIndex extends ReaderIndex {

  private final SelectionVector4 sv4;

  public HyperRowIndex(SelectionVector4 sv4) {
    super(sv4::getCount);
    this.sv4 = sv4;
  }

  @Override
  public int offset() {
    return AccessorUtilities.sv4Index(sv4.get(position));
  }

  @Override
  public int hyperVectorIndex( ) {
    return AccessorUtilities.sv4Batch(sv4.get(position));
  }
}
