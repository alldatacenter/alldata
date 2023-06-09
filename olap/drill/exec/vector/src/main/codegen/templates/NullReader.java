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
<@pp.dropOutputFile />
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/impl/NullReader.java" />


<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
public class NullReader extends AbstractBaseReader implements FieldReader {

  public static final NullReader INSTANCE = new NullReader();
  public static final NullReader EMPTY_LIST_INSTANCE = new NullReader(Types.repeated(TypeProtos.MinorType.NULL));
  public static final NullReader EMPTY_MAP_INSTANCE = new NullReader(Types.required(TypeProtos.MinorType.MAP));
  private MajorType type;

  private NullReader() {
    type = Types.NULL;
  }

  private NullReader(MajorType type) {
    this.type = type;
  }

  @Override
  public MajorType getType() {
    return type;
  }

  public void read(ValueHolder holder) {
    throw new UnsupportedOperationException("NullReader cannot write into non-nullable holder");
  }

  public void copyAsValue(MapWriter writer) {}

  public void copyAsValue(ListWriter writer) {}

  public void copyAsValue(UnionWriter writer) {}

  <#list vv.types as type><#list type.minor as minor><#assign name = minor.class?cap_first />
  public void read(${name}Holder holder) {
    throw new UnsupportedOperationException("NullReader cannot write into non-nullable holder");
  }

  public void read(Nullable${name}Holder holder) {
    holder.isSet = 0;
  }

  public void read(int arrayIndex, ${name}Holder holder) {
    throw new ArrayIndexOutOfBoundsException();
  }

  public void copyAsValue(${minor.class}Writer writer) {}
  <#if minor.class == "VarDecimal">
  public void copyAsField(String name, ${minor.class}Writer writer, int precision, int scale) {}
  <#else>
  public void copyAsField(String name, ${minor.class}Writer writer) {}
  </#if>

  public void read(int arrayIndex, Nullable${name}Holder holder) {
    throw new ArrayIndexOutOfBoundsException();
  }
  </#list></#list>

  public int size() {
    return 0;
  }

  public boolean isSet() {
    return false;
  }

  public boolean next() {
    return false;
  }

  public RepeatedMapReader map() {
    return this;
  }

  public RepeatedListReader list() {
    return this;
  }

  public MapReader map(String name) {
    return this;
  }

  public ListReader list(String name) {
    return this;
  }

  public FieldReader reader(String name) {
    return this;
  }

  public FieldReader reader() {
    return this;
  }

  private void fail(String name) {
    throw new IllegalArgumentException(String.format("You tried to read a %s type when you are using a ValueReader of type %s.", name, this.getClass().getSimpleName()));
  }

  <#list ["Object", "BigDecimal", "Integer", "Long", "Boolean",
          "Character", "LocalDate", "LocalTime", "LocalDateTime", "Period", "Double", "Float",
          "Text", "String", "Byte", "Short", "byte[]"] as friendlyType>
  <#assign safeType=friendlyType />
  <#if safeType=="byte[]"><#assign safeType="ByteArray" /></#if>

  public ${friendlyType} read${safeType}(int arrayIndex) {
    return null;
  }

  public ${friendlyType} read${safeType}() {
    return null;
  }
  </#list>

  @Override
  public void copyAsValue(DictWriter writer) {}

  @Override
  public int find(String key) {
    return -1;
  }

  @Override
  public int find(int key) {
    return -1;
  }

  @Override
  public int find(Object key) {
    return -1;
  }

  @Override
  public void read(String key, ValueHolder holder) {
  }

  @Override
  public void read(int key, ValueHolder holder) {
  }

  @Override
  public void read(Object key, ValueHolder holder) {
  }
}



