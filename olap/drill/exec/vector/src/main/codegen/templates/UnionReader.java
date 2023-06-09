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
import org.apache.drill.common.types.Types;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.vector.complex.UnionVector;
import org.apache.drill.exec.vector.complex.impl.NullReader;

<@pp.dropOutputFile />
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/impl/UnionReader.java" />


<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
public class UnionReader extends AbstractFieldReader {

  public final UnionVector data;
  private final BaseReader[] readers = new BaseReader[UnionVector.TYPE_COUNT];

  public UnionReader(UnionVector data) {
    this.data = data;
  }

  @Override
  public MajorType getType() {
    return UnionVector.TYPES[data.getTypeValue(idx())];
  }

  public MinorType getVectorType() {
    return data.getField().getType().getMinorType();
  }

  public boolean isSet(){
    return !data.getAccessor().isNull(idx());
  }

  public void read(UnionHolder holder) {
    holder.reader = this;
    holder.isSet = this.isSet() ? 1 : 0;
  }

  public void read(int index, UnionHolder holder) {
    getList().read(index, holder);
  }

  private FieldReader getReaderForIndex(int index) {
    int typeValue = data.getTypeValue(index);
    if (typeValue == UnionVector.NULL_MARKER) {
      return NullReader.INSTANCE;
    }
    FieldReader reader = (FieldReader) readers[typeValue];
    if (reader != null) {
      return reader;
    }
    // Warning: do not use valueOf as that uses Protobuf
    // field numbers, not enum ordinals.
    MinorType type = MinorType.values()[typeValue];
    switch (type) {
    case MAP:
      return (FieldReader) getMap();
    case DICT:
      return (FieldReader) getDict();
    case LIST:
      return (FieldReader) getList();
    <#list vv.types as type><#list type.minor as minor><#assign name = minor.class?cap_first />
    <#assign uncappedName = name?uncap_first/>
    <#if !minor.class?starts_with("Decimal")>
    case ${name?upper_case}:
      return (FieldReader) get${name}();
    </#if>
    </#list></#list>
    default:
      throw new UnsupportedOperationException("Unsupported type: " + MinorType.valueOf(typeValue));
    }
  }

  private SingleMapReaderImpl mapReader;

  private MapReader getMap() {
    if (mapReader == null) {
      mapReader = (SingleMapReaderImpl) data.getMap().getReader();
      mapReader.setPosition(idx());
      readers[MinorType.MAP.ordinal()] = mapReader;
    }
    return mapReader;
  }

  private SingleDictReaderImpl dictReader;

  private DictReader getDict() {
    if (dictReader == null) {
      dictReader = (SingleDictReaderImpl) data.getDict().getReader();
      dictReader.setPosition(idx());
      readers[MinorType.DICT.ordinal()] = dictReader;
    }
    return dictReader;
  }

  private UnionListReader listReader;

  private FieldReader getList() {
    if (listReader == null) {
      listReader = new UnionListReader(data.getList());
      listReader.setPosition(idx());
      readers[MinorType.LIST.ordinal()] = listReader;
    }
    return listReader;
  }

  @Override
  public java.util.Iterator<String> iterator() {
    return getMap().iterator();
  }

  @Override
  public void copyAsValue(UnionWriter writer) {
    writer.data.copyFrom(idx(), writer.idx(), data);
  }

  <#list ["Object", "BigDecimal", "Integer", "Long", "Boolean",
          "Character", "LocalDate", "LocalTime", "LocalDateTime", "Period", "Double", "Float",
          "Text", "String", "Byte", "Short", "byte[]"] as friendlyType>
  <#assign safeType=friendlyType />
  <#if safeType=="byte[]"><#assign safeType="ByteArray" /></#if>

  @Override
  public ${friendlyType} read${safeType}() {
    return getReaderForIndex(idx()).read${safeType}();
  }
  </#list>
  <#list vv.types as type><#list type.minor as minor><#assign name = minor.class?cap_first />
          <#assign uncappedName = name?uncap_first/>
  <#assign boxedType = (minor.boxedType!type.boxedType) />
  <#assign javaType = (minor.javaType!type.javaType) />
  <#assign friendlyType = (minor.friendlyType!minor.boxedType!type.boxedType) />
  <#assign safeType=friendlyType />
  <#if safeType=="byte[]"><#assign safeType="ByteArray" /></#if>
  <#if !minor.class?starts_with("Decimal")>

  private Nullable${name}ReaderImpl ${uncappedName}Reader;

  private Nullable${name}ReaderImpl get${name}() {
    if (${uncappedName}Reader == null) {
      ${uncappedName}Reader = new Nullable${name}ReaderImpl(data.get${name}Vector());
      ${uncappedName}Reader.setPosition(idx());
      readers[MinorType.${name?upper_case}.ordinal()] = ${uncappedName}Reader;
    }
    return ${uncappedName}Reader;
  }

  public void read(Nullable${name}Holder holder){
    getReaderForIndex(idx()).read(holder);
  }

  public void copyAsValue(${name}Writer writer){
    getReaderForIndex(idx()).copyAsValue(writer);
  }
  </#if>
  </#list></#list>

  @Override
  public void copyAsValue(ListWriter writer) {
    ComplexCopier.copy(this, (FieldWriter) writer);
  }

  @Override
  public void setPosition(int index) {
    super.setPosition(index);
    for (BaseReader reader : readers) {
      if (reader != null) {
        reader.setPosition(index);
      }
    }
  }

  public FieldReader reader(String name){
    return getMap().reader(name);
  }

  public FieldReader reader() {
    return getList().reader();
  }

  public boolean next() {
    return getReaderForIndex(idx()).next();
  }

  @Override
  public String getTypeString() {
    if (isSet()) {
      return getType().getMinorType().name();
    } else {
      return MinorType.NULL.name();
    }
  }
}
