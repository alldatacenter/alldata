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

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.physical.impl.protocol.BatchAccessor;
import org.apache.drill.exec.physical.resultSet.model.ReaderBuilder;
import org.apache.drill.exec.physical.rowSet.HyperRowIndex;
import org.apache.drill.exec.physical.rowSet.RowSetReaderImpl;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.VariantMetadata;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ColumnReaderIndex;
import org.apache.drill.exec.vector.accessor.reader.AbstractObjectReader;
import org.apache.drill.exec.vector.accessor.reader.ArrayReaderImpl;
import org.apache.drill.exec.vector.accessor.reader.DictReaderImpl;
import org.apache.drill.exec.vector.accessor.reader.MapReader;
import org.apache.drill.exec.vector.accessor.reader.UnionReaderImpl;
import org.apache.drill.exec.vector.accessor.reader.VectorAccessor;
import org.apache.drill.exec.vector.accessor.reader.VectorAccessors;
import org.apache.drill.exec.vector.accessor.reader.VectorAccessors.BaseHyperVectorAccessor;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;

/**
 * Base reader builder for a hyper-batch. The semantics of hyper-batches are
 * a bit rough. When a single batch, we can walk the vector tree to get the
 * information we need. But, hyper vector wrappers don't provide that same
 * information, so we can't just walk them. Further, the code that builds
 * hyper-batches appears perfectly happy to accept batches with differing
 * schemas, something that will cause the readers to blow up because they
 * must commit to a particular kind of reader for each vector.
 * <p>
 * The solution is to build the readers in two passes. The first builds a
 * metadata model for each batch and merges those models. (This version
 * requires strict identity in schemas; a fancier solution could handle,
 * say, the addition of map members in one batch vs. another or the addition
 * of union/list members across batches.)
 * <p>
 * The metadata (by design) has the information we need, so in the second pass
 * we walk the metadata hierarchy and build up readers from that, creating
 * vector accessors as we go to provide a runtime path from the root vectors
 * (selected by the SV4) to the inner vectors (which are not represented as
 * hypervectors.)
 * <p>
 * The hypervector wrapper mechanism provides a crude way to handle inner
 * vectors, but it is awkward, and does not lend itself to the kind of caching
 * we'd like for performance, so we use our own accessors for inner vectors.
 * The outermost hyper vector accessors wrap a hyper vector wrapper. Inner
 * accessors directly navigate at the vector level (from a vector provided by
 * the outer vector accessor.)
 */

public class HyperReaderBuilder extends ReaderBuilder {

  private static final HyperReaderBuilder INSTANCE = new HyperReaderBuilder();

  private HyperReaderBuilder() { }

  public static RowSetReaderImpl build(VectorContainer container, TupleMetadata schema, SelectionVector4 sv4) {
    HyperRowIndex rowIndex = new HyperRowIndex(sv4);
    return new RowSetReaderImpl(schema, rowIndex,
        INSTANCE.buildContainerChildren(container, schema));
  }

  /**
   * Build a hyper-batch reader given a batch accessor.
   *
   * @param batch wrapper which provides the container and SV4
   * @return a row set reader for the hyper-batch
   * @throws SchemaChangeException if the individual batches have
   * inconsistent schemas (say, a column in batch 1 is an INT, but in
   * batch 2 it is a VARCHAR)
   */

  public static RowSetReaderImpl build(BatchAccessor batch) throws SchemaChangeException {
    VectorContainer container = batch.container();
    return build(container,
        new HyperSchemaInference().infer(container),
        batch.selectionVector4());
  }

  /**
   * Vector accessor used by the column accessors to obtain the vector for
   * each column value. That is, position 0 might be batch 4, index 3,
   * while position 1 might be batch 1, index 7, and so on.
   * <p>
   * Must be here: the reader layer is in the <tt>vector</tt> package
   * and does not have visibility to <tt>java-exec</tt> classes.
   */

  public static class HyperVectorAccessor extends BaseHyperVectorAccessor {

    private final ValueVector[] vectors;
    private ColumnReaderIndex rowIndex;

    public HyperVectorAccessor(VectorWrapper<?> vw) {
      super(vw.getField().getType());
      vectors = vw.getValueVectors();
    }

    @Override
    public void bind(ColumnReaderIndex index) {
      rowIndex = index;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueVector> T vector() {
      return (T) vectors[rowIndex.hyperVectorIndex()];
    }
  }

