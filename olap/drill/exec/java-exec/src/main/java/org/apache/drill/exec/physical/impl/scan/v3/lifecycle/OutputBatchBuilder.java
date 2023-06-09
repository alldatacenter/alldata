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
package org.apache.drill.exec.physical.impl.scan.v3.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.exec.expr.BasicTypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Builds an output batch based on an output schema and one or more input
 * schemas. The input schemas must represent disjoint subsets of the output
 * schema.
 * <p>
 * Handles maps, which can overlap at the map level (two inputs can hold a map
 * column named {@code `m`}, say), but the map members must be disjoint. Applies
 * the same rule recursively to nested maps.
 * <p>
 * Maps must be built with members in the same order as the corresponding
 * schema. Though maps are usually thought of as unordered name/value pairs,
 * they are actually tuples, with both a name and a defined ordering.
 * <p>
 * This code uses a name lookup in maps because the semantics of maps do not
 * guarantee a uniform numbering of members from {@code 0} to {@code n-1}, where
 * {code n} is the number of map members. Map members are ordered, but the
 * ordinal used by the map vector is not necessarily sequential.
 * <p>
 * Once the output container is built, the same value vectors reside in the
 * input and output containers. This works because Drill requires vector
 * persistence: the same vectors must be presented downstream in every batch
 * until a schema change occurs.
 *
 * <h4>Projection</h4>
 *
 * To visualize projection, assume we have numbered table columns, lettered
 * implicit, null or partition columns:<pre><code>
 * [ 1 | 2 | 3 | 4 ]    Table columns in table order
 * [ A | B | C ]        Static columns
 * </code></pre>
 * Now, we wish to project them into select order.
 * Let's say that the SELECT clause looked like this, with "t"
 * indicating table columns:<pre><code>
 * SELECT t2, t3, C, B, t1, A, t2 ...
 * </code></pre>
 * Then the projection looks like this:<pre><code>
 * [ 2 | 3 | C | B | 1 | A | 2 ]
 * </code></pre>
 * Often, not all table columns are projected. In this case, the
 * result set loader presents the full table schema to the reader,
 * but actually writes only the projected columns. Suppose we
 * have:<pre><code>
 * SELECT t3, C, B, t1,, A ...
 * </code></pre>
 * Then the abbreviated table schema looks like this:<pre><code>
 * [ 1 | 3 ]</code></pre>
 * Note that table columns retain their table ordering.
 * The projection looks like this:<pre><code>
 * [ 2 | C | B | 1 | A ]
 * </code></pre>
 * <p>
 * The projector is created once per schema, then can be reused for any
 * number of batches.
 * <p>
 * Merging is done in one of two ways, depending on the input source:
 * <ul>
 * <li>For the table loader, the merger discards any data in the output,
 * then exchanges the buffers from the input columns to the output,
 * leaving projected columns empty. Note that unprojected columns must
 * be cleared by the caller.</li>
 * <li>For implicit and null columns, the output vector is identical
 * to the input vector.</li>
 */
public class OutputBatchBuilder {

  /**
   * Describes an input batch with a schema and a vector container.
   */
  public static class BatchSource {
    private final TupleMetadata schema;
    private final VectorContainer container;

    public BatchSource(TupleMetadata schema, VectorContainer container) {
      this.schema = schema;
      this.container = container;
     }
  }

  /**
   * Describes a vector source: an index to a batch source (or nested map
   * source), and an offset into that source. (Actually, an offset into the
   * source schema which is the same as the container offset for the top-level
   * row, but which translates to a name lookup for maps.)
   */
  private static class VectorSource {
    protected final int source;
    protected final int offset;

    public VectorSource(int source, int offset) {
      this.source = source;
      this.offset = offset;
    }

    @Override
    public String toString() {
      return "[source=" + source +
             ", offset=" + offset + "]";
    }
  }

  /**
   * Source map as a map schema and map vector.
   */
  public static class MapSource {
    protected final TupleMetadata mapSchema;
    protected final AbstractMapVector mapVector;

    public MapSource(TupleMetadata mapSchema, AbstractMapVector mapVector) {
      this.mapSchema = mapSchema;
      this.mapVector = mapVector;
    }
  }

  /**
   * Construct a map from an output schema and a set of input maps. The logic
   * is very similar to that for a batch, but just different enough that we need
   * a separate implementation.
   */
  private static class MapBuilder {
    ColumnMetadata outputCol;
    TupleMetadata mapSchema;
    List<MapSource> sourceMaps;
    Object memberSources[];

    private MapBuilder(ColumnMetadata outputCol, List<MapSource> sourceMaps) {
      this.outputCol = outputCol;
      this.mapSchema = outputCol.tupleSchema();
      this.sourceMaps = sourceMaps;
      this.memberSources = new Object[mapSchema.size()];
      for (int i = 0; i < sourceMaps.size(); i++) {
        defineSourceMapMapping(sourceMaps.get(i).mapSchema, i);
      }
    }

    /**
     * Define the mapping for one of the sources. Mappings are
     * stored in output order as a set of (source, offset) pairs.
     */
    @SuppressWarnings("unchecked")
    private void defineSourceMapMapping(TupleMetadata sourceSchema, int source) {
      for (int i = 0; i < sourceSchema.size(); i++) {
        ColumnMetadata col = sourceSchema.metadata(i);
        int outputIndex = mapSchema.index(col.name());
        Preconditions.checkState(outputIndex >= 0);
        VectorSource vectorSource = new VectorSource(source, i);
        if (col.isMap()) {
          if (memberSources[outputIndex] == null) {
            memberSources[outputIndex] = new ArrayList<VectorSource>();
          }
          ((List<VectorSource>) memberSources[outputIndex]).add(vectorSource);
        } else {
          assert memberSources[outputIndex] == null;
          memberSources[outputIndex] = vectorSource;
        }
      }
    }

