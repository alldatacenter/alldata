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
package org.apache.drill.common.scanner.persistence;

import static java.lang.String.format;

import org.apache.drill.common.exceptions.DrillRuntimeException;

/**
 * represents the type of a field
 */
public abstract class TypeDescriptor {

  private final boolean primitive;
  private final int arrayDim;

  TypeDescriptor(boolean primitive, int arrayDim) {
    this.primitive = primitive;
    this.arrayDim = arrayDim;
  }

  public boolean isPrimitive() {
    return primitive;
  }

  public boolean isArray() {
    return arrayDim > 0;
  }

  /**
   * @return the number of dimensions of the array or 0 if it's not an array
   */
  public int getArrayDim() {
    return arrayDim;
  }

  /**
   * @return the type (element type if an array)
   */
  public abstract Class<?> getType();

  private static final class PrimitiveTypeDescriptor extends TypeDescriptor {
    private final Class<?> type;

    private static Class<?> getPrimitiveType(char descriptor) {
      switch (descriptor) {
      case 'V': return void.class;
      case 'I': return int.class;
      case 'B': return byte.class;
      case 'J': return long.class;
      case 'D': return double.class;
      case 'F': return float.class;
      case 'C': return char.class;
      case 'S': return short.class;
      case 'Z': return boolean.class;
      default: throw new DrillRuntimeException("bad descriptor: " + descriptor);
      }
    }

    private PrimitiveTypeDescriptor(char typeDescriptor, int arrayDepth) {
      super(true, arrayDepth);
      this.type = getPrimitiveType(typeDescriptor);
    }

    @Override
    public Class<?> getType() {
      return this.type;
    }
  }

  private static final class ClassTypeDescriptor extends TypeDescriptor {
    private String className;

    private ClassTypeDescriptor(String className, int arrayDim) {
      super(false, arrayDim);
      this.className = className;
    }

    @Override
    public Class<?> getType() {
      try {
        return Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new DrillRuntimeException(
            format(
                "Unexpected class load failure while attempting to load Function Registry (%s)",
                className),
            e);
      }
    }
  }

  public static TypeDescriptor forClass(String className, int arrayDim) {
    return new ClassTypeDescriptor(className, arrayDim);
  }

  public static TypeDescriptor forPrimitive(char c, int arrayDim) {
    return new PrimitiveTypeDescriptor(c, arrayDim);
  }
}