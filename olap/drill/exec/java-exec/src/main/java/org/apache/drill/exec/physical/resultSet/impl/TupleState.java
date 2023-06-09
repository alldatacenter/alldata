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
package org.apache.drill.exec.physical.resultSet.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.impl.ColumnState.BaseContainerColumnState;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.apache.drill.exec.vector.accessor.writer.AbstractObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.AbstractTupleWriter;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.drill.exec.vector.complex.RepeatedDictVector;

/**
 * Represents the loader state for a tuple: a row or a map. This is "state" in
 * the sense of variables that are carried along with each tuple. Handles
 * write-time issues such as defining new columns, allocating memory, handling
 * overflow, assembling the output version of the map, and so on. Each
 * row and map in the result set has a tuple state instances associated
 * with it.
 * <p>
 * Here, by "tuple" we mean a container of vectors, each of which holds
 * a variety of values. So, the "tuple" here is structural, not a specific
 * set of values, but rather the collection of vectors that hold tuple
 * values.
 *
 * Drill vector containers and maps are both tuples, but they irritatingly
 * have completely different APIs for working with their child vectors.
 * These classes are a proxy to wrap the two APIs to provide a common
 * view for the use the result set builder and its internals.
 *
 * <h4>Output Container</h4>
 *
 * Builds the harvest vector container that includes only the columns that
 * are included in the harvest schema version. That is, it excludes columns
 * added while writing an overflow row.
 * <p>
 * Because a Drill row is actually a hierarchy, walks the internal hierarchy
 * and builds a corresponding output hierarchy.
 * <ul>
 * <li>The root node is the row itself (vector container),</li>
 * <li>Internal nodes are maps (structures),</li>
 * <li>Leaf notes are primitive vectors (which may be arrays).</li>
 * </ul>
 * The basic algorithm is to identify the version of the output schema,
 * then add any new columns added up to that version. This object maintains
 * the output container across batches, meaning that updates are incremental:
 * we need only add columns that are new since the last update. And, those new
 * columns will always appear directly after all existing columns in the row
 * or in a map.
 * <p>
 * As special case occurs when columns are added in the overflow row. These
 * columns <i>do not</i> appear in the output container for the main part
 * of the batch; instead they appear in the <i>next</i> output container
 * that includes the overflow row.
 * <p>
 * Since the container here may contain a subset of the internal columns, an
 * interesting case occurs for maps. The maps in the output container are
 * <b>not</b> the same as those used internally. Since a map column can contain
 * either one list of columns or another, the internal and external maps must
 * differ. The set of child vectors (except for child maps) are shared.
 */