    /**
     * Creates a new output map vector to hold the merger of member
     * vectors from the source maps.
     */
    @SuppressWarnings("unchecked")
    public AbstractMapVector build(BufferAllocator allocator) {
      AbstractMapVector mapVector = (AbstractMapVector)
          BasicTypeHelper.getNewVector(outputCol.name(),
              allocator, outputCol.majorType(), null);
      for (int i = 0; i < mapSchema.size(); i++) {
        ColumnMetadata outputCol = mapSchema.metadata(i);
        ValueVector outputVector;
        if (outputCol.isMap()) {
          outputVector = buildNestedMap(allocator, outputCol, (List<VectorSource>) memberSources[i]);
        } else {
          outputVector = getMember((VectorSource) memberSources[i]);
        }
        mapVector.putChild(outputCol.name(), outputVector);
      }

      return mapVector;
    }

    private ValueVector buildNestedMap(BufferAllocator allocator, ColumnMetadata outputCol,
        List<VectorSource> vectorSources) {
      List<MapSource> childMaps = new ArrayList<>();
      for (VectorSource source : vectorSources) {
        childMaps.add(
            new MapSource(
                sourceMaps.get(source.source).mapSchema
                  .metadata(source.offset).tupleSchema(),
                (AbstractMapVector) getMember(source)));
      }
      return new MapBuilder(outputCol, childMaps).build(allocator);
    }

    public ValueVector getMember(VectorSource source) {
      MapSource sourceMap = sourceMaps.get(source.source);
      ColumnMetadata sourceCol = sourceMap.mapSchema.metadata(source.offset);
      return sourceMap.mapVector.getChild(sourceCol.name());
    }
  }

  private final TupleMetadata outputSchema;
  private final List<BatchSource> sources;
  private final Object vectorSources[];
  private final VectorContainer outputContainer;
  private final List<MapVector> mapVectors = new ArrayList<>();

  public OutputBatchBuilder(TupleMetadata outputSchema, List<BatchSource> sources,
      BufferAllocator allocator) {
    this.outputSchema = outputSchema;
    this.sources = sources;
    this.outputContainer = new VectorContainer(allocator);
    this.vectorSources = new Object[outputSchema.size()];
    for (int i = 0; i < sources.size(); i++) {
      defineSourceBatchMapping(sources.get(i).schema, i);
    }
    physicalProjection();
  }

  /**
   * Define the mapping for one of the sources. Mappings are
   * stored in output order as a set of (source, offset) pairs.
   */
  @SuppressWarnings("unchecked")
  protected void defineSourceBatchMapping(TupleMetadata schema, int source) {
    for (int i = 0; i < schema.size(); i++) {
      ColumnMetadata col = schema.metadata(i);
      int outputIndex = outputSchema.index(col.name());
      Preconditions.checkState(outputIndex >= 0);
      VectorSource vectorSource = new VectorSource(source, i);
      if (col.isMap()) {
        if (vectorSources[outputIndex] == null) {
          vectorSources[outputIndex] = new ArrayList<VectorSource>();
        }
        ((List<VectorSource>) vectorSources[outputIndex]).add(vectorSource);
      } else {
        assert vectorSources[outputIndex] == null;
        vectorSources[outputIndex] = vectorSource;
      }
    }
  }

  /**
   * Project the source vectors to the output container, merging
   * maps along the way.
   */
  @SuppressWarnings("unchecked")
  private void physicalProjection() {
    outputContainer.removeAll();
    mapVectors.clear();
    for (int i = 0; i < outputSchema.size(); i++) {
      ColumnMetadata outputCol = outputSchema.metadata(i);
      ValueVector outputVector;
      if (outputCol.isMap()) {
        outputVector = buildTopMap(outputCol, (List<VectorSource>) vectorSources[i]);

        // Map vectors are a nuisance: they carry their own value could which
        // must be set separately from the underling data vectors.
        if (outputVector instanceof MapVector) {
          mapVectors.add((MapVector) outputVector);
        }
      } else {
        outputVector = getVector((VectorSource) vectorSources[i]);
      }
      outputContainer.add(outputVector);
    }
    outputContainer.buildSchema(SelectionVectorMode.NONE);
  }

  private ValueVector buildTopMap(ColumnMetadata outputCol,
      List<VectorSource> vectorSources) {
    List<MapSource> sourceMaps = new ArrayList<>();
    for (VectorSource source : vectorSources) {
      sourceMaps.add(
          new MapSource(
              sources.get(source.source).schema.metadata(source.offset).tupleSchema(),
              (AbstractMapVector) getVector(source)));
    }
    return new MapBuilder(outputCol, sourceMaps).build(outputContainer.getAllocator());
  }

  public ValueVector getVector(VectorSource source) {
    return sources.get(source.source).container
        .getValueVector(source.offset).getValueVector();
  }

  public void load(int rowCount) {
    outputContainer.setRecordCount(rowCount);
    for (MapVector v : mapVectors) {
      v.setMapValueCount(rowCount);
    }
  }

  public VectorContainer outputContainer() { return outputContainer; }

  /**
   * Release per-reader resources. Does not release the actual value
   * vectors as those reside in a cache.
   */
  public void close() {
    outputContainer.removeAll();
  }
}
