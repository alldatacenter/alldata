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
package org.apache.drill.exec.physical.impl.protocol;

import java.util.Iterator;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modular implementation of the standard Drill record batch iterator
 * protocol. The protocol has two parts: control of the operator and
 * access to the record batch. Each is encapsulated in separate
 * implementation classes to allow easier customization for each
 * situation. The operator internals are, themselves, abstracted to
 * yet another class with the steps represented as method calls rather
 * than as internal states as in the record batch iterator protocol.
 * <p>
 * Note that downstream operators make an assumption that the
 * same vectors will appear from one batch to the next. That is,
 * not only must the schema be the same, but if column "a" appears
 * in two batches, the same value vector must back "a" in both
 * batches. The <tt>TransferPair</tt> abstraction fails if different
 * vectors appear across batches.
 */
public class OperatorRecordBatch implements CloseableRecordBatch {
  static final Logger logger = LoggerFactory.getLogger(OperatorRecordBatch.class);

  private final OperatorDriver driver;
  private final BatchAccessor batchAccessor;
  private IterOutcome lastOutcome;

  public OperatorRecordBatch(FragmentContext context, PhysicalOperator config,
      OperatorExec opExec, boolean enableSchemaBatch) {
    OperatorContext opContext = context.newOperatorContext(config);
    opContext.getStats().startProcessing();

    // Chicken-and-egg binding: the two objects must know about each other. Pass the
    // context to the operator exec via a bind method.

    try {
      opExec.bind(opContext);
      driver = new OperatorDriver(opContext, opExec, enableSchemaBatch);
      batchAccessor = opExec.batchAccessor();
    } catch (UserException e) {
      opContext.close();
      throw e;
    } catch (Throwable t) {
      opContext.close();
      throw UserException.executionError(t)
        .addContext("Exception thrown from", opExec.getClass().getSimpleName() + ".bind()")
        .build(logger);
    }
    finally {
      opContext.getStats().stopProcessing();
    }
  }

  @Override
  public FragmentContext getContext() {
    return fragmentContext();
  }

  // No longer needed, can be removed after all
  // batch size control work is committed.

  public FragmentContext fragmentContext() {
    return driver.operatorContext().getFragmentContext();
  }

  @Override
  public BatchSchema getSchema() { return batchAccessor.schema(); }

  @Override
  public int getRecordCount() { return batchAccessor.rowCount(); }

  @Override
  public VectorContainer getOutgoingContainer() {
    return batchAccessor.container();
  }

  @Override
  public TypedFieldId getValueVectorId(SchemaPath path) {
    return batchAccessor.getValueVectorId(path);
  }

  @Override
  public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... ids) {
    return batchAccessor.getValueAccessorById(clazz, ids);
  }

  @Override
  public WritableBatch getWritableBatch() {
    return batchAccessor.writableBatch();
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    return batchAccessor.selectionVector2();
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    return batchAccessor.selectionVector4();
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return batchAccessor.iterator();
  }

  @Override
  public void cancel() {
    driver.cancel();
  }

  @Override
  public IterOutcome next() {
    try {
      driver.operatorContext().getStats().startProcessing();
      lastOutcome = driver.next();
      return lastOutcome;
    } finally {
      driver.operatorContext().getStats().stopProcessing();
    }
  }

  @Override
  public void close() {
    driver.close();
  }

  @Override
  public VectorContainer getContainer() {
    return batchAccessor.container();
  }

  @Override
  public void dump() {
    logger.error("OperatorRecordBatch[batchAccessor={}, lastOutcome={}]", batchAccessor, lastOutcome);
  }
}
