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
package org.apache.drill.exec.expr.fn;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;

/**
 * Represents a declared variable (parameter) in a Drill function.
 * This is a field declared with the <code>@Param</code> or
 * <code>@Output</code> tags.
 */
public class ValueReference {
  private final MajorType type;
  private final String name;
  private boolean isConstant;
  private boolean isFieldReader;
  private boolean isComplexWriter;
  private boolean isInternal;
  private boolean isVarArg;

  public ValueReference(MajorType type, String name) {
    Preconditions.checkNotNull(type);
    Preconditions.checkNotNull(name);
    this.type = type;
    this.name = name;
  }

  public void setConstant(boolean isConstant) {
    this.isConstant = isConstant;
  }

  public void setVarArg(boolean isVarArg) {
    this.isVarArg = isVarArg;
  }

  public MajorType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public boolean isConstant() {
    return isConstant;
  }

  public void setInternal(boolean isInternal) {
    this.isInternal = isInternal;
  }

  public boolean isInternal() {
    return isInternal;
  }

  public boolean isFieldReader() {
    return isFieldReader;
  }

  public boolean isComplexWriter() {
    return isComplexWriter;
  }

  public boolean isVarArg() {
    return isVarArg;
  }

  @Override
  public String toString() {
    return "ValueReference [type=" + Types.toString(type) + ", name=" + name + "]";
  }

  public static ValueReference createFieldReaderRef(String name) {
    MajorType type = Types.required(MinorType.LATE);
    ValueReference ref = new ValueReference(type, name);
    ref.isFieldReader = true;
    return ref;
  }

  public static ValueReference createComplexWriterRef(String name) {
    MajorType type = Types.required(MinorType.LATE);
    ValueReference ref = new ValueReference(type, name);
    ref.isComplexWriter = true;
    return ref;
  }
}