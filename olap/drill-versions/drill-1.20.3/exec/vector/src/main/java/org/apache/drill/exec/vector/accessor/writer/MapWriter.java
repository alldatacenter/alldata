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
package org.apache.drill.exec.vector.accessor.writer;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.writer.AbstractArrayWriter.ArrayObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.dummy.DummyArrayWriter;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.exec.vector.complex.RepeatedMapVector;

/**
 * Writer for a Drill Map type. Maps are actually tuples, just like rows.
 */

public abstract class MapWriter extends AbstractTupleWriter {

  /**
   * Writer for a single (non-array) map. Clients don't really "write" maps;
   * rather, this writer is a holder for the columns within the map, and those
   * columns are what is written.
   */

  protected static class SingleMapWriter extends MapWriter {
    private final MapVector mapVector;

    protected SingleMapWriter(ColumnMetadata schema, MapVector vector, List<AbstractObjectWriter> writers) {
      super(schema, writers);
      mapVector = vector;
    }

    @Override
    public void endWrite() {
      super.endWrite();

      // A non repeated map has a field that holds the value count.
      // Update it. (A repeated map uses the offset vector's value count.)
      // Special form of set value count: used only for
      // this class to avoid setting the value count of children.
      // Setting these counts was already done. Doing it again
      // will corrupt nullable vectors because the writers don't
      // set the "lastSet" field of nullable vector accessors,
      // and the initial value of -1 will cause all values to
      // be overwritten.

      mapVector.setMapValueCount(vectorIndex.vectorIndex());
    }

    @Override
    public void preRollover() {
      super.preRollover();
      mapVector.setMapValueCount(vectorIndex.rowStartIndex());
    }

    @Override
    public boolean isProjected() { return true; }
  }

  /**
   * Writer for a an array of maps. A single array index coordinates writes
   * to the constituent member vectors so that, say, the values for (row 10,
   * element 5) all occur to the same position in the columns within the map.
   * Since the map is an array, it has an associated offset vector, which the
   * parent array writer is responsible for maintaining.
   */

  protected static class ArrayMapWriter extends MapWriter {

    protected ArrayMapWriter(ColumnMetadata schema, List<AbstractObjectWriter> writers) {
      super(schema, writers);
    }

    @Override
    public void bindIndex(ColumnWriterIndex index) {

      // This is a repeated map, so the provided index is an array element
      // index. Convert this to an index that will not increment the element
      // index on each write so that a map with three members, say, won't
      // increment the index for each member. Rather, the index must be
      // incremented at the array level.

      bindIndex(index, new MemberWriterIndex(index));
    }

    @Override
    public boolean isProjected() { return true; }
  }

  protected static class DummyMapWriter extends MapWriter {

    protected DummyMapWriter(ColumnMetadata schema,
        List<AbstractObjectWriter> writers) {
      super(schema, writers);
    }

    @Override
    public boolean isProjected() { return false; }

    @Override
    public void copy(ColumnReader from) { }
  }

  protected static class DummyArrayMapWriter extends MapWriter {

    protected DummyArrayMapWriter(ColumnMetadata schema,
        List<AbstractObjectWriter> writers) {
      super(schema, writers);
    }

    @Override
    public boolean isProjected() { return false; }

    @Override
    public void copy(ColumnReader from) { }
  }

  protected final ColumnMetadata mapColumnSchema;

  protected MapWriter(ColumnMetadata schema, List<AbstractObjectWriter> writers) {
    super(schema.tupleSchema(), writers);
    mapColumnSchema = schema;
  }

  public static TupleObjectWriter buildMap(ColumnMetadata schema, MapVector vector,
      List<AbstractObjectWriter> writers) {
    MapWriter mapWriter;
    if (vector != null) {

      // Vector is not required for a map writer; the map's columns
      // are written, but not the (non-array) map.

      mapWriter = new SingleMapWriter(schema, vector, writers);
    } else {
      mapWriter = new DummyMapWriter(schema, writers);
    }
    return new TupleObjectWriter(mapWriter);
  }

  public static ArrayObjectWriter buildMapArray(ColumnMetadata schema,
      RepeatedMapVector mapVector,
      List<AbstractObjectWriter> writers) {
    MapWriter mapWriter;
    if (mapVector != null) {
      mapWriter = new ArrayMapWriter(schema, writers);
    } else {
      mapWriter = new DummyArrayMapWriter(schema, writers);
    }
    TupleObjectWriter mapArray = new TupleObjectWriter(mapWriter);
    AbstractArrayWriter arrayWriter;
    if (mapVector != null) {
      arrayWriter = new ObjectArrayWriter(schema,
          mapVector.getOffsetVector(),
          mapArray);
    } else  {
      arrayWriter = new DummyArrayWriter(schema, mapArray);
    }
    return new ArrayObjectWriter(arrayWriter);
  }

  public static AbstractObjectWriter buildMapWriter(ColumnMetadata schema,
      AbstractMapVector vector,
      List<AbstractObjectWriter> writers) {
    if (schema.isArray()) {
      return MapWriter.buildMapArray(schema,
          (RepeatedMapVector) vector, writers);
    } else {
      return MapWriter.buildMap(schema, (MapVector) vector, writers);
    }
  }

  public static AbstractObjectWriter buildMapWriter(ColumnMetadata schema, AbstractMapVector vector) {
    assert schema.tupleSchema().size() == 0;
    return buildMapWriter(schema, vector, new ArrayList<AbstractObjectWriter>());
  }

  @Override
  public ColumnMetadata schema() { return mapColumnSchema; }
}
