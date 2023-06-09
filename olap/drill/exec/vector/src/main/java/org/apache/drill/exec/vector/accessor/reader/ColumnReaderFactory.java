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
package org.apache.drill.exec.vector.accessor.reader;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.vector.accessor.ColumnAccessorUtils;

/**
 * Gather generated reader classes into a set of class tables to allow rapid
 * run-time creation of readers. Builds the reader and its object reader
 * wrapper which binds the vector to the reader.
 */

@SuppressWarnings("unchecked")
public class ColumnReaderFactory {

  private static final int typeCount = MinorType.values().length;
  private static final Class<? extends BaseScalarReader> requiredReaders[] = new Class[typeCount];

  static {
    ColumnAccessorUtils.defineRequiredReaders(requiredReaders);
  }

  public static BaseScalarReader buildColumnReader(VectorAccessor va) {
    MajorType major = va.type();
    MinorType type = major.getMinorType();

    switch (type) {
    case GENERIC_OBJECT:
    case LATE:
    case NULL:
    case LIST:
    case MAP:
    case DICT:
      throw new UnsupportedOperationException(type.toString());
    default:
      return newAccessor(type, requiredReaders);
    }
  }

  public static <T> T newAccessor(MinorType type, Class<? extends T> accessors[]) {
    try {
      Class<? extends T> accessorClass = accessors[type.ordinal()];
      if (accessorClass == null) {
        throw new UnsupportedOperationException(type.toString());
      }
      return accessorClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}
