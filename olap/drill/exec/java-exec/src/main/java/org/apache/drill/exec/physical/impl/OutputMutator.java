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
package org.apache.drill.exec.physical.impl;

import io.netty.buffer.DrillBuf;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.vector.ValueVector;

/**
 * Interface that allows a record reader to modify the current schema.
 *
 * The output mutator interface abstracts ValueVector creation and maintenance away from any particular RecordReader.
 * This means, among other things, that a new RecordReader that shares the same column definitions in a different order
 * does not generate a Schema change event for downstream consumers.
 */
public interface OutputMutator {

  /**
   * Add a ValueVector for new (or existing) field.
   *
   * @param field
   *          The specification of the newly desired vector.
   * @param clazz
   *          The expected ValueVector class. Also allows strongly typed use of this interface.
   *
   * @return The existing or new ValueVector associated with the provided field.
   *
   * @throws SchemaChangeException
   *           If the addition of this field is incompatible with this OutputMutator's capabilities.
   */
  public <T extends ValueVector> T addField(MaterializedField field, Class<T> clazz) throws SchemaChangeException;

  public void allocate(int recordCount);

  /**
   * Whether or not the fields added to the OutputMutator generated a new schema event.
   * @return
   */
  // TODO(DRILL-2970)
  public boolean isNewSchema();

  /**
   * Allows a scanner to request a set of managed block of memory.
   * @return A DrillBuf that will be released at the end of the current query (and can be resized as desired during use).
   */
  public DrillBuf getManagedBuffer();

  /**
   *
   * @return the CallBack object for this mutator
   */
  public CallBack getCallBack();

  /**
   * Clear this mutator i.e. reset it to pristine condition
   */
  public void clear();
}
