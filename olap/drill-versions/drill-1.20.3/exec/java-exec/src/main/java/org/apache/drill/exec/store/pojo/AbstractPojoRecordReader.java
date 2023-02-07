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

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.store.AbstractRecordReader;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ControlsInjectorFactory;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parent class for all pojo readers. Pojo readers can be based on java class (field list is predefined) or dynamic.
 * Contains general logic for initiating writers and reading values from each row fields.
 */
public abstract class AbstractPojoRecordReader<T> extends AbstractRecordReader implements Iterable<T> {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractPojoRecordReader.class);
  private static final ControlsInjector injector = ControlsInjectorFactory.getInjector(AbstractPojoRecordReader.class);
  public static final int DEFAULT_RECORDS_PER_BATCH = 4000;

  @JsonProperty private final int recordsPerBatch;
  @JsonProperty protected final List<T> records;

  protected List<PojoWriter> writers;

  private Iterator<T> currentIterator;
  private OperatorContext operatorContext;

  protected AbstractPojoRecordReader(List<T> records) {
    this(records, DEFAULT_RECORDS_PER_BATCH);
  }

  protected AbstractPojoRecordReader(List<T> records, int recordsPerBatch) {
    this.records = records;
    this.recordsPerBatch = Math.min(recordsPerBatch, DEFAULT_RECORDS_PER_BATCH);
  }

  @Override
  public void setup(OperatorContext context, OutputMutator output) throws ExecutionSetupException {
    operatorContext = context;
    writers = setupWriters(output);
    currentIterator = records.iterator();
  }

  @Override
  public int next() {
    boolean allocated = false;
    injector.injectPause(operatorContext.getExecutionControls(), "read-next", logger);

    int recordCount = 0;
    while (currentIterator.hasNext() && recordCount < recordsPerBatch) {
      if (!allocated) {
        allocate();
        allocated = true;
      }

      T row = currentIterator.next();
      for (int i = 0; i < writers.size(); i++) {
        PojoWriter writer = writers.get(i);
        writer.writeField(getFieldValue(row, i), recordCount);
      }
      recordCount++;
    }

    if (recordCount != 0) {
      setValueCount(recordCount);
    }
    return recordCount;
  }

  @Override
  public void allocate(Map<String, ValueVector> vectorMap) throws OutOfMemoryException {
    for (final ValueVector v : vectorMap.values()) {
      AllocationHelper.allocate(v, recordsPerBatch, 50, 10);
    }
  }

  @Override
  public void close() {
  }

  @Override
  public Iterator<T> iterator() {
    return records.iterator();
  }

  /**
   * Creates writer based input class type and then initiates it.
   *
   * @param type class type
   * @param fieldName field name
   * @param output output mutator
   * @return pojo writer
   */
  protected PojoWriter initWriter(Class<?> type, String fieldName, OutputMutator output) throws ExecutionSetupException {
    PojoWriter writer = PojoWriters.getWriter(type, fieldName, output.getManagedBuffer());
    try {
      writer.init(output);
      return writer;
    } catch (SchemaChangeException e) {
      throw new ExecutionSetupException("Failure while setting up schema for AbstractPojoRecordReader.", e);
    }
  }

  /**
   * Allocates buffers for each writer.
   */
  private void allocate() {
    for (PojoWriter writer : writers) {
      writer.allocate();
    }
  }

  /**
   * Sets number of written records for each writer.
   *
   * @param recordCount number of records written
   */
  private void setValueCount(int recordCount) {
    for (PojoWriter writer : writers) {
      writer.setValueCount(recordCount);
    }
  }

  /**
   * Setups writers for each field in the row.
   *
   * @param output output mutator
   * @return list of pojo writers
   */
  protected abstract List<PojoWriter> setupWriters(OutputMutator output) throws ExecutionSetupException;

  /**
   * Retrieves field value to be written based for given row and field position.
   *
   * @param row current row
   * @param fieldPosition current field position
   * @return field value to be written for given row
   */
  protected abstract Object getFieldValue(T row, int fieldPosition);

}
