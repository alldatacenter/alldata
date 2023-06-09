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
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/impl/ComplexCopier.java" />


<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />
import org.apache.drill.common.types.TypeProtos;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
public class ComplexCopier {

  /**
   * Do a deep copy of the value in input into output
   * @param in
   * @param out
   */
  public static void copy(FieldReader input, FieldWriter output) {
    writeValue(input, output);
  }

  private static void writeValue(FieldReader reader, FieldWriter writer) {
    final DataMode m = reader.getType().getMode();
    final MinorType mt = reader.getType().getMinorType();

    switch(m){
    case OPTIONAL:
    case REQUIRED:


      switch (mt) {

      case LIST:
        writer.startList();
        while (reader.next()) {
          writeValue(reader.reader(), getListWriterForReader(reader.reader(), writer));
        }
        writer.endList();
        break;
      case MAP:
        writer.start();
        if (reader.isSet()) {
          for(String name : reader){
            FieldReader childReader = reader.reader(name);
            if(childReader.isSet()){
              writeValue(childReader, getMapWriterForReader(childReader, writer, name));
            }
          }
        }
        writer.end();
        break;
        case DICT:
          DictWriter wr = (DictWriter) writer;
          wr.start();
          if (reader.isSet()) {
            while (reader.next()) {
              wr.startKeyValuePair();
              FieldReader keyReader = reader.reader(DictVector.FIELD_KEY_NAME);
              FieldReader valueReader = reader.reader(DictVector.FIELD_VALUE_NAME);
              writeValue(keyReader, getMapWriterForReader(keyReader, writer, DictVector.FIELD_KEY_NAME));
              writeValue(valueReader, getMapWriterForReader(valueReader, writer, DictVector.FIELD_VALUE_NAME));
              wr.endKeyValuePair();
            }
          }
          wr.end();
          break;
  <#list vv.types as type><#list type.minor as minor><#assign name = minor.class?cap_first />
  <#assign fields = minor.fields!type.fields />
  <#assign uncappedName = name?uncap_first/>
  <#if !minor.class?starts_with("Decimal")>

      case ${name?upper_case}:
        if (reader.isSet()) {
          Nullable${name}Holder ${uncappedName}Holder = new Nullable${name}Holder();
          reader.read(${uncappedName}Holder);
          if (${uncappedName}Holder.isSet == 1) {
            writer.write${name}(<#list fields as field>${uncappedName}Holder.${field.name}<#if field_has_next>, </#if></#list>);
          }
        }
        break;

  </#if>
  </#list></#list>
      }
              break;
    }
  }

  public static FieldWriter getMapWriterForType(TypeProtos.MajorType type, MapWriter writer, String name) {
    switch (type.getMinorType()) {
    <#list vv.types as type><#list type.minor as minor><#assign name = minor.class?cap_first />
    <#assign fields = minor.fields!type.fields />
    <#assign uncappedName = name?uncap_first/>
    <#if !minor.class?contains("Decimal")>
    case ${name?upper_case}:
      return (FieldWriter) writer.<#if name == "Int">integer<#else>${uncappedName}</#if>(name);
    <#elseif minor.class?contains("VarDecimal")>
    case ${name?upper_case}:
      return (FieldWriter) writer.${uncappedName}(name, type.getPrecision(), type.getScale());
    </#if>
    </#list></#list>
    case MAP:
      return (FieldWriter) writer.map(name);
    case DICT:
      return (FieldWriter) writer.dict(name);
    case LIST:
      return (FieldWriter) writer.list(name);
    default:
      throw new UnsupportedOperationException(String.format("[%s] type is not supported.", type.toString()));
    }
  }

  public static FieldWriter getListWriterForType(TypeProtos.MajorType type, ListWriter writer) {
    switch (type.getMinorType()) {
    <#list vv.types as type><#list type.minor as minor><#assign name = minor.class?cap_first />
    <#assign fields = minor.fields!type.fields />
    <#assign uncappedName = name?uncap_first/>
    <#if !minor.class?contains("Decimal")>
    case ${name?upper_case}:
      return (FieldWriter) writer.<#if name == "Int">integer<#else>${uncappedName}</#if>();
    <#elseif minor.class?contains("VarDecimal")>
    case ${name?upper_case}:
      return (FieldWriter) writer.${uncappedName}(type.getPrecision(), type.getScale());
    </#if>
    </#list></#list>
    case MAP:
      return (FieldWriter) writer.map();
    case LIST:
      return (FieldWriter) writer.list();
    default:
      throw new UnsupportedOperationException(String.format("[%s] type is not supported.", type.toString()));
    }
  }

  private static FieldWriter getMapWriterForReader(FieldReader reader, BaseWriter.MapWriter writer, String name) {
    return getMapWriterForType(reader.getType(), writer, name);
  }

  private static FieldWriter getListWriterForReader(FieldReader reader, ListWriter writer) {
    return getListWriterForType(reader.getType(), writer);
  }
}
