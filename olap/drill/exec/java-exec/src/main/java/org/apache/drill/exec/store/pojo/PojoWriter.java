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
package org.apache.drill.exec.store.pojo;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.physical.impl.OutputMutator;

/**
 * Pojo writer interface for writers based on types supported for pojo.
 */
public interface PojoWriter {

  /**
   * Writes given value to the given position of the bit to set.
   *
   * @param value values to be written
   * @param outboundIndex position of the bit
   */
  void writeField(Object value, int outboundIndex);

  /**
   * Initializes value vector.
   *
   * @param output output mutator
   */
  void init(OutputMutator output) throws SchemaChangeException;

  /**
   * Allocates new buffer for value vector.
   */
  void allocate();

  /**
   * Sets number of written records.
   *
   * @param recordCount record count
   */
  void setValueCount(int recordCount);

  /**
   * Performs clean up if needed.
   */
  void cleanup();
}