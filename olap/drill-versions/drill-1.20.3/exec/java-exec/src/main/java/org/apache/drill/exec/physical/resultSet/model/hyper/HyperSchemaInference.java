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
package org.apache.drill.exec.physical.resultSet.model.hyper;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.record.metadata.VariantMetadata;
import org.apache.drill.exec.vector.ValueVector;

/**
 * Infer the schema for a hyperbatch. Scans each vector of each batch
 * to ensure that the vectors are compatible. (They should be.)
 * <p>
 * This code should be extended to handle merges. For example, batch
 * 1 may have a union with type INT. Batch 2 might have a union with
 * VARCHAR. The combined schema should have (INT, VARCHAR). The same
 * is true with (non-repeated) lists. There may be other cases.
 */

public class HyperSchemaInference {

  public TupleMetadata infer(VectorContainer container) throws SchemaChangeException {
    TupleMetadata schema = new TupleSchema();
    for (int i = 0; i < container.getNumberOfColumns(); i++) {
      VectorWrapper<?> vw = container.getValueVector(i);
      schema.addColumn(buildColumn(vw));
    }
    return schema;
  }

  private ColumnMetadata buildColumn(VectorWrapper<?> vw) throws SchemaChangeException {
    ColumnMetadata commonSchema = null;
    for (ValueVector vector : vw.getValueVectors()) {
      ColumnMetadata mapSchema = MetadataUtils.fromField(vector.getField());
      if (commonSchema == null) {
        commonSchema = mapSchema;
      } else if (! commonSchema.isEquivalent(mapSchema)) {
        throw new SchemaChangeException("Maps are not consistent");
      }
    }

    // Special handling of lists and unions

    if (commonSchema.isVariant()) {
      VariantMetadata variantSchema = commonSchema.variantSchema();
      if (variantSchema.size() == 1) {
        variantSchema.becomeSimple();
      }
    }
    return commonSchema;
  }
}
