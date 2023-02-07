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
package org.apache.drill.exec.physical.impl.scan.project;

import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;

/**
 * A resolved column has a name, and a specification for how to project
 * data from a source vector to a vector in the final output container.
 * Describes the projection of a single column from
 * an input to an output batch.
 * <p>
 * Although the table schema mechanism uses the newer "metadata"
 * mechanism, resolved columns revert back to the original
 * {@link MajorType} and {@link MaterializedField} mechanism used
 * by the rest of Drill. Doing so loses a bit of additional
 * information, but at present there is no way to export that information
 * along with a serialized record batch; each operator must rediscover
 * it after deserialization.
 */
public abstract class ResolvedColumn implements ColumnProjection {

  private final VectorSource source;
  private final int sourceIndex;
  private final ColumnMetadata outputCol;

  public ResolvedColumn(VectorSource source, int sourceIndex) {
    this.source = source;
    this.sourceIndex = sourceIndex;
    outputCol = null;
  }

  public ResolvedColumn(ColumnMetadata outputCol, VectorSource source, int sourceIndex) {
    this.source = source;
    this.sourceIndex = sourceIndex;
    this.outputCol = outputCol;
  }

  public VectorSource source() { return source; }

  public int sourceIndex() { return sourceIndex; }

  public ColumnMetadata metadata() { return outputCol; }

  /**
   * Return the type of this column. Used primarily by the schema smoothing
   * mechanism.
   *
   * @return the MaterializedField representation of this column
   */
  public abstract MaterializedField schema();

  public void project(ResolvedTuple dest) {
    dest.addVector(source.vector(sourceIndex));
  }
}
