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
package org.apache.drill.exec.physical.resultSet.model;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.physical.impl.protocol.BatchAccessor;
import org.apache.drill.exec.physical.resultSet.model.hyper.HyperReaderBuilder;
import org.apache.drill.exec.physical.resultSet.model.single.SimpleReaderBuilder;
import org.apache.drill.exec.physical.rowSet.RowSetReaderImpl;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.reader.AbstractObjectReader;
import org.apache.drill.exec.vector.accessor.reader.ArrayReaderImpl;
import org.apache.drill.exec.vector.accessor.reader.BaseScalarReader;
import org.apache.drill.exec.vector.accessor.reader.ColumnReaderFactory;
import org.apache.drill.exec.vector.accessor.reader.VectorAccessor;

public abstract class ReaderBuilder {

  public static RowSetReaderImpl buildReader(BatchAccessor batch) {
    if (batch.schema().getSelectionVectorMode() == SelectionVectorMode.FOUR_BYTE) {
      try {
        return HyperReaderBuilder.build(batch);
      } catch (SchemaChangeException e) {

        // The caller is responsible for ensuring that the hyper-batch
        // has a consistent schema. If it is possible that the schema is
        // inconsistent, then call the build() method directory and
        // "do the right thing", which is pretty much to fail, as a
        // hyper-batch is a very awkward place to discover an inconsistent
        // schema.

        throw new IllegalStateException("Hyper-batch contains an inconsistent schema", e);
      }
    } else {
      return SimpleReaderBuilder.build(batch);
    }
  }

  protected AbstractObjectReader buildScalarReader(VectorAccessor va, ColumnMetadata schema) {
    BaseScalarReader scalarReader = ColumnReaderFactory.buildColumnReader(va);
    DataMode mode = va.type().getMode();
    switch (mode) {
    case OPTIONAL:
      return BaseScalarReader.buildOptional(schema, va, scalarReader);
    case REQUIRED:
      return BaseScalarReader.buildRequired(schema, va, scalarReader);
    case REPEATED:
      return ArrayReaderImpl.buildScalar(schema, va, scalarReader);
    default:
      throw new UnsupportedOperationException(mode.toString());
    }
  }
}
