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

import java.util.List;

public class ListTypeInfo<T> extends TypeInfo<List<T>> {

  private final TypeInfo<T> elementTypeInfo;

  public ListTypeInfo(TypeInfo<T> elementTypeInfo) {
    Preconditions.checkNotNull(elementTypeInfo, "elementTypeInfo can not be null");
    this.elementTypeInfo = elementTypeInfo;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<List<T>> getTypeClass() {
    return (Class<List<T>>) (Class<?>) List.class;
  }

  @Override
  public String toString() {
    return "List<" + elementTypeInfo + '>';
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof ListTypeInfo) {
      final ListTypeInfo<?> other = (ListTypeInfo<?>) obj;
      return elementTypeInfo.equals(other.elementTypeInfo);
    } else {
      return false;
    }
  }

  public TypeInfo<T> getElementTypeInfo() {
    return this.elementTypeInfo;
  }

  @Override
  public int hashCode() {
    return 31 * elementTypeInfo.hashCode() + 1;
  }
}
