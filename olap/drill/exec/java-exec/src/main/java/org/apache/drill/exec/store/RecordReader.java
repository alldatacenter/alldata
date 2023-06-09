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
package org.apache.drill.exec.store;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.planner.sql.handlers.FindLimit0Visitor;
import org.apache.drill.exec.store.pojo.PojoRecordReader;
import org.apache.drill.exec.vector.ValueVector;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PojoRecordReader.class, name = "PojoRecordReader"),
    @JsonSubTypes.Type(value = FindLimit0Visitor.RelDataTypeReader.class, name = "RelDataTypeRecordReader") })
public interface RecordReader extends AutoCloseable {
  long ALLOCATOR_INITIAL_RESERVATION = 1 * 1024 * 1024;
  long ALLOCATOR_MAX_RESERVATION = 20L * 1000 * 1000 * 1000;

  /**
   * Configure the RecordReader with the provided schema and the record batch that should be written to.
   *
   * @param context operator context for the reader
   * @param output
   *          The place where output for a particular scan should be written. The record reader is responsible for
   *          mutating the set of schema values for that particular record.
   * @throws ExecutionSetupException
   */
  void setup(OperatorContext context, OutputMutator output) throws ExecutionSetupException;

  void allocate(Map<String, ValueVector> vectorMap) throws OutOfMemoryException;

  /**
   * Check if the reader may have potentially more data to be read in subsequent iterations. Certain types of readers
   * such as repeatable readers can be invoked multiple times, so this method will allow ScanBatch to check with
   * the reader before closing it.
   * @return return true if there could potentially be more reads, false otherwise
   */
  boolean hasNext();

  /**
   * Increments this record reader forward, writing via the provided output
   * mutator into the output batch.
   *
   * @return The number of additional records added to the output.
   */
  int next();
}
