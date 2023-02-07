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
package org.apache.drill.exec.vector.accessor;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.vector.ValueVector;


public class GenericAccessor extends AbstractSqlAccessor {

  private ValueVector v;

  public GenericAccessor(ValueVector v) {
    this.v = v;
  }

  @Override
  public Class<?> getObjectClass() {
    return Object.class;
  }

  @Override
  public boolean isNull(int index) {
    return v.getAccessor().isNull(index);
  }

  @Override
  public Object getObject(int index) throws InvalidAccessException {
    return v.getAccessor().getObject(index);
  }

  @Override
  public TypeProtos.MajorType getType() {
    return v.getMetadata().getMajorType();
  }
}
