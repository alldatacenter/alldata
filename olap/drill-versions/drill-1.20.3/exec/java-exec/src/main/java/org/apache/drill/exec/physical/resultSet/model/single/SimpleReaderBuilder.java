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
import org.apache.drill.exec.physical.impl.protocol.BatchAccessor;
import org.apache.drill.exec.physical.resultSet.model.MetadataProvider;
import org.apache.drill.exec.physical.resultSet.model.MetadataProvider.MetadataCreator;
import org.apache.drill.exec.physical.resultSet.model.MetadataProvider.MetadataRetrieval;
import org.apache.drill.exec.physical.resultSet.model.MetadataProvider.VectorDescrip;
import org.apache.drill.exec.physical.resultSet.model.ReaderBuilder;
import org.apache.drill.exec.physical.resultSet.model.ReaderIndex;
import org.apache.drill.exec.physical.rowSet.IndirectRowIndex;
import org.apache.drill.exec.physical.rowSet.RowSetReaderImpl;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.reader.AbstractObjectReader;
import org.apache.drill.exec.vector.accessor.reader.AbstractScalarReader;
import org.apache.drill.exec.vector.accessor.reader.ArrayReaderImpl;
import org.apache.drill.exec.vector.accessor.reader.DictReaderImpl;
import org.apache.drill.exec.vector.accessor.reader.MapReader;
import org.apache.drill.exec.vector.accessor.reader.UnionReaderImpl;
import org.apache.drill.exec.vector.accessor.reader.VectorAccessor;
import org.apache.drill.exec.vector.accessor.reader.VectorAccessors.SingleVectorAccessor;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.drill.exec.vector.complex.ListVector;
import org.apache.drill.exec.vector.complex.RepeatedListVector;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;
import org.apache.drill.exec.vector.complex.UnionVector;

/**
 * Builds a set of readers for a single (non-hyper) batch. Single batches
 * are indexed directly or via a simple indirection vector.
 * <p>
 * Derived classes handle the details of the various kinds of readers.
 * Today there is a single subclass that builds (test-time)
 * {@link org.apache.drill.exec.physical.rowSet.RowSet} objects.
 * The idea, however, is that we may eventually
 * want to create a "result set reader" for use in internal operators,
 * in parallel to the "result set loader". The result set reader would
 * handle a stream of incoming batches. The extant RowSet class handles
 * just one batch (the batch that is returned from a test.)
 * <p>
 * Readers are built recursively by walking the tree that defines a
 * row's structure. For a classic relational tuple, the tree has just
 * a root and a set of primitives. But, once we add array (repeated),
 * variant (LIST, UNION) and tuple (MAP) columns, the tree grows
 * quite complex.
 */

public class SimpleReaderBuilder extends ReaderBuilder {

  private static final SimpleReaderBuilder INSTANCE = new SimpleReaderBuilder();

  private SimpleReaderBuilder() { }

  public static RowSetReaderImpl build(VectorContainer container,
      TupleMetadata schema, ReaderIndex rowIndex) {
    return new RowSetReaderImpl(schema, rowIndex,
        INSTANCE.buildContainerChildren(container,
           new MetadataRetrieval(schema)));
  }

  public static RowSetReaderImpl build(VectorContainer container, ReaderIndex rowIndex) {
    MetadataCreator mdCreator = new MetadataCreator();
    List<AbstractObjectReader> children = INSTANCE.buildContainerChildren(container,
        mdCreator);
    return new RowSetReaderImpl(mdCreator.tuple(), rowIndex, children);
  }

  public static RowSetReaderImpl build(BatchAccessor batch) {
    return SimpleReaderBuilder.build(batch.container(),
        readerIndex(batch));
  }

  public static ReaderIndex readerIndex(BatchAccessor batch) {
    switch (batch.schema().getSelectionVectorMode()) {
    case TWO_BYTE:
      return new IndirectRowIndex(batch.selectionVector2());
    case NONE:
      return new DirectRowIndex(batch.container());
    default:
      throw new UnsupportedOperationException("Cannot use this method for a hyper-batch");
    }
  }

  public List<AbstractObjectReader> buildContainerChildren(
      VectorContainer container, MetadataProvider mdProvider) {
    final List<AbstractObjectReader> readers = new ArrayList<>();
    for (int i = 0; i < container.getNumberOfColumns(); i++) {
      final ValueVector vector = container.getValueVector(i).getValueVector();
      final VectorDescrip descrip = new VectorDescrip(mdProvider, i, vector.getField());
      readers.add(buildVectorReader(vector, descrip));
    }
    return readers;
  }

  protected AbstractObjectReader buildVectorReader(ValueVector vector, VectorDescrip descrip) {
    final VectorAccessor va = new SingleVectorAccessor(vector);
    final MajorType type = va.type();

    switch(type.getMinorType()) {
      case DICT:
        return buildDict(vector, va, descrip);
      case MAP:
        return buildMap((AbstractMapVector) vector, va, type.getMode(), descrip);
      case UNION:
        return buildUnion((UnionVector) vector, va, descrip);
      case LIST:
        return buildList(vector, va, descrip);
      case LATE:

        // Occurs for a list with no type: a list of nulls.

        return AbstractScalarReader.nullReader(descrip.metadata);
      default:
        return buildScalarReader(va, descrip.metadata);
    }
  }

