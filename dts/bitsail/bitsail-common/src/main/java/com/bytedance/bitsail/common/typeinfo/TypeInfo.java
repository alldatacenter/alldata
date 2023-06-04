/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.common.typeinfo;

import java.io.Serializable;
import java.util.List;

public abstract class TypeInfo<T> implements Serializable {
  /**
   * Gets the class of the type represented by this type information.
   *
   * @return The class of the type represented by this type information.
   */
  public abstract Class<T> getTypeClass();

  /**
   * Indicate type extension properties, prepare for future.
   * Extension properties like
   * <pre>
   *    nullable;
   *    not null'
   *  </pre>
   */
  public List<TypeProperty> getTypeProperties() {
    throw new UnsupportedOperationException();
  }

  public void setTypeProperties(List<TypeProperty> typeProperties) {

  }
}
