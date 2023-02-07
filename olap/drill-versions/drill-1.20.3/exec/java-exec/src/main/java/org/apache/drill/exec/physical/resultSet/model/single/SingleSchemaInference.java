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
package org.apache.drill.exec.physical.resultSet.model.single;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.record.metadata.VariantSchema;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.drill.exec.vector.complex.ListVector;
import org.apache.drill.exec.vector.complex.RepeatedDictVector;
import org.apache.drill.exec.vector.complex.RepeatedListVector;
import org.apache.drill.exec.vector.complex.UnionVector;

/**
 * Produce a metadata schema from a vector container. Used when given a record
 * batch without metadata.
 * <p>
 * At runtime, a vector container is a holder for a batch of rows. Here we are
 * concerned with metadata where we see a vector container as implementing a
 * tree of column schemas, expressed as the {@link MaterializedField} associated
 * with a vector. The tree structure comes from the structured vectors such as
 * maps, unions, lists and so on.
 * <p>
 * This class is, essentially, a tree converter: it converts a tree of vectors
 * to a tree of metadata.
 * <p>
 * The schema is inferred recursively by walking the vector tree that defines a
 * batches's structure. For a classic relational tuple, the tree has just a
 * vector container and a set of primitive vectors. But, once we add array
 * (repeated), variant (LIST, UNION) and tuple (MAP) columns, the tree grows
 * quite complex.
 */
public class SingleSchemaInference {

  public TupleMetadata infer(VectorContainer container) {
    final List<ColumnMetadata> columns = new ArrayList<>();
    for (int i = 0; i < container.getNumberOfColumns(); i++) {
      columns.add(inferVector(container.getValueVector(i).getValueVector()));
    }
    return MetadataUtils.fromColumns(columns);
  }

  private ColumnMetadata inferVector(ValueVector vector) {
    final MaterializedField field = vector.getField();
    switch (field.getType().getMinorType()) {
    case DICT:
      return MetadataUtils.newDict(field, inferDictSchema(vector));
    case MAP:
      return MetadataUtils.newMap(field, inferMapSchema((AbstractMapVector) vector));
    case LIST:
      if (field.getDataMode() == DataMode.REPEATED) {
        return MetadataUtils.newRepeatedList(field.getName(),
            inferVector(((RepeatedListVector) vector).getDataVector()));
      } else {
        return MetadataUtils.newVariant(field, inferListSchema((ListVector) vector));
      }
    case UNION:
      return MetadataUtils.newVariant(field, inferUnionSchema((UnionVector) vector));
    default:
      return MetadataUtils.fromField(field);
    }
  }

  private TupleSchema inferDictSchema(ValueVector vector) {
    final List<ColumnMetadata> columns = new ArrayList<>();
    DictVector dictVector;
    if (vector.getField().getType().getMode() == DataMode.REPEATED) {
      dictVector = (DictVector) ((RepeatedDictVector) vector).getDataVector();
    } else {
      dictVector = (DictVector) vector;
    }
    for (int i = 0; i < dictVector.size(); i++) {
      columns.add(inferVector(dictVector.getChildByOrdinal(i)));
    }
    return MetadataUtils.fromColumns(columns);
  }

  private TupleSchema inferMapSchema(AbstractMapVector vector) {
    final List<ColumnMetadata> columns = new ArrayList<>();
    for (int i = 0; i < vector.size(); i++) {
      columns.add(inferVector(vector.getChildByOrdinal(i)));
    }
    return MetadataUtils.fromColumns(columns);
  }

  private VariantSchema inferListSchema(ListVector vector) {
    final ValueVector dataVector = vector.getDataVector();
    if (dataVector instanceof UnionVector) {
      return inferUnionSchema((UnionVector) dataVector);
    }
    final VariantSchema schema = new VariantSchema();
    if (! vector.isEmptyType()) {
      schema.addType(inferVector(dataVector));
    }
    return schema;
  }

  private VariantSchema inferUnionSchema(UnionVector vector) {
    final VariantSchema schema = new VariantSchema();
    for (final MinorType type : vector.getField().getType().getSubTypeList()) {
      schema.addType(inferVector(vector.getMember(type)));
    }
    return schema;
  }
}
