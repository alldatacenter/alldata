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
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/impl/UnionVectorWriter.java" />
<#include "/@includes/license.ftl" />
package org.apache.drill.exec.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ListWriter-like writer with only difference that it acts only as a factory
 * for concrete type writers for UnionVector data vector.
 */
public class UnionVectorWriter extends AbstractFieldWriter {

  /**
   * Map holding lazily initialized type specific writers for the union data vector
   */
  final Map<MinorType, FieldWriter> typeWriters = new TypeWritersMap();

  /**
   * Data vector here used as a producer for type specific vectors
   * which will be used by type writers for storing concrete values.
   */
  final UnionVector dataVector;

  /**
  * Constructs writer with dataVector of UnionVector type.
  *
  * @param vector union data vector
  * @param parent parent writer
  */
  public UnionVectorWriter(UnionVector vector, FieldWriter parent) {
    super(parent);
    dataVector = vector;
  }

  // FACTORIES FOR COMPLEX TYPE WRITERS

  @Override
  public UnionVectorWriter union() {
    return this;
  }

  @Override
  public MapWriter map() {
    return typeWriters.computeIfAbsent(MinorType.MAP, type -> new SingleMapUnionWriter(dataVector.getMap(), null, false));
  }

  @Override
  public DictWriter dict() {
    return typeWriters.computeIfAbsent(MinorType.DICT, type -> new SingleDictUnionWriter(dataVector.getDict(), null, false));
  }

  @Override
  public ListWriter list() {
    return typeWriters.computeIfAbsent(MinorType.LIST, listType -> new ListUnionWriter(dataVector.getList()));
  }

  // FACTORIES FOR PRIMITIVE TYPE WRITERS
<#list vv.types as type>
  <#list type.minor as minor>
    <#assign lowerName = minor.class?uncap_first />
    <#assign upperName = minor.class?upper_case />
    <#assign capName = minor.class?cap_first />
    <#if lowerName == "int" >
      <#assign lowerName = "integer" />
    </#if>
    <#if !minor.class?starts_with("Decimal")>

  /**
   * Get concrete writer for writing ${upperName?lower_case} data to {@link #dataVector}.
   *
   * @return ${upperName?lower_case} writer
   */
  @Override
  public ${capName}Writer ${lowerName}() {
    return typeWriters.computeIfAbsent(MinorType.${upperName}, ${capName}UnionWriter::new);
  }
      <#if minor.class == "VarDecimal">

  @Override
  public ${capName}Writer ${lowerName}(int precision, int scale) {
    return typeWriters.computeIfAbsent(MinorType.${upperName}, type -> new ${capName}UnionWriter(type, precision, scale));
  }
      </#if>
    </#if>
  </#list>
</#list>

  // WRITER's METHODS
  @Override
  public void allocate() {
    dataVector.allocateNew();
  }

  @Override
  public void clear() {
    dataVector.clear();
  }

  @Override
  public int getValueCapacity() {
    return dataVector.getValueCapacity();
  }

  @Override
  public MaterializedField getField() {
    return dataVector.getField();
  }

  @Override
  public void close() throws Exception {
    dataVector.close();
  }

  @Override
  public void writeNull() {
    dataVector.getMutator().setNull(UnionVectorWriter.this.idx());
  }

  private void setTypeAndIndex(MinorType type, Positionable positionable) {
    dataVector.getMutator().setType(UnionVectorWriter.this.idx(), type);
    positionable.setPosition(UnionVectorWriter.this.idx());
  }

// TYPE SPECIFIC INNER WRITERS
<#list vv.types as type>
  <#list type.minor as minor>
    <#assign name = minor.class?cap_first />
    <#assign fields = minor.fields!type.fields />
    <#assign uncappedName = name?uncap_first/>
    <#if !minor.class?starts_with("Decimal")>

  class ${name}UnionWriter extends Nullable${name}WriterImpl {
    private final MinorType type;

    ${name}UnionWriter(MinorType type) {
      super(dataVector.get${name}Vector(), null);
      this.type = type;
    }
    <#if minor.class == "VarDecimal">

    ${name}UnionWriter(MinorType type, int precision, int scale) {
      this(type);
      MaterializedField field = super.vector.getField();
      MajorType typeWithPrecisionAndScale = field.getType().toBuilder()
          .setPrecision(precision).setScale(scale).build();
      field.replaceType(typeWithPrecisionAndScale);
    }

    @Override
    public void write${minor.class}(BigDecimal value) {
      setTypeAndIndex(type, this);
      super.write${minor.class}(value);
      dataVector.getMutator().setValueCount(idx() + 1);
    }
    </#if>

    @Override
    public void write${minor.class}(<#list fields as field>${field.type} ${field.name}<#if field_has_next>, </#if></#list>) {
      setTypeAndIndex(type, this);
      super.write${name}(<#list fields as field>${field.name}<#if field_has_next>, </#if></#list>);
      dataVector.getMutator().setValueCount(idx() + 1);
    }

    @Override
    public void write(${name}Holder holder) {
      write${minor.class}(<#list fields as field>holder.${field.name}<#if field_has_next>, </#if></#list>);
    }
  }
    </#if>
  </#list>
</#list>

  class ListUnionWriter extends UnionListWriter {

    ListUnionWriter(ListVector vector) {
      super(vector);
    }

    @Override
    public void startList() {
      dataVector.getMutator().setType(UnionVectorWriter.this.idx(), MinorType.LIST);
      super.startList();
      dataVector.getMutator().setValueCount(idx() + 1);
    }

    /*
     Overridden methods here are used to initialize early $data$ field to avoid schema change exception
     when transfer pair called to transfer from empty list to list with initialized $data$ vector.
     For example, without the fix exception was thrown on attempt to transfer
        FROM: [`list` (LIST:OPTIONAL), children=([`[DEFAULT]` (LATE:OPTIONAL)])]
        TO:   [`list` (LIST:OPTIONAL), children=([`[DEFAULT]` (LATE:OPTIONAL)], [`$data$` (VARCHAR:OPTIONAL)])]
     */
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
      writer.getWriter(MinorType.${upperName});
      return super.${lowerName}();
    }
    </#if>
  </#list>
</#list>
  }
<#list ["Map", "Dict"] as capFirstName>

  class Single${capFirstName}UnionWriter extends Single${capFirstName}Writer {

    Single${capFirstName}UnionWriter(${capFirstName}Vector container, FieldWriter parent, boolean unionEnabled) {
      super(container, parent, unionEnabled);
    }

    @Override
    public void start() {
      dataVector.getMutator().setType(UnionVectorWriter.this.idx(), MinorType.${capFirstName?upper_case});
      super.start();
      dataVector.getMutator().setValueCount(idx() + 1);
    }
  }
</#list>

  // CONTAINER FOR ALL TYPE-SPECIFIC WRITERS
  private class TypeWritersMap extends EnumMap<MinorType, FieldWriter> {
    TypeWritersMap() {
      super(MinorType.class);
    }

    @Override
    public FieldWriter computeIfAbsent(MinorType key, Function<? super MinorType, ? extends FieldWriter> mappingFunction) {
      FieldWriter fw = get(key);
      if (fw == null) {
        put(key, (fw = mappingFunction.apply(key)));
      }
      // fixes copying in MapUtility for case when column has type STRUCT<f:UNIONTYPE<...>>
      setTypeAndIndex(key, fw);
      return fw;
    }
  }
}
