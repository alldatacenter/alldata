/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.flink.core.typeinfo;

import com.bytedance.bitsail.common.column.BooleanColumn;
import com.bytedance.bitsail.common.column.BytesColumn;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.common.column.DoubleColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.flink.core.typeutils.base.BoolColumnComparator;
import com.bytedance.bitsail.flink.core.typeutils.base.BoolColumnSerializer;
import com.bytedance.bitsail.flink.core.typeutils.base.BytesColumnComparator;
import com.bytedance.bitsail.flink.core.typeutils.base.BytesColumnSerializer;
import com.bytedance.bitsail.flink.core.typeutils.base.DateColumnComparator;
import com.bytedance.bitsail.flink.core.typeutils.base.DateColumnSerializer;
import com.bytedance.bitsail.flink.core.typeutils.base.DoubleColumnComparator;
import com.bytedance.bitsail.flink.core.typeutils.base.DoubleColumnSerializer;
import com.bytedance.bitsail.flink.core.typeutils.base.LongColumnComparator;
import com.bytedance.bitsail.flink.core.typeutils.base.LongColumnSerializer;
import com.bytedance.bitsail.flink.core.typeutils.base.StringColumnComparator;
import com.bytedance.bitsail.flink.core.typeutils.base.StringColumnSerializer;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.functions.InvalidTypesException;
import org.apache.flink.api.common.typeinfo.AtomicType;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeComparator;
import org.apache.flink.api.common.typeutils.TypeSerializer;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * @desc:
 */
public class PrimitiveColumnTypeInfo<T> extends TypeInformation<T> implements AtomicType<T> {

  public static final PrimitiveColumnTypeInfo<StringColumn> STRING_COLUMN_TYPE_INFO = new PrimitiveColumnTypeInfo<>(StringColumn.class, new Class<?>[] {},
      StringColumnSerializer.INSTANCE, StringColumnComparator.class);
  public static final PrimitiveColumnTypeInfo<BooleanColumn> BOOL_COLUMN_TYPE_INFO = new PrimitiveColumnTypeInfo<>(BooleanColumn.class, new Class<?>[] {},
      BoolColumnSerializer.INSTANCE, BoolColumnComparator.class);
  public static final PrimitiveColumnTypeInfo<BytesColumn> BYTES_COLUMN_TYPE_INFO = new PrimitiveColumnTypeInfo<>(BytesColumn.class, new Class<?>[] {},
      BytesColumnSerializer.INSTANCE, BytesColumnComparator.class);
  public static final PrimitiveColumnTypeInfo<LongColumn> LONG_COLUMN_TYPE_INFO = new PrimitiveColumnTypeInfo<>(LongColumn.class, new Class<?>[] {},
      LongColumnSerializer.INSTANCE, LongColumnComparator.class);
  public static final PrimitiveColumnTypeInfo<DateColumn> DATE_COLUMN_TYPE_INFO = new PrimitiveColumnTypeInfo<>(DateColumn.class, new Class<?>[] {},
      DateColumnSerializer.INSTANCE, DateColumnComparator.class);
  public static final PrimitiveColumnTypeInfo<DoubleColumn> DOUBLE_COLUMN_TYPE_INFO = new PrimitiveColumnTypeInfo<>(DoubleColumn.class, new Class<?>[] {},
      DoubleColumnSerializer.INSTANCE, DoubleColumnComparator.class);
  private static final long serialVersionUID = 1L;
  private static final Map<Class<?>, PrimitiveColumnTypeInfo<?>> TYPES = new HashMap<>();

  static {
    TYPES.put(StringColumn.class, STRING_COLUMN_TYPE_INFO);
    TYPES.put(BooleanColumn.class, BOOL_COLUMN_TYPE_INFO);
    TYPES.put(LongColumn.class, LONG_COLUMN_TYPE_INFO);
    TYPES.put(DoubleColumn.class, DOUBLE_COLUMN_TYPE_INFO);
    TYPES.put(DateColumn.class, DATE_COLUMN_TYPE_INFO);
    TYPES.put(BytesColumn.class, BYTES_COLUMN_TYPE_INFO);
  }

  private final Class<T> clazz;
  private final TypeSerializer<T> serializer;
  private final Class<?>[] possibleCastTargetTypes;
  private final Class<? extends TypeComparator<T>> comparatorClass;

  protected PrimitiveColumnTypeInfo(Class<T> clazz, Class<?>[] possibleCastTargetTypes, TypeSerializer<T> serializer,
                                    Class<? extends TypeComparator<T>> comparatorClass) {
    this.clazz = checkNotNull(clazz);
    this.possibleCastTargetTypes = checkNotNull(possibleCastTargetTypes);
    this.serializer = checkNotNull(serializer);
    // comparator can be null as in VOID_TYPE_INFO
    this.comparatorClass = comparatorClass;
  }

  private static <X> TypeComparator<X> instantiateComparator(Class<? extends TypeComparator<X>> comparatorClass, boolean ascendingOrder) {
    try {
      Constructor<? extends TypeComparator<X>> constructor = comparatorClass.getConstructor(boolean.class);
      return constructor.newInstance(ascendingOrder);
    } catch (Exception e) {
      throw new RuntimeException("Could not initialize basic comparator " + comparatorClass.getName(), e);
    }
  }

  @PublicEvolving
  public static <X> PrimitiveColumnTypeInfo<X> getInfoFor(Class<X> type) {
    if (type == null) {
      throw new NullPointerException();
    }

    @SuppressWarnings("unchecked")
    PrimitiveColumnTypeInfo<X> info = (PrimitiveColumnTypeInfo<X>) TYPES.get(type);
    return info;
  }

  /**
   * Returns whether this type should be automatically casted to
   * the target type in an arithmetic operation.
   */
  @PublicEvolving
  public boolean shouldAutocastTo(BasicTypeInfo<?> to) {
    for (Class<?> possibleTo : possibleCastTargetTypes) {
      if (possibleTo.equals(to.getTypeClass())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isBasicType() {
    return false;
  }

  @Override
  public boolean isTupleType() {
    return false;
  }

  @Override
  public int getArity() {
    return 1;
  }

  @Override
  public int getTotalFields() {
    return 1;
  }

  @Override
  public Class<T> getTypeClass() {
    return this.clazz;
  }

  @Override
  public boolean isKeyType() {
    return true;
  }

  @Override
  public TypeSerializer<T> createSerializer(ExecutionConfig config) {
    return this.serializer;
  }

  @Override
  @PublicEvolving
  public TypeComparator<T> createComparator(boolean sortOrderAscending, ExecutionConfig executionConfig) {
    if (comparatorClass != null) {
      return instantiateComparator(comparatorClass, sortOrderAscending);
    } else {
      throw new InvalidTypesException("The type " + clazz.getSimpleName() + " cannot be used as a key.");
    }
  }

  @Override
  public String toString() {
    return clazz.getSimpleName();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PrimitiveColumnTypeInfo) {
      @SuppressWarnings("unchecked")
      PrimitiveColumnTypeInfo<T> other = (PrimitiveColumnTypeInfo<T>) obj;

      return other.canEqual(this) &&
          this.clazz == other.clazz &&
          serializer.equals(other.serializer) &&
          this.comparatorClass == other.comparatorClass;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return (31 * Objects.hash(clazz, serializer, comparatorClass)) + Arrays.hashCode(possibleCastTargetTypes);
  }

  @Override
  public boolean canEqual(Object obj) {
    return obj instanceof PrimitiveColumnTypeInfo;
  }

}
