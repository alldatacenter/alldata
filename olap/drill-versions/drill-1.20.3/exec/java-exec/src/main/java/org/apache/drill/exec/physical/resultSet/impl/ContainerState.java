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

import java.util.Collection;

import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.record.metadata.ColumnMetadata;

/**
 * Abstract representation of a container of vectors: a row, a map, a
 * repeated map, a list or a union.
 * <p>
 * The container is responsible for creating new columns in response
 * from a writer listener event. Column creation requires a set of
 * four items:
 * <ul>
 * <li>The value vector (which may be null if the column is not
 * projected.</li>
 * <li>The writer for the column.</li>
 * <li>A vector state that manages allocation, overflow, cleanup
 * and other vector-specific tasks.</li>
 * <li>A column state which orchestrates the above three items.</li>
 * <ul>
 */
public abstract class ContainerState {

  protected final LoaderInternals loader;
  protected final ProjectionFilter projectionSet;
  protected ColumnState parentColumn;

  /**
   * Vector cache for this loader.
   * {@see ResultSetOptionBuilder#setVectorCache()}.
   */
  protected final ResultVectorCache vectorCache;

  public ContainerState(LoaderInternals loader, ResultVectorCache vectorCache, ProjectionFilter projectionSet) {
    this.loader = loader;
    this.vectorCache = vectorCache;
    this.projectionSet = projectionSet;
  }

  public ContainerState(LoaderInternals loader, ResultVectorCache vectorCache) {
    this(loader, vectorCache, ProjectionFilter.PROJECT_ALL);
  }

  public void bindColumnState(ColumnState parentState) {
    this.parentColumn = parentState;
  }

  public abstract int innerCardinality();
  protected abstract void addColumn(ColumnState colState);
  protected abstract Collection<ColumnState> columnStates();
  protected ProjectionFilter projection() { return projectionSet; }

  /**
   * Reports whether this container is subject to version management. Version
   * management adds columns to the output container at harvest time based on
   * whether they should appear in the output batch.
   *
   * @return {@code true} if versioned
   */
  protected abstract boolean isVersioned();

  protected LoaderInternals loader() { return loader; }
  public ResultVectorCache vectorCache() { return vectorCache; }

  public ColumnState addColumn(ColumnMetadata columnSchema) {

    // Create the vector, writer and column state
    ColumnState colState = loader.columnBuilder().buildColumn(this, columnSchema);

    // Add the column to this container
    addColumn(colState);

    // Set initial cardinality
    colState.updateCardinality(innerCardinality());

    // Allocate vectors if a batch is in progress.
    if (loader().writeable()) {
      colState.allocateVectors();
    }
    return colState;
  }

  /**
   * In order to allocate the correct-sized vectors, the container must know
   * its member cardinality: the number of elements in each row. This
   * is 1 for a single map or union, but may be any number for a map array
   * or a list. Then,
   * this value is recursively pushed downward to compute the cardinality
   * of lists of maps that contains lists of maps, and so on.
   */
  public void updateCardinality() {
    int innerCardinality = innerCardinality();
    assert innerCardinality > 0;
    for (ColumnState colState : columnStates()) {
      colState.updateCardinality(innerCardinality);
    }
  }

  /**
   * Start a new batch by shifting the overflow buffers back into the main
   * write vectors and updating the writers.
   */
  public void startBatch(boolean schemaOnly) {
    for (ColumnState colState : columnStates()) {
      colState.startBatch(schemaOnly);
    }
  }

  /**
   * A column within the row batch overflowed. Prepare to absorb the rest of the
   * in-flight row by rolling values over to a new vector, saving the complete
   * vector for later. This column could have a value for the overflow row, or
   * for some previous row, depending on exactly when and where the overflow
   * occurs.
   */
  public void rollover() {
    for (ColumnState colState : columnStates()) {
      colState.rollover();
    }
  }

  /**
   * Writing of a row batch is complete, and an overflow occurred. Prepare the
   * vector for harvesting to send downstream. Set aside the look-ahead vector
   * and put the full vector buffer back into the active vector.
   */
  public void harvestWithLookAhead() {
    for (ColumnState colState : columnStates()) {
      colState.harvestWithLookAhead();
    }
  }

  /**
   * Clean up state (such as backup vectors) associated with the state
   * for each vector.
   */
  public void close() {
    for (ColumnState colState : columnStates()) {
      colState.close();
    }
  }
}
