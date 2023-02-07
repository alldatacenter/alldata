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
package org.apache.drill.exec.store.easy.text.reader;

import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin;
import org.apache.drill.exec.vector.accessor.ValueWriter;

/**
 * Class is responsible for generating record batches for text file inputs. We
 * generate a record batch with a set of varchar vectors. A varchar vector
 * contains all the field values for a given column. Each record is a single
 * value within each vector of the set.
 */
class FieldVarCharOutput extends BaseFieldOutput {

  protected final ValueWriter[] colWriters;

  /**
   * We initialize and add the varchar vector for each incoming field in this
   * constructor.
   *
   * @param writer row set writer
   */
  FieldVarCharOutput(RowSetLoader writer, ValueWriter[] colWriters) {
    super(writer,
        TextFormatPlugin.MAXIMUM_NUMBER_COLUMNS,
        makeMask(writer));
    this.colWriters = colWriters;
  }

  private static boolean[] makeMask(RowSetLoader writer) {
    final TupleMetadata schema = writer.tupleSchema();
    final boolean[] projectionMask = new boolean[schema.size()];
    for (int i = 0; i < schema.size(); i++) {
      projectionMask[i] = writer.column(i).isProjected();
    }
    return projectionMask;
  }

  @Override
  public boolean endField() {
    writeToVector();
    return super.endField();
  }

  @Override
  protected ValueWriter columnWriter() {
    return colWriters[currentFieldIndex];
  }
}
