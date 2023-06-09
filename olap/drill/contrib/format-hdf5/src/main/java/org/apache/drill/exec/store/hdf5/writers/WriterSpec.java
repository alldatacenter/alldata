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
package org.apache.drill.exec.store.hdf5.writers;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.convert.StandardConversions;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;

/**
 * Encapsulates the information needed to handle implicit type conversions
 * for scalar fields. The provided schema gives the type of vector to
 * create, the {@link #makeWriter(String, MinorType, DataMode)} method
 * gives the type of data to be written. A standard conversion is inserted
 * where needed.
 */
public class WriterSpec {
  public final TupleWriter tupleWriter;
  public final TupleMetadata providedSchema;
  public final CustomErrorContext errorContext;
  public final StandardConversions conversions;

  public WriterSpec(TupleWriter tupleWriter, TupleMetadata providedSchema, CustomErrorContext errorContext) {
    this.tupleWriter = tupleWriter;
    this.providedSchema = providedSchema;
    this.errorContext = errorContext;
    this.conversions = StandardConversions.builder()
        .withSchema(providedSchema)
        .build();
  }

  public ValueWriter makeWriter(String name, MinorType type, DataMode mode) {
    ColumnMetadata providedCol = providedSchema == null ? null :
        providedSchema.metadata(name);
    if (providedCol == null) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, type, mode);
      int index = tupleWriter.addColumn(colSchema);
      return tupleWriter.scalar(index);
    } else {
      int index = tupleWriter.addColumn(providedCol);
      return conversions.converterFor(tupleWriter.scalar(index), type);
    }
  }
}
