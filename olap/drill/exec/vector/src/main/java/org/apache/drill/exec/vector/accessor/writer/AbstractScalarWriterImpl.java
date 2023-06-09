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

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.accessor.ColumnWriter;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;

/**
 * Column writer implementation that acts as the basis for the
 * generated, vector-specific implementations. All set methods
 * throw an exception; subclasses simply override the supported
 * method(s).
 */
public abstract class AbstractScalarWriterImpl extends AbstractScalarWriter implements WriterEvents {

  /**
   * Wraps a scalar writer and its event handler to provide a uniform
   * JSON-like interface for all writer types.
   * <p>
   * The client sees only the scalar writer API. But, internals need
   * visibility into a rather complex set of events to orchestrate
   * vector events: mostly sent to the writer, but some times sent
   * from the writer, such as vector overflow. Separating the two
   * concepts makes it easier to add type-conversion shims on top of
   * the actual vector writer.
   */
  public static class ScalarObjectWriter extends AbstractObjectWriter {

    private final AbstractScalarWriterImpl scalarWriter;

    public ScalarObjectWriter(AbstractScalarWriterImpl baseWriter) {
      scalarWriter = baseWriter;
    }

    @Override
    public ScalarWriter scalar() { return scalarWriter; }

    @Override
    public ColumnWriter writer() { return scalarWriter; }

    @Override
    public WriterEvents events() { return scalarWriter; }

    @Override
    public void dump(HierarchicalFormatter format) {
      format
        .startObject(this)
        .attribute("scalarWriter");
      scalarWriter.dump(format);
      format.endObject();
    }
  }

  protected ColumnMetadata schema;

  /**
   * Indicates the position in the vector to write. Set via an object so that
   * all writers (within the same subtree) can agree on the write position.
   * For example, all top-level, simple columns see the same row index.
   * All columns within a repeated map see the same (inner) index, etc.
   */
  protected ColumnWriterIndex vectorIndex;

  @Override
  public ObjectType type() { return ObjectType.SCALAR; }

  public void bindSchema(ColumnMetadata schema) {
    this.schema = schema;
  }

  @Override
  public void bindIndex(ColumnWriterIndex vectorIndex) {
    this.vectorIndex = vectorIndex;
  }

  @Override
  public int rowStartIndex() {
    return vectorIndex.rowStartIndex();
  }

  @Override
  public int writeIndex() {
    return vectorIndex.vectorIndex();
  }

  @Override
  public ColumnMetadata schema() { return schema; }

  public abstract BaseDataValueVector vector();

  @Override
  public void startWrite() { }

  @Override
  public void startRow() { }

  @Override
  public void endArrayValue() { }

  @Override
  public void saveRow() { }

  @Override
  public boolean isProjected() { return true; }

  @Override
  public void dump(HierarchicalFormatter format) {
    format
      .startObject(this)
      .attributeIdentity("vector", vector())
      .attribute("schema", vector().getField())
      .endObject();
  }
}