  private AbstractObjectReader buildDict(ValueVector vector, VectorAccessor va, VectorDescrip descrip) {

    boolean isArray = descrip.metadata.isArray();

    DictVector dictVector;
    VectorAccessor dictAccessor;
    if (isArray) {
      dictVector = (DictVector) ((RepeatedValueVector) vector).getDataVector();
      dictAccessor = new SingleVectorAccessor(dictVector);
    } else {
      dictVector = (DictVector) vector;
      dictAccessor = va;
    }

    List<AbstractObjectReader> readers = buildMapMembers(dictVector, descrip.childProvider());
    AbstractObjectReader reader = DictReaderImpl.build(descrip.metadata, dictAccessor, readers);

    if (!isArray) {
      return reader;
    }

    return ArrayReaderImpl.buildTuple(descrip.metadata, va, reader);
  }

  private AbstractObjectReader buildMap(AbstractMapVector vector, VectorAccessor va, DataMode mode, VectorDescrip descrip) {

    final boolean isArray = mode == DataMode.REPEATED;

    // Map type

    final AbstractObjectReader mapReader = MapReader.build(
        descrip.metadata,
        isArray ? null : va,
        buildMapMembers(vector,
            descrip.parent.childProvider(descrip.metadata)));

    // Single map

    if (! isArray) {
      return mapReader;
    }

    // Repeated map

    return ArrayReaderImpl.buildTuple(descrip.metadata, va, mapReader);
  }

  protected List<AbstractObjectReader> buildMapMembers(AbstractMapVector mapVector, MetadataProvider provider) {
    final List<AbstractObjectReader> readers = new ArrayList<>();
    int i = 0;
    for (final ValueVector vector : mapVector) {
      final VectorDescrip descrip = new VectorDescrip(provider, i, vector.getField());
      readers.add(buildVectorReader(vector, descrip));
      i++;
    }
    return readers;
  }

  private AbstractObjectReader buildUnion(UnionVector vector, VectorAccessor unionAccessor, VectorDescrip descrip) {
    final MetadataProvider provider = descrip.childProvider();
    final AbstractObjectReader[] variants = new AbstractObjectReader[MinorType.values().length];
    int i = 0;
    for (final MinorType type : vector.getField().getType().getSubTypeList()) {

      // This call will create the vector if it does not yet exist.
      // Will throw an exception for unsupported types.
      // so call this only if the MajorType reports that the type
      // already exists.

      final ValueVector memberVector = vector.getMember(type);
      final VectorDescrip memberDescrip = new VectorDescrip(provider, i++, memberVector.getField());
      variants[type.ordinal()] = buildVectorReader(memberVector, memberDescrip);
    }
    return UnionReaderImpl.build(
        descrip.metadata,
        unionAccessor,
        variants);
  }

  private AbstractObjectReader buildList(ValueVector vector, VectorAccessor listAccessor,
      VectorDescrip listDescrip) {
    if (vector.getField().getType().getMode() == DataMode.REPEATED) {
      return buildMultiDList((RepeatedListVector) vector, listAccessor, listDescrip);
    } else {
      return build1DList((ListVector) vector, listAccessor, listDescrip);
    }
  }

  private AbstractObjectReader buildMultiDList(RepeatedListVector vector,
      VectorAccessor listAccessor, VectorDescrip listDescrip) {

    final ValueVector child = vector.getDataVector();
    if (child == null) {
      throw new UnsupportedOperationException("No child vector for repeated list.");
    }
    final VectorDescrip childDescrip = new VectorDescrip(listDescrip.childProvider(), 0, child.getField());
    final AbstractObjectReader elementReader = buildVectorReader(child, childDescrip);
    return ArrayReaderImpl.buildRepeatedList(listDescrip.metadata, listAccessor, elementReader);
  }

  /**
   * Build a list vector.
   * <p>
   * The list vector is a complex, somewhat ad-hoc structure. It can
   * take the place of repeated vectors, with some extra features.
   * The four "modes" of list vector, and thus list reader, are:
   * <ul>
   * <li>Similar to a scalar array.</li>
   * <li>Similar to a map (tuple) array.</li>
   * <li>The only way to represent an array of unions.</li>
   * <li>The only way to represent an array of lists.</li>
   * </ul>
   * Lists add an extra feature compared to the "regular" scalar or
   * map arrays. Each array entry can be either null or empty (regular
   * arrays can only be empty.)
   * <p>
   * When working with unions, this introduces an ambiguity: both the
   * list and the union have a null flag. Here, we assume that the
   * list flag has precedence, and that if the list entry is not null
   * then the union must also be not null. (Experience will show whether
   * existing code does, in fact, follow that convention.)
   */

  private AbstractObjectReader build1DList(ListVector vector, VectorAccessor listAccessor,
      VectorDescrip listDescrip) {
    final ValueVector dataVector = vector.getDataVector();
    VectorDescrip dataMetadata;
    if (dataVector.getField().getType().getMinorType() == MinorType.UNION) {

      // At the metadata level, a list always holds a union. But, at the
      // implementation layer, a union of a single type is collapsed out
      // to leave just a list of that single type.

      dataMetadata = listDescrip;
    } else {
      dataMetadata = new VectorDescrip(listDescrip.childProvider(), 0, dataVector.getField());
    }
    return ArrayReaderImpl.buildList(listDescrip.metadata,
        listAccessor, buildVectorReader(dataVector, dataMetadata));
  }
}
