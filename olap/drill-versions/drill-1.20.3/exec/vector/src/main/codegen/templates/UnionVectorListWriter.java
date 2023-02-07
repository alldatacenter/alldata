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

import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.drill.exec.vector.complex.ListVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.exec.vector.complex.impl.UnionVectorWriter;
import org.apache.drill.exec.vector.complex.writer.FieldWriter;

<@pp.dropOutputFile />
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/impl/UnionVectorListWriter.java" />
<#include "/@includes/license.ftl" />
package org.apache.drill.exec.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Union vector writer for writing list of union-type values
 */
public class UnionVectorListWriter extends UnionVectorWriter {

  private final ListVector listVector;
  private final UInt4Vector offsets;
  private int listPosition;

  public UnionVectorListWriter(ListVector listVector, FieldWriter parent) {
    super(listVector.promoteToUnion(), parent);
    this.listVector = listVector;
    this.offsets = listVector.getOffsetVector();
  }

  // FACTORIES FOR COMPLEX LIST ELEMENT WRITERS

  public UnionVectorWriter union() {
    return this;
  }

  public MapWriter map() {
    return typeWriters.computeIfAbsent(MinorType.MAP, type -> new SingleMapUnionListElementWriter(dataVector.getMap(), null, false));
  }

  public DictWriter dict() {
    return typeWriters.computeIfAbsent(MinorType.DICT, type -> new SingleDictUnionListElementWriter(dataVector.getDict(), null, false));
  }

  public ListWriter list() {
    return typeWriters.computeIfAbsent(MinorType.LIST, type -> new UnionListUnionElementWriter(dataVector.getList()));
  }

  // FACTORIES FOR PRIMITIVE LIST ELEMENT WRITERS
<#list vv.types as type>
  <#list type.minor as minor>
    <#assign lowerName = minor.class?uncap_first />
    <#assign upperName = minor.class?upper_case />
    <#assign capName = minor.class?cap_first />
    <#if lowerName == "int" >
      <#assign lowerName = "integer" />
    </#if>
    <#if !minor.class?starts_with("Decimal")>

  @Override
  public ${capName}Writer ${lowerName}() {
    return typeWriters.computeIfAbsent(MinorType.${upperName}, ${capName}UnionListElementWriter::new);
  }
      <#if minor.class == "VarDecimal">

  @Override
  public ${capName}Writer ${lowerName}(int precision, int scale) {
    return typeWriters.computeIfAbsent(MinorType.${upperName}, type -> new ${capName}UnionListElementWriter(type, precision, scale));
  }
      </#if>
    </#if>
  </#list>
</#list>

  // WRITER's METHODS

  /**
   * Superclass's idx() returns index of element inside list row. So the method uses
   * additional field {@link #listPosition} for storing index of list row for {@link #listVector}.
   *
   * @param index of list in list vector
   */
  @Override
  public void setPosition(int index) {
    this.listPosition = index;
    int dataPosition = offsets.getAccessor().get(listPosition);
    super.setPosition(dataPosition);
  }

  @Override
  public void allocate() {
    listVector.allocateNew();
  }

  @Override
  public void clear() {
    listVector.clear();
  }

  @Override
  public int getValueCapacity() {
    return listVector.getValueCapacity();
  }

  @Override
  public MaterializedField getField() {
    return listVector.getField();
  }

  @Override
  public void close() throws Exception {
    listVector.close();
  }

  private void setNextOffset() {
    final int nextOffset = offsets.getAccessor().get(listPosition + 1);
    listVector.getMutator().setNotNull(listPosition);
    super.setPosition(nextOffset);
  }

  private void increaseOffset() {
    offsets.getMutator().setSafe(listPosition + 1, idx() + 1);
  }

// TYPE SPECIFIC LIST ELEMENTS INNER WRITERS
<#list vv.types as type>
  <#list type.minor as minor>
    <#assign name = minor.class?cap_first />
    <#assign fields = minor.fields!type.fields />
    <#assign uncappedName = name?uncap_first/>
    <#if !minor.class?starts_with("Decimal")>

  private class ${name}UnionListElementWriter extends UnionVectorWriter.${name}UnionWriter {

    private ${name}UnionListElementWriter(MinorType type) {
      super(type);
    }
    <#if minor.class == "VarDecimal">

    private ${name}UnionListElementWriter(MinorType type, int precision, int scale) {
      super(type, precision, scale);
    }

    @Override
    public void write${minor.class}(BigDecimal value) {
      setNextOffset();
      super.write${minor.class}(value);
      increaseOffset();
    }
    </#if>

    @Override
    public void write${minor.class}(<#list fields as field>${field.type} ${field.name}<#if field_has_next>, </#if></#list>) {
      setNextOffset();
      super.write${name}(<#list fields as field>${field.name}<#if field_has_next>, </#if></#list>);
      increaseOffset();
    }
  }
    </#if>
  </#list>
</#list>

  private class UnionListUnionElementWriter extends UnionVectorWriter.ListUnionWriter {

    UnionListUnionElementWriter(ListVector vector) {
      super(vector);
    }

    @Override
    public void startList() {
      setNextOffset();
      super.startList();
      increaseOffset();
    }
  }
<#list ["Map", "Dict"] as capFirstName>

  class Single${capFirstName}UnionListElementWriter extends UnionVectorWriter.Single${capFirstName}UnionWriter {

    Single${capFirstName}UnionListElementWriter(${capFirstName}Vector container, FieldWriter parent, boolean unionEnabled) {
      super(container, parent, unionEnabled);
    }

    @Override
    public void start() {
      setNextOffset();
      super.start();
      increaseOffset();
    }
  }
</#list>
}
