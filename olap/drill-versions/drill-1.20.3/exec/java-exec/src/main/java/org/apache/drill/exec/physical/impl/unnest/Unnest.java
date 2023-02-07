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
package org.apache.drill.exec.physical.impl.unnest;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;

import java.util.List;

/**
 * Placeholder for future unnest implementation that may require code generation. Current implementation does not
 * require any
 * @see UnnestImpl
 */
public interface Unnest {
  //TemplateClassDefinition<Unnest> TEMPLATE_DEFINITION = new TemplateClassDefinition<Unnest>(Unnest.class, UnnestImpl
  // .class);

  void setup(FragmentContext context, RecordBatch incoming, RecordBatch outgoing, List<TransferPair> transfers)
      throws SchemaChangeException;

  /**
   * Performs the actual unnest operation.
   * @param recordCount
   * @return number of values in output
   */
  int unnestRecords(int recordCount);

  /**
   * Set the field to be unnested
   * @param repeatedColumn
   */
  void setUnnestField(RepeatedValueVector repeatedColumn);

  /**
   * Set the maximum number of values allowed in the output.
   * @param outputCount
   */
  void setOutputCount(int outputCount);

  RepeatedValueVector getUnnestField();

  /**
   * Set the vector for the rowId implicit column
   */
  void setRowIdVector (IntVector v);

  /**
   * Reset the index at which the incoming vector is being processed. Called every
   * time a new batch comes in.
   */
  void resetGroupIndex();

  void close();
}
