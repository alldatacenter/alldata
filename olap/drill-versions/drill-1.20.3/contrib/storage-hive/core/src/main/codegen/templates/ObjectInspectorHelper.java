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
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/hive/ObjectInspectorHelper.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl.hive;

import com.sun.codemodel.*;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.DirectExpression;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.*;

import java.lang.UnsupportedOperationException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;

public class ObjectInspectorHelper {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ObjectInspectorHelper.class);

  private static Multimap<MinorType, Class> OIMAP_REQUIRED = ArrayListMultimap.create();
  private static Multimap<MinorType, Class> OIMAP_OPTIONAL = ArrayListMultimap.create();
  static {
<#assign entries = drillDataType.map + drillOI.map />
<#list entries as entry>
    <#if entry.needOIForDrillType == true>
    OIMAP_REQUIRED.put(MinorType.${entry.drillType?upper_case}, Drill${entry.drillType}${entry.hiveOI}.Required.class);
    OIMAP_OPTIONAL.put(MinorType.${entry.drillType?upper_case}, Drill${entry.drillType}${entry.hiveOI}.Optional.class);
    </#if>
</#list>
  }

  public static ObjectInspector getDrillObjectInspector(DataMode mode, MinorType minorType, boolean varCharToStringReplacement) {
    try {
      if (mode == DataMode.REQUIRED) {
        if (OIMAP_REQUIRED.containsKey(minorType)) {
          if (varCharToStringReplacement && minorType == MinorType.VARCHAR) {
            return (ObjectInspector) ((Class) OIMAP_REQUIRED.get(minorType).toArray()[1]).newInstance();
          } else {
            return (ObjectInspector) ((Class) OIMAP_REQUIRED.get(minorType).toArray()[0]).newInstance();
          }
        }
      } else if (mode == DataMode.OPTIONAL) {
        if (OIMAP_OPTIONAL.containsKey(minorType)) {
          if (varCharToStringReplacement && minorType == MinorType.VARCHAR) {
            return (ObjectInspector) ((Class) OIMAP_OPTIONAL.get(minorType).toArray()[1]).newInstance();
          } else {
            return (ObjectInspector) ((Class) OIMAP_OPTIONAL.get(minorType).toArray()[0]).newInstance();
          }
        }
      } else {
        throw new UnsupportedOperationException("Repeated types are not supported as arguement to Hive UDFs");
      }
    } catch(InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Failed to instantiate ObjectInspector", e);
    }

    throw new UnsupportedOperationException(
        String.format("Type %s[%s] not supported as arguement to Hive UDFs", minorType.toString(), mode.toString()));
  }

  public static JBlock initReturnValueHolder(ClassGenerator<?> g, JCodeModel m, JVar returnValueHolder, ObjectInspector oi, MinorType returnType) {
    JBlock block = new JBlock(false, false);
    switch(oi.getCategory()) {
      case PRIMITIVE: {
        PrimitiveObjectInspector poi = (PrimitiveObjectInspector)oi;
        switch(poi.getPrimitiveCategory()) {
<#assign entries = drillDataType.map + drillOI.map />
<#list entries as entry>
          case ${entry.hiveType}:{
            JType holderClass = TypeHelper.getHolderType(m, returnType, TypeProtos.DataMode.OPTIONAL);
            block.assign(returnValueHolder, JExpr._new(holderClass));

          <#if entry.hiveType == "VARCHAR" || entry.hiveType == "STRING" || entry.hiveType == "BINARY" || entry.hiveType == "CHAR">
            block.assign( //
                returnValueHolder.ref("buffer"), //
                g
                  .getMappingSet()
                  .getIncoming()
                  .invoke("getContext")
                  .invoke("getManagedBuffer")
                  .invoke("reallocIfNeeded")
                    .arg(JExpr.lit(1024))
            );
          </#if>
            return block;
          }
</#list>
          default:
            throw new UnsupportedOperationException(String.format("Received unknown/unsupported type '%s'", poi.getPrimitiveCategory().toString()));
        }
      }

      case MAP:
      case LIST:
      case STRUCT:
      default:
        throw new UnsupportedOperationException(String.format("Received unknown/unsupported type '%s'", oi.getCategory().toString()));
    }
  }

  private static Map<PrimitiveCategory, MinorType> TYPE_HIVE2DRILL = new HashMap<>();
  static {
<#assign entries = drillDataType.map + drillOI.map />
<#list entries as entry>
    TYPE_HIVE2DRILL.put(PrimitiveCategory.${entry.hiveType}, MinorType.${entry.drillType?upper_case});
</#list>
  }

  public static MinorType getDrillType(ObjectInspector oi) {
    switch(oi.getCategory()) {
      case PRIMITIVE: {
        PrimitiveObjectInspector poi = (PrimitiveObjectInspector)oi;
        if (TYPE_HIVE2DRILL.containsKey(poi.getPrimitiveCategory())) {
          return TYPE_HIVE2DRILL.get(poi.getPrimitiveCategory());
        }
        throw new UnsupportedOperationException();
      }

      case MAP:
      case LIST:
      case STRUCT:
      default:
        throw new UnsupportedOperationException();
    }
  }

  public static JBlock getDrillObject(JCodeModel m, ObjectInspector oi,
    JVar returnOI, JVar returnValueHolder, JVar returnValue) {
    JBlock block = new JBlock(false, false);
    switch(oi.getCategory()) {
      case PRIMITIVE: {
        PrimitiveObjectInspector poi = (PrimitiveObjectInspector)oi;
        switch(poi.getPrimitiveCategory()) {
<#assign entries = drillDataType.map + drillOI.map />
<#list entries as entry>
          case ${entry.hiveType}:{
            JConditional jc = block._if(returnValue.eq(JExpr._null()));
            jc._then().assign(returnValueHolder.ref("isSet"), JExpr.lit(0));
            jc._else().assign(returnValueHolder.ref("isSet"), JExpr.lit(1));
            JVar castedOI = jc._else().decl(
              m.directClass(${entry.hiveOI}.class.getCanonicalName()), "castOI", JExpr._null());
            jc._else().assign(castedOI,
              JExpr.cast(m.directClass(${entry.hiveOI}.class.getCanonicalName()), returnOI));

          <#if entry.hiveType == "BOOLEAN">
            JConditional booleanJC = jc._else()._if(castedOI.invoke("get").arg(returnValue));
            booleanJC._then().assign(returnValueHolder.ref("value"), JExpr.lit(1));
            booleanJC._else().assign(returnValueHolder.ref("value"), JExpr.lit(0));

          <#elseif entry.hiveType == "VARCHAR" || entry.hiveType == "CHAR" || entry.hiveType == "STRING" || entry.hiveType == "BINARY">
            <#if entry.hiveType == "VARCHAR">
              JVar data = jc._else().decl(m.directClass(byte[].class.getCanonicalName()), "data",
                  castedOI.invoke("getPrimitiveJavaObject").arg(returnValue)
                      .invoke("getValue")
                      .invoke("getBytes"));
            <#elseif entry.hiveType == "CHAR">
                JVar data = jc._else().decl(m.directClass(byte[].class.getCanonicalName()), "data",
                    castedOI.invoke("getPrimitiveJavaObject").arg(returnValue)
                        .invoke("getStrippedValue")
                        .invoke("getBytes"));
            <#elseif entry.hiveType == "STRING">
              JVar data = jc._else().decl(m.directClass(byte[].class.getCanonicalName()), "data",
                  castedOI.invoke("getPrimitiveJavaObject").arg(returnValue)
                      .invoke("getBytes"));
            <#elseif entry.hiveType == "BINARY">
                JVar data = jc._else().decl(m.directClass(byte[].class.getCanonicalName()), "data",
                    castedOI.invoke("getPrimitiveJavaObject").arg(returnValue));
            </#if>

            JConditional jnullif = jc._else()._if(data.eq(JExpr._null()));
            jnullif._then().assign(returnValueHolder.ref("isSet"), JExpr.lit(0));

            jnullif._else().add(returnValueHolder.ref("buffer")
                .invoke("setBytes").arg(JExpr.lit(0)).arg(data));
            jnullif._else().assign(returnValueHolder.ref("start"), JExpr.lit(0));
            jnullif._else().assign(returnValueHolder.ref("end"), data.ref("length"));
            jnullif._else().add(returnValueHolder.ref("buffer").invoke("setIndex").arg(JExpr.lit(0)).arg(data.ref("length")));

          <#elseif entry.hiveType == "TIMESTAMP">
            JVar tsVar = jc._else().decl(m.directClass(${entry.javaType}.class.getCanonicalName()), "ts",
              castedOI.invoke("getPrimitiveJavaObject").arg(returnValue));
              <#if entry.javaType == "org.apache.hadoop.hive.common.type.Timestamp">
            jc._else().assign(returnValueHolder.ref("value"), tsVar.invoke("toEpochMilli"));
              <#else>
            // Bringing relative timestamp value without timezone info to timestamp value in UTC, since Drill keeps date-time values in UTC
            JVar localDateTimeVar = jc._else().decl(m.directClass(org.joda.time.LocalDateTime.class.getCanonicalName()), "localDateTime",
                JExpr._new(m.directClass(org.joda.time.LocalDateTime.class.getCanonicalName())).arg(tsVar));
            jc._else().assign(returnValueHolder.ref("value"), localDateTimeVar.invoke("toDateTime")
                .arg(m.directClass(org.joda.time.DateTimeZone.class.getCanonicalName()).staticRef("UTC")).invoke("getMillis"));
              </#if>
          <#elseif entry.hiveType == "DATE">
            JVar dVar = jc._else().decl(m.directClass(${entry.javaType}.class.getCanonicalName()), "d",
              castedOI.invoke("getPrimitiveJavaObject").arg(returnValue));
              <#if entry.javaType == "org.apache.hadoop.hive.common.type.Date">
            jc._else().assign(returnValueHolder.ref("value"), dVar.invoke("toEpochMilli"));
              <#else>
            jc._else().assign(returnValueHolder.ref("value"), dVar.invoke("getTime"));
              </#if>
          <#else>
            jc._else().assign(returnValueHolder.ref("value"),
              castedOI.invoke("get").arg(returnValue));
          </#if>
            return block;
          }

</#list>
          default:
            throw new UnsupportedOperationException(String.format("Received unknown/unsupported type '%s'", poi.getPrimitiveCategory().toString()));
        }
      }

      case MAP:
      case LIST:
      case STRUCT:
      default:
        throw new UnsupportedOperationException(String.format("Received unknown/unsupported type '%s'", oi.getCategory().toString()));
    }
  }
}
