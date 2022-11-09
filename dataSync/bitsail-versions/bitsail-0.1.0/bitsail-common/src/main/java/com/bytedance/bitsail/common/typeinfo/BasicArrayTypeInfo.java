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

import com.bytedance.bitsail.common.util.Preconditions;

import java.util.Objects;

public class BasicArrayTypeInfo<T> extends TypeInfo<T> {

  public static final BasicArrayTypeInfo<byte[]> BINARY_TYPE_INFO =
      new BasicArrayTypeInfo<>(byte[].class);

  private final Class<T> arrayClass;

  protected BasicArrayTypeInfo(Class<T> arrayClass) {
    Preconditions.checkNotNull(arrayClass);
    this.arrayClass = arrayClass;
  }

  @Override
  public Class<T> getTypeClass() {
    return this.arrayClass;
  }

  @Override
  public String toString() {
    return arrayClass.getComponentType().getName() + "[]";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BasicArrayTypeInfo) {
      BasicArrayTypeInfo<?> other = (BasicArrayTypeInfo<?>) obj;
      return arrayClass == other.arrayClass;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(arrayClass);
  }
}
