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
package org.apache.drill.exec.physical.impl.flatten;

import java.util.List;

import org.apache.drill.exec.compile.TemplateClassDefinition;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;

public interface Flattener {
  TemplateClassDefinition<Flattener> TEMPLATE_DEFINITION = new TemplateClassDefinition<Flattener>(Flattener.class, FlattenTemplate.class);

  void setup(FragmentContext context, RecordBatch incoming, RecordBatch outgoing, List<TransferPair> transfers)  throws SchemaChangeException;

  interface Monitor {
    /**
     * Get the required buffer size for the specified number of records.
     * {@see ValueVector#getBufferSizeFor(int)} for the meaning of this.
     *
     * @param recordCount the number of records processed so far
     * @return the buffer size the vectors report as being in use
     */
    int getBufferSizeFor(int recordCount);
  }

  int flattenRecords(int recordCount, int firstOutputIndex, Monitor monitor);

  void setFlattenField(RepeatedValueVector repeatedColumn);
  void setOutputCount(int outputCount);

  RepeatedValueVector getFlattenField();

  void resetGroupIndex();
}