  protected List<AbstractObjectReader> buildContainerChildren(
      VectorContainer container) throws SchemaChangeException {
    TupleMetadata schema = new HyperSchemaInference().infer(container);
    return buildContainerChildren(container, schema);
  }

  protected List<AbstractObjectReader> buildContainerChildren(
      VectorContainer container, TupleMetadata schema) {
    List<AbstractObjectReader> readers = new ArrayList<>();
    for (int i = 0; i < container.getNumberOfColumns(); i++) {
      VectorWrapper<?> vw = container.getValueVector(i);
      VectorAccessor va = new HyperVectorAccessor(vw);
      readers.add(buildVectorReader(va, schema.metadata(i)));
    }
    return readers;
  }

  protected AbstractObjectReader buildVectorReader(VectorAccessor va, ColumnMetadata metadata) {
    switch(metadata.type()) {
    case DICT:
      return buildDict(va, metadata);
    case MAP:
      return buildMap(va, metadata.mode(), metadata);
    case UNION:
      return buildUnion(va, metadata);
    case LIST:
      return buildList(va, metadata);
    default:
      return buildScalarReader(va, metadata);
    }
  }

  private AbstractObjectReader buildDict(VectorAccessor va, ColumnMetadata metadata) {
    boolean isArray = metadata.isArray();

    ValueVector vector = va.vector();
    VectorAccessor dictAccessor;
    if (isArray) {
      ValueVector dictVector = ((RepeatedValueVector) vector).getDataVector();
      dictAccessor = new VectorAccessors.SingleVectorAccessor(dictVector);
    } else {
      dictAccessor = va;
    }

    List<AbstractObjectReader> readers = buildMapMembers(dictAccessor, metadata.tupleSchema());
    AbstractObjectReader reader = DictReaderImpl.build(metadata, dictAccessor, readers);

    if (!isArray) {
      return reader;
    }

    return ArrayReaderImpl.buildTuple(metadata, va, reader);
  }

  private AbstractObjectReader buildMap(VectorAccessor va, DataMode mode, ColumnMetadata metadata) {

    boolean isArray = mode == DataMode.REPEATED;

    // Map type

    AbstractObjectReader mapReader = MapReader.build(
        metadata,
        isArray ? null : va,
        buildMapMembers(va, metadata.tupleSchema()));

    // Single map

    if (! isArray) {
      return mapReader;
    }

    // Repeated map

    return ArrayReaderImpl.buildTuple(metadata, va, mapReader);
  }

  protected List<AbstractObjectReader> buildMapMembers(VectorAccessor va, TupleMetadata mapSchema) {
    List<AbstractObjectReader> readers = new ArrayList<>();
    for (int i = 0; i < mapSchema.size(); i++) {
      ColumnMetadata member = mapSchema.metadata(i);
      // Does not use the hyper-vector mechanism.

      readers.add(buildVectorReader(
          new VectorAccessors.MapMemberHyperVectorAccessor(va, i, member.majorType()),
          member));
    }
    return readers;
  }

  private AbstractObjectReader buildUnion(VectorAccessor unionAccessor, ColumnMetadata metadata) {
    VariantMetadata unionSchema = metadata.variantSchema();
    final AbstractObjectReader variants[] = new AbstractObjectReader[MinorType.values().length];
    for (ColumnMetadata member : unionSchema.members()) {

      // The following builds a synthetic field since we have no good way to
      // access the real field at this point.

      variants[member.type().ordinal()] = buildVectorReader(
          new VectorAccessors.UnionMemberHyperVectorAccessor(unionAccessor, member.majorType()),
          member);
    }
    return UnionReaderImpl.build(
        metadata,
        unionAccessor,
        variants);
  }

  // Note: Does not yet handle 2D lists. See the "single" base reader builder
  // for the needed code.

  private AbstractObjectReader buildList(VectorAccessor listAccessor,
      ColumnMetadata metadata) {
    VariantMetadata listSchema = metadata.variantSchema();
    ColumnMetadata dataMetadata = listSchema.listSubtype();
    return ArrayReaderImpl.buildList(metadata,
        listAccessor,
        buildVectorReader(
            new VectorAccessors.ListMemberHyperVectorAccessor(
                listAccessor, dataMetadata.majorType()),
            dataMetadata));
  }
}
