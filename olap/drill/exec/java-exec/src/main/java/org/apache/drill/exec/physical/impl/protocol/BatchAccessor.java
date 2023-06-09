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

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;

/**
 * Provides access to the row set (record batch) produced by an
 * operator. Previously, a record batch <i>was</i> an operator.
 * In this version, the row set is a service of the operator rather
 * than being part of the operator.
 */

public interface BatchAccessor {
  BatchSchema schema();
  int schemaVersion();
  int rowCount();
  VectorContainer container();
  TypedFieldId getValueVectorId(SchemaPath path);
  VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... ids);
  WritableBatch writableBatch();
  SelectionVector2 selectionVector2();
  SelectionVector4 selectionVector4();
  Iterator<VectorWrapper<?>> iterator();
  void release();
}
