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
package org.apache.drill.exec.record;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.LateralContract;
import org.apache.drill.exec.physical.base.PhysicalOperator;

/**
 * Implements AbstractUnaryRecodBatch for operators that do not have an incoming
 * record batch available at creation time; the input is typically set up a few
 * steps after creation. Table functions and operators like Unnest that require
 * input before they can produce output fall into this category. Table functions
 * can be associated with a Lateral operator in which case they simultaneously
 * operate on the same row as the Lateral operator. In this case the
 * LateralContract member is not null and the table function uses the lateral
 * contract to keep in sync with the Lateral operator.
 *
 * @param <T>
 */
public abstract class AbstractTableFunctionRecordBatch<T extends PhysicalOperator> extends
    AbstractUnaryRecordBatch<T> implements TableFunctionContract {

  protected RecordBatch incoming;
  protected LateralContract lateral;

  public AbstractTableFunctionRecordBatch(T popConfig, FragmentContext context) throws OutOfMemoryException {
    super(popConfig, context);
    lateral = null;
  }

  @Override
  protected RecordBatch getIncoming() {
    return incoming;
  }

  @Override
  public void setIncoming(RecordBatch incoming) {
    Preconditions.checkArgument(this.incoming == null, "Incoming is already set. setIncoming cannot be called "
        + "multiple times.");
    this.incoming = incoming;
  }

  @Override
  public void setIncoming(LateralContract incoming) {
    setIncoming(incoming.getIncoming());
    lateral = incoming;
  }
}
