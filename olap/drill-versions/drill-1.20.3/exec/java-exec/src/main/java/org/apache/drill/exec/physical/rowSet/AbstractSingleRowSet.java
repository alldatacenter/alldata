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
import org.apache.drill.exec.physical.resultSet.model.single.SimpleReaderBuilder;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Base class for row sets backed by a single record batch.
 */

public abstract class AbstractSingleRowSet extends AbstractRowSet implements SingleRowSet {

  protected AbstractSingleRowSet(AbstractSingleRowSet rowSet) {
    super(rowSet.container, rowSet.schema);
  }

  protected AbstractSingleRowSet(VectorContainer container, TupleMetadata schema) {
    super(container, schema);
  }

  @Override
  public long size() {
    RecordBatchSizer sizer = new RecordBatchSizer(container());
    return sizer.getActualSize();
  }

  /**
   * Internal method to build the set of column readers needed for
   * this row set. Used when building a row set reader.
   *
   * @param rowIndex object that points to the current row
   * @return an array of column readers: in the same order as the
   * (non-map) vectors.
   */
  protected RowSetReader buildReader(ReaderIndex rowIndex) {
    return SimpleReaderBuilder.build(container(), schema, rowIndex);
  }
}