public abstract class TupleState extends ContainerState
  implements AbstractTupleWriter.TupleWriterListener {

  /**
   * The set of columns added via the writers: includes both projected
   * and unprojected columns. (The writer is free to add columns that the
   * query does not project; the result set loader creates a dummy column
   * and dummy writer, then does not project the column to the output.)
   */
  protected final List<ColumnState> columns = new ArrayList<>();

  /**
   * Internal writer schema that matches the column list.
   */
  protected final TupleMetadata schema = new TupleSchema();

  /**
   * Metadata description of the output container (for the row) or map
   * (for map or repeated map.)
   * <p>
   * Rows and maps have an output schema which may differ from the internal schema.
   * The output schema excludes unprojected columns. It also excludes
   * columns added in an overflow row.
   * <p>
   * The output schema is built slightly differently for maps inside a
   * union vs. normal top-level (or nested) maps. Maps inside a union do
   * not defer columns because of the muddy semantics (and infrequent use)
   * of unions.
   */
  protected TupleMetadata outputSchema;

  private int prevHarvestIndex = -1;

  protected TupleState(LoaderInternals events,
      ResultVectorCache vectorCache,
      ProjectionFilter projectionSet) {
    super(events, vectorCache, projectionSet);
  }

  protected void bindOutputSchema(TupleMetadata outputSchema) {
    this.outputSchema = outputSchema;
  }

  /**
   * Returns an ordered set of the columns which make up the tuple.
   * Column order is the same as that defined by the map's schema,
   * to allow indexed access. New columns always appear at the end
   * of the list to preserve indexes.
   *
   * @return ordered list of column states for the columns within
   * this tuple
   */
  public List<ColumnState> columns() { return columns; }

  public TupleMetadata schema() { return writer().tupleSchema(); }

  public abstract AbstractTupleWriter writer();

  @Override
  public boolean isProjected(String colName) {
    return projectionSet.isProjected(colName);
  }

  @Override
  public ObjectWriter addColumn(TupleWriter tupleWriter, MaterializedField column) {
    return addColumn(tupleWriter, MetadataUtils.fromField(column));
  }

  @Override
  public ObjectWriter addColumn(TupleWriter tupleWriter, ColumnMetadata columnSchema) {
    return BuildFromSchema.instance().buildColumn(this, columnSchema);
  }

  @Override
  protected void addColumn(ColumnState colState) {
    columns.add(colState);
  }

  public boolean hasProjections() {
    for (final ColumnState colState : columns) {
      if (colState.isProjected()) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected Collection<ColumnState> columnStates() {
    return columns;
  }

  protected void updateOutput(int curSchemaVersion) {

    // Scan all columns
    for (int i = 0; i < columns.size(); i++) {
      final ColumnState colState = columns.get(i);

      // Ignore unprojected columns
      if (! colState.writer().isProjected()) {
        continue;
      }

      // If this is a new column added since the last output, then we may have
      // to add the column to this output. For the row itself, and for maps
      // outside of unions, If the column was added after the output schema
      // version cutoff, skip that column for now. But, if this tuple is
      // within a union, then we always add all columns because union
      // semantics are too muddy to play the deferred column game. Further,
      // all columns in a map within a union must be nullable, so we know we
      // can fill the column with nulls. (Something that is not true for
      // normal maps.)

      if (i > prevHarvestIndex && (! isVersioned() || colState.addVersion <= curSchemaVersion)) {
        colState.buildOutput(this);
        prevHarvestIndex = i;
      }

      // If the column is a map or a dict, then we have to recurse into it
      // itself. If the map is inside a union, then the map's vectors
      // already appear in the map vector, but we still must update the
      // output schema.

      if (colState.schema().isMap()) {
        final MapState childMap = ((MapColumnState) colState).mapState();
        childMap.updateOutput(curSchemaVersion);
      } else if (colState.schema().isDict()) {
        final DictState child = ((DictColumnState) colState).dictState();
        child.updateOutput(curSchemaVersion);
      }
    }
  }

  public abstract int addOutputColumn(ValueVector vector, ColumnMetadata colSchema);

  public TupleMetadata outputSchema() { return outputSchema; }

  public void dump(HierarchicalFormatter format) {
    format
      .startObject(this)
      .attributeArray("columns");
    for (int i = 0; i < columns.size(); i++) {
      format.element(i);
      columns.get(i).dump(format);
    }
    format
      .endArray()
      .endObject();
  }

  /**
   * Represents a map column (either single or repeated). Includes maps that
   * are top-level, nested within other maps, or nested inside a union.
   * Schema management is a bit complex:
   * <table border=1>
   * <tr><th rowspan=2>Condition</th><th colspan=2>Action</th></tr>
   * <tr><th>Outside of Union</th><th>Inside of Union<th></tr>
   * <tr><td>Unprojected</td><td>N/A</td><td>Omitted from output</td></tr>
   * <tr><td>Added in prior batch</td><td colspan=2>Included in output</td></tr>
   * <tr><td>Added in present batch, before overflow</td>
   *     <td colspan=2>Included in output</td></tr>
   * <tr><td>Added in present batch, after overflow</td>
   *     <td>Omitted from output this batch (added next batch)</td>
   *     <td>Included in output</td></tr>
   * </table>
   * <p>
   * The above rules say that, for maps in a union, the output schema
   * is identical to the internal writer schema. But, for maps outside
   * of union, the output schema is a subset of the internal schema with
   * two types of omissions:
   * <ul>
   * <li>Unprojected columns</li>
   * <li>Columns added after overflow</li>
   * </ul
   * <p>
   * New columns can be added at any time for data readers that discover
   * their schema as data is read (such as JSON). In this case, new columns
   * always appear at the end of the map (remember, in Drill, a "map" is actually
   * a structured: an ordered, named list of columns.) When looking for newly
   * added columns, they will always be at the end.
   */
  public static class MapColumnState extends BaseContainerColumnState {
    protected final MapState mapState;
    protected boolean isVersioned;
    protected final ColumnMetadata outputSchema;

    public MapColumnState(MapState mapState,
        AbstractObjectWriter writer,
        VectorState vectorState,
        boolean isVersioned) {
      super(mapState.loader(), writer, vectorState);
      this.mapState = mapState;
      mapState.bindColumnState(this);
      this.isVersioned = isVersioned;
      if (isVersioned) {
        outputSchema = schema().cloneEmpty();
      } else {
        outputSchema = schema();
      }
      mapState.bindOutputSchema(outputSchema.tupleSchema());
    }

    public MapState mapState() { return mapState; }

    @Override
    public ContainerState container() { return mapState; }

    @Override
    public boolean isProjected() {
      return mapState.hasProjections();
    }

    /**
     * Indicate if this map is versioned. A versionable map has three attributes:
     * <ol>
     * <li>Columns can be unprojected. (Columns appear as writers for the client
     * of the result set loader, but are not materialized and do not appear in
     * the projected output container.</li>
     * <li>Columns appear in the output only if added before the overflow row.</li>
     * <li>As a result, the output schema is a subset of the internal input
     * schema.</li>
     * </ul>
     * @return <tt>true</tt> if this map is versioned as described above
     */
    public boolean isVersioned() { return isVersioned; }

    @Override
    public ColumnMetadata outputSchema() { return outputSchema; }
  }

  /**
   * State for a map vector. If the map is repeated, it will have an offset
   * vector. The map vector itself is a pseudo-vector that is simply a
   * container for other vectors, and so needs no management itself.
   */
  public static class MapVectorState implements VectorState {

    private final AbstractMapVector mapVector;
    private final VectorState offsets;

    public MapVectorState(AbstractMapVector mapVector, VectorState offsets) {
      this.mapVector = mapVector;
      this.offsets = offsets;
    }

    @Override
    public int allocate(int cardinality) {
      // The mapVector is a pseudo-vector; nothing to allocate.

      return offsets.allocate(cardinality);
    }

    @Override
    public void rollover(int cardinality) {
      offsets.rollover(cardinality);
    }

    @Override
    public void harvestWithLookAhead() {
      offsets.harvestWithLookAhead();
    }

    @Override
    public void startBatchWithLookAhead() {
      offsets.harvestWithLookAhead();
    }

    @Override
    public void close() {
      offsets.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public AbstractMapVector vector() { return mapVector; }

    public VectorState offsetVectorState() { return offsets; }

    @Override
    public boolean isProjected() {
      return offsets.isProjected();
    }

    @Override
    public void dump(HierarchicalFormatter format) {
      format.startObject(this)
          .attribute("field", mapVector != null ? mapVector.getField() : "null")
          .endObject();
    }
  }

  /**
   * Handles the details of the top-level tuple, the data row itself.
   * Note that by "row" we mean the set of vectors that define the
   * set of rows.
   */
  public static class RowState extends TupleState {

    /**
     * The row-level writer for stepping through rows as they are written,
     * and for accessing top-level columns.
     */
    private final RowSetLoaderImpl writer;

    /**
     * The projected set of columns presented to the consumer of the
     * row set loader. Excludes non-projected columns presented to the
     * consumer of the writers. Also excludes columns if added during
     * an overflow row.
     */
    private final VectorContainer outputContainer;

    public RowState(ResultSetLoaderImpl rsLoader, ResultVectorCache vectorCache) {
      super(rsLoader, vectorCache, rsLoader.projectionSet());
      writer = new RowSetLoaderImpl(rsLoader, schema);
      writer.bindListener(this);
      outputContainer = new VectorContainer(rsLoader.allocator());
      outputSchema = new TupleSchema();
    }

    public RowSetLoaderImpl rootWriter() { return writer; }

    @Override
    public AbstractTupleWriter writer() { return writer; }

    @Override
    public int innerCardinality() { return loader.targetRowCount();}

    /**
     * The row as a whole is versioned.
     *
     * @return <tt>true</tt>
     */
    @Override
    protected boolean isVersioned() { return true; }

    @Override
    protected void updateOutput(int curSchemaVersion) {
      super.updateOutput(curSchemaVersion);
      outputContainer.buildSchema(SelectionVectorMode.NONE);
    }

    @Override
    public int addOutputColumn(ValueVector vector, ColumnMetadata colSchema) {
      outputContainer.add(vector);
      final int index = outputSchema.addColumn(colSchema);
      assert outputContainer.getNumberOfColumns() == outputSchema.size();
      return index;
    }

    public VectorContainer outputContainer() { return outputContainer; }
  }

  /**
   * Represents a tuple defined as a Drill map: single or repeated. Note that
   * the map vector does not exist here; it is assembled only when "harvesting"
   * a batch. This design supports the obscure case in which a new column
   * is added during an overflow row, so exists within this abstraction,
   * but is not published to the map that makes up the output.
   * <p>
   * The map state is associated with a map vector. This vector is built
   * either during harvest time (normal maps) or on the fly (union maps.)
   */
  public static abstract class MapState extends TupleState {

    public MapState(LoaderInternals events,
        ResultVectorCache vectorCache,
        ProjectionFilter projectionSet) {
      super(events, vectorCache, projectionSet);
    }

    public void bindColumnState(MapColumnState colState) {
      super.bindColumnState(colState);
      writer().bindListener(this);
    }

    @Override
    public int addOutputColumn(ValueVector vector, ColumnMetadata colSchema) {
      final AbstractMapVector mapVector = parentColumn.vector();
      if (isVersioned()) {
        mapVector.putChild(colSchema.name(), vector);
      }
      final int index = outputSchema.addColumn(colSchema);
      assert mapVector.size() == outputSchema.size();
      assert mapVector.getField().getChildren().size() == outputSchema.size();
      return index;
    }

    @Override
    protected void addColumn(ColumnState colState) {
      super.addColumn(colState);

      // If the map is materialized (because it is nested inside a union)
      // then add the new vector to the map at add time. But, for top-level
      // maps, or those nested inside other maps (but not a union or
      // repeated list), defer
      // adding the column until harvest time, to allow for the case that
      // new columns are added in the overflow row. Such columns may be
      // required, and not allow back-filling. But, inside unions, all
      // columns must be nullable, so back-filling of nulls is possible.

      if (! isVersioned()) {
        final AbstractMapVector mapVector = parentColumn.vector();
        mapVector.putChild(colState.schema().name(), colState.vector());
      }
    }

    /**
     * A map is within a union if the map vector has been materialized.
     * Top-level maps are built at harvest time. But, due to the complexity
     * of unions, maps within unions are materialized. This method ensures
     * that maps are materialized regardless of nesting depth within
     * a union.
     */
    @Override
    protected boolean isVersioned() {
      return ((MapColumnState) parentColumn).isVersioned();
    }

    @Override
    public int innerCardinality() {
      return parentColumn.innerCardinality();
    }

    @Override
    public void dump(HierarchicalFormatter format) {
      format
        .startObject(this)
        .attribute("column", parentColumn.schema().name())
        .attribute("cardinality", innerCardinality())
        .endObject();
    }
  }

  public static class SingleMapState extends MapState {

    public SingleMapState(LoaderInternals events,
        ResultVectorCache vectorCache,
        ProjectionFilter projectionSet) {
      super(events, vectorCache, projectionSet);
    }

    /**
     * Return the tuple writer for the map. If this is a single
     * map, then it is the writer itself. If this is a map array,
     * then the tuple is nested inside the array.
     */
    @Override
    public AbstractTupleWriter writer() {
      return (AbstractTupleWriter) parentColumn.writer().tuple();
    }
  }

  public static class MapArrayState extends MapState {

    public MapArrayState(LoaderInternals events,
        ResultVectorCache vectorCache,
        ProjectionFilter projectionSet) {
      super(events, vectorCache, projectionSet);
    }

    /**
     * Return the tuple writer for the map. If this is a single
     * map, then it is the writer itself. If this is a map array,
     * then the tuple is nested inside the array.
     */
    @Override
    public AbstractTupleWriter writer() {
      return (AbstractTupleWriter) parentColumn.writer().array().tuple();
    }
  }
  public static class DictColumnState extends BaseContainerColumnState {
    protected final DictState dictState;
    protected boolean isVersioned;
    protected final ColumnMetadata outputSchema;

    public DictColumnState(DictState dictState,
                          AbstractObjectWriter writer,
                          VectorState vectorState,
                          boolean isVersioned) {
      super(dictState.loader(), writer, vectorState);
      this.dictState = dictState;
      dictState.bindColumnState(this);
      this.isVersioned = isVersioned;
      if (isVersioned) {
        outputSchema = schema().cloneEmpty();
      } else {
        outputSchema = schema();
      }
      dictState.bindOutputSchema(outputSchema.tupleSchema());
    }

    @Override
    public void buildOutput(TupleState tupleState) {
      outputIndex = tupleState.addOutputColumn(vector(), outputSchema());
    }

    public DictState dictState() {
      return dictState;
    }

    @Override
    public ContainerState container() {
      return dictState;
    }

    @Override
    public boolean isProjected() {
      return dictState.hasProjections();
    }

    public boolean isVersioned() {
      return isVersioned;
    }

    @Override
    public ColumnMetadata outputSchema() { return outputSchema; }
  }

  public static abstract class DictState extends MapState {

    public DictState(LoaderInternals events,
                    ResultVectorCache vectorCache,
                    ProjectionFilter projectionSet) {
      super(events, vectorCache, projectionSet);
    }

    @Override
    public void bindColumnState(ColumnState colState) {
      super.bindColumnState(colState);
      writer().bindListener(this);
    }

    @Override
    protected boolean isVersioned() {
      return ((DictColumnState) parentColumn).isVersioned();
    }

    @Override
    public void dump(HierarchicalFormatter format) {
      format.startObject(this)
          .attribute("column", parentColumn.schema().name())
          .attribute("cardinality", innerCardinality())
          .endObject();
    }
  }

  public static class SingleDictState extends DictState {

    public SingleDictState(LoaderInternals events,
                          ResultVectorCache vectorCache,
                          ProjectionFilter projectionSet) {
      super(events, vectorCache, projectionSet);
    }

    @Override
    public AbstractTupleWriter writer() {
      return (AbstractTupleWriter) parentColumn.writer().dict().tuple();
    }
  }

  public static class DictArrayState extends DictState {

    public DictArrayState(LoaderInternals events,
                         ResultVectorCache vectorCache,
                         ProjectionFilter projectionSet) {
      super(events, vectorCache, projectionSet);
    }

    @Override
    public int addOutputColumn(ValueVector vector, ColumnMetadata colSchema) {
      final RepeatedDictVector repeatedDictVector = parentColumn.vector();
      DictVector dictVector = (DictVector) repeatedDictVector.getDataVector();
      if (isVersioned()) {
        dictVector.putChild(colSchema.name(), vector);
      }
      final int index = outputSchema.addColumn(colSchema);
      assert dictVector.size() == outputSchema.size();
      assert dictVector.getField().getChildren().size() == outputSchema.size();
      return index;
    }

    @Override
    public AbstractTupleWriter writer() {
      return (AbstractTupleWriter) parentColumn.writer().array().dict().tuple();
    }
  }

  public static abstract class DictVectorState<T extends ValueVector> implements VectorState {

    protected final T vector;
    protected final VectorState offsets;

    public DictVectorState(T vector, VectorState offsets) {
      this.vector = vector;
      this.offsets = offsets;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T vector() {
      return vector;
    }

    @Override
    public int allocate(int cardinality) {
      return offsets.allocate(cardinality);
    }

    @Override
    public void rollover(int cardinality) {
      offsets.rollover(cardinality);
    }

    @Override
    public void harvestWithLookAhead() {
      offsets.harvestWithLookAhead();
    }

    @Override
    public void startBatchWithLookAhead() {
      offsets.harvestWithLookAhead();
    }

    @Override
    public void close() {
      offsets.close();
    }

    public VectorState offsetVectorState() {
      return offsets;
    }

    @Override
    public boolean isProjected() {
      return offsets.isProjected();
    }

    @Override
    public void dump(HierarchicalFormatter format) {
      format.startObject(this)
          .attribute("field", vector != null ? vector.getField() : "null")
          .endObject();
    }
  }

  public static class SingleDictVectorState extends DictVectorState<DictVector> {

    public SingleDictVectorState(DictVector vector, VectorState offsets) {
      super(vector, offsets);
    }
  }

  public static class DictArrayVectorState extends DictVectorState<RepeatedDictVector> {

    // offsets for the data vector
    private final VectorState dictOffsets;

    public DictArrayVectorState(RepeatedDictVector vector, VectorState offsets, VectorState dictOffsets) {
      super(vector, offsets);
      this.dictOffsets = dictOffsets;
    }

    @Override
    public int allocate(int cardinality) {
      return offsets.allocate(cardinality);
    }

    @Override
    public void rollover(int cardinality) {
      super.rollover(cardinality);
      dictOffsets.rollover(cardinality);
    }

    @Override
    public void harvestWithLookAhead() {
      super.harvestWithLookAhead();
      dictOffsets.harvestWithLookAhead();
    }

    @Override
    public void startBatchWithLookAhead() {
      super.startBatchWithLookAhead();
      dictOffsets.harvestWithLookAhead();
    }

    @Override
    public void close() {
      super.close();
      dictOffsets.close();
    }
  }
}
