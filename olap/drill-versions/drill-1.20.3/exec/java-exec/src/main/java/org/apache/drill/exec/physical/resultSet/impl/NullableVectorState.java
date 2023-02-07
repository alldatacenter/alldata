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

import org.apache.drill.exec.physical.resultSet.impl.SingleVectorState.IsSetVectorState;
import org.apache.drill.exec.physical.resultSet.impl.SingleVectorState.SimpleVectorState;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.NullableVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.apache.drill.exec.vector.accessor.writer.AbstractObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.NullableScalarWriter;

public class NullableVectorState implements VectorState {

  private final ColumnMetadata schema;
  private final NullableScalarWriter writer;
  private final NullableVector vector;
  private final VectorState bitsState;
  private final VectorState valuesState;

  public NullableVectorState(AbstractObjectWriter writer, NullableVector vector) {
    this.schema = writer.schema();
    this.vector = vector;

    this.writer = (NullableScalarWriter) writer.events();
    bitsState = new IsSetVectorState(this.writer.bitsWriter(), vector.getBitsVector());
    valuesState = SimpleVectorState.vectorState(this.writer.schema(),
        this.writer.baseWriter(), vector.getValuesVector());
  }

  @Override
  public int allocate(int cardinality) {
    return bitsState.allocate(cardinality) +
           valuesState.allocate(cardinality);
  }

  @Override
  public void rollover(int cardinality) {
    bitsState.rollover(cardinality);
    valuesState.rollover(cardinality);
  }

  @Override
  public void harvestWithLookAhead() {
    bitsState.harvestWithLookAhead();
    valuesState.harvestWithLookAhead();
  }

  @Override
  public void startBatchWithLookAhead() {
    bitsState.startBatchWithLookAhead();
    valuesState.startBatchWithLookAhead();
  }

  @Override
  public void close() {
    bitsState.close();
    valuesState.close();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ValueVector> T vector() { return (T) vector; }

  @Override
  public boolean isProjected() { return true; }

  @Override
  public void dump(HierarchicalFormatter format) {
    format
      .startObject(this)
      .attribute("schema", schema)
      .attributeIdentity("writer", writer)
      .attributeIdentity("vector", vector)
      .attribute("bitsState");
    bitsState.dump(format);
    format
      .attribute("valuesState");
    valuesState.dump(format);
    format
      .endObject();
  }
}
