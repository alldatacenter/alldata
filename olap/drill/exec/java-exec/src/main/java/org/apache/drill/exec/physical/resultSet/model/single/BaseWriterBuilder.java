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
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.model.MetadataProvider;
import org.apache.drill.exec.physical.resultSet.model.MetadataProvider.VectorDescrip;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.writer.AbstractObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.ColumnWriterFactory;
import org.apache.drill.exec.vector.accessor.writer.ListWriterImpl;
import org.apache.drill.exec.vector.accessor.writer.MapWriter;
import org.apache.drill.exec.vector.accessor.writer.ObjectDictWriter;
import org.apache.drill.exec.vector.accessor.writer.RepeatedListWriter;
import org.apache.drill.exec.vector.accessor.writer.UnionWriterImpl;
import org.apache.drill.exec.vector.accessor.writer.AbstractArrayWriter.ArrayObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.UnionWriterImpl.VariantObjectWriter;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.drill.exec.vector.complex.ListVector;
import org.apache.drill.exec.vector.complex.RepeatedDictVector;
import org.apache.drill.exec.vector.complex.RepeatedListVector;
import org.apache.drill.exec.vector.complex.UnionVector;

/**
 * Build a set of writers for a single (non-hyper) vector container.
 * This base class provides behavior common to the test-time RowSet
 * abstractions, and the production result set loader abstractions.
 * <p>
 * Writers are built recursively by walking the tree that defines a
 * row's structure. For a classic relational tuple, the tree has just
 * a root and a set of primitives. But, once we add array (repeated),
 * variant (LIST, UNION) and tuple (MAP) columns, the tree grows
 * quite complex.
 */
public abstract class BaseWriterBuilder {

  protected List<AbstractObjectWriter> buildContainerChildren(VectorContainer container,
      MetadataProvider mdProvider) {
    final List<AbstractObjectWriter> writers = new ArrayList<>();
    for (int i = 0; i < container.getNumberOfColumns(); i++) {
      final ValueVector vector = container.getValueVector(i).getValueVector();
      final VectorDescrip descrip = new VectorDescrip(mdProvider, i, vector.getField());
      writers.add(buildVectorWriter(vector, descrip));
    }
    return writers;
  }

  private AbstractObjectWriter buildVectorWriter(ValueVector vector, VectorDescrip descrip) {
    final MajorType type = vector.getField().getType();
    switch (type.getMinorType()) {
      case DICT:
        return buildDict(vector, descrip);
      case MAP:
        return MapWriter.buildMapWriter(descrip.metadata,
            (AbstractMapVector) vector,
            buildMap((AbstractMapVector) vector, descrip));

      case UNION:
        return buildUnion((UnionVector) vector, descrip);

      case LIST:
        return buildList(vector, descrip);

      default:
        return ColumnWriterFactory.buildColumnWriter(descrip.metadata, vector);
    }
  }

  private AbstractObjectWriter buildDict(ValueVector vector, VectorDescrip descrip) {
    if (vector.getField().getType().getMode() == DataMode.REPEATED) {
      ValueVector dataVector = ((RepeatedDictVector) vector).getDataVector();
      List<AbstractObjectWriter> writers = buildMap((AbstractMapVector) dataVector, descrip);
      return ObjectDictWriter.buildDictArray(descrip.metadata, (RepeatedDictVector) vector, writers);
    } else {
      List<AbstractObjectWriter> writers = buildMap((AbstractMapVector) vector, descrip);
      return ObjectDictWriter.buildDict(descrip.metadata, (DictVector) vector, writers);
    }
  }

  private List<AbstractObjectWriter> buildMap(AbstractMapVector vector, VectorDescrip descrip) {
    final List<AbstractObjectWriter> writers = new ArrayList<>();
    final MetadataProvider provider = descrip.parent.childProvider(descrip.metadata);
    int i = 0;
    for (final ValueVector child : vector) {
      final VectorDescrip childDescrip = new VectorDescrip(provider, i, child.getField());
      writers.add(buildVectorWriter(child, childDescrip));
      i++;
    }
    return writers;
  }

  private AbstractObjectWriter buildUnion(UnionVector vector, VectorDescrip descrip) {

    // Dummy writers are used when the schema is known up front, but the
    // query chooses not to project a column. Variants are used in the case when
    // the schema is not known, and we discover it on the fly. In this case,
    // (which currently occurs only in JSON) dummy vectors are not used.

    if (vector == null) {
      throw new UnsupportedOperationException("Dummy variant writer not yet supported");
    }
    final AbstractObjectWriter variants[] = new AbstractObjectWriter[MinorType.values().length];
    final MetadataProvider mdProvider = descrip.childProvider();
    int i = 0;
    for (final MinorType type : vector.getField().getType().getSubTypeList()) {

      // This call will create the vector if it does not yet exist.
      // Will throw an exception for unsupported types.
      // so call this only if the MajorType reports that the type
      // already exists.

      final ValueVector memberVector = vector.getMember(type);
      final VectorDescrip memberDescrip = new VectorDescrip(mdProvider, i++, memberVector.getField());
      variants[type.ordinal()] = buildVectorWriter(memberVector, memberDescrip);
    }
    return new VariantObjectWriter(
        new UnionWriterImpl(descrip.metadata, vector, variants));
  }

  private AbstractObjectWriter buildList(ValueVector vector,
      VectorDescrip descrip) {
    if (vector == null) {
      throw new UnsupportedOperationException("Dummy list writer not yet supported");
    }
    if (vector.getField().getType().getMode() == DataMode.REPEATED) {
      return buildMultiDList((RepeatedListVector) vector, descrip);
    } else {
      return build1DList((ListVector) vector, descrip);
    }
  }

  private AbstractObjectWriter buildMultiDList(RepeatedListVector vector,
      VectorDescrip descrip) {

    final ValueVector child = vector.getDataVector();
    if (child == null) {
      throw new UnsupportedOperationException("No child vector for repeated list.");
    }
    final VectorDescrip childDescrip = new VectorDescrip(descrip.childProvider(), 0, child.getField());
    final AbstractObjectWriter childWriter = buildVectorWriter(child, childDescrip);
    return RepeatedListWriter.buildRepeatedList(descrip.metadata, vector, childWriter);
  }

  private AbstractObjectWriter build1DList(ListVector vector,
      VectorDescrip descrip) {
    final ValueVector dataVector = vector.getDataVector();
    VectorDescrip dataMetadata;
    if (dataVector.getField().getType().getMinorType() == MinorType.UNION) {

      // If the list holds a union, then the list and union are collapsed
      // together in the metadata layer.
      dataMetadata = descrip;
    } else {
      dataMetadata = new VectorDescrip(descrip.childProvider(), 0, dataVector.getField());
    }
    return new ArrayObjectWriter(
      new ListWriterImpl(descrip.metadata,
          vector,
          buildVectorWriter(dataVector, dataMetadata)));
  }
}
