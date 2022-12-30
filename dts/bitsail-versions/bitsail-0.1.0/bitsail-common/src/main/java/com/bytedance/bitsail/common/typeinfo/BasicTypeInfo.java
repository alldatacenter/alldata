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

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

public class BasicTypeInfo<T> extends TypeInfo<T> {

  private final Class<T> clazz;

  private List<TypeProperty> typeProperties;

  public BasicTypeInfo(Class<T> clazz) {
    this(clazz, Lists.newArrayList());
  }

  public BasicTypeInfo(Class<T> clazz, List<TypeProperty> typeProperties) {
    this.clazz = clazz;
    this.typeProperties = typeProperties;
  }

  @Override
  public Class<T> getTypeClass() {
    return this.clazz;
  }

  @Override
  public String toString() {
    return this.clazz.getSimpleName();
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object obj) {
    if (obj instanceof BasicTypeInfo) {
      BasicTypeInfo<T> other = (BasicTypeInfo<T>) obj;
      return this.clazz == other.clazz;
    } else {
      return false;
    }
  }

  @Override
  public List<TypeProperty> getTypeProperties() {
    return typeProperties;
  }

  @Override
  public void setTypeProperties(List<TypeProperty> typeProperties) {
    this.typeProperties = typeProperties;
  }

  @Override
  public int hashCode() {
    return 31 * Objects.hash(clazz);
  }

}
