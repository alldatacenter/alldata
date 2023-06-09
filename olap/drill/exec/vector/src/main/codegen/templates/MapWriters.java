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
<#list ["Single", "Repeated"] as mode>

<#if mode == "Repeated">
<#assign className = "AbstractRepeatedMapWriter">
<#else>
<#assign className = "${mode}MapWriter">
</#if>

<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/impl/${className}.java" />
<#if mode == "Single">
<#assign containerClass = "MapVector" />
<#assign index = "idx()">
<#else>
<#assign containerClass = "T" />
<#assign index = "currentChildIndex">
</#if>

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />
import java.util.Map;
import java.util.HashMap;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.exec.expr.holders.RepeatedMapHolder;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.FieldWriter;

/*
 * This class is generated using FreeMarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
<#if mode == "Repeated">
public abstract class ${className}<${containerClass} extends AbstractRepeatedMapVector> extends AbstractFieldWriter {
<#else>
public class ${className} extends AbstractFieldWriter {
</#if>

  protected final ${containerClass} container;
  <#if mode == "Repeated">protected<#else>private</#if> final Map<String, FieldWriter> fields = new HashMap<>();
  <#if mode == "Repeated">protected int currentChildIndex;</#if>

  private final boolean unionEnabled;

  public ${className}(${containerClass} container, FieldWriter parent, boolean unionEnabled) {
    super(parent);
    this.container = container;
    this.unionEnabled = unionEnabled;
  }

  public ${className}(${containerClass} container, FieldWriter parent) {
    this(container, parent, false);
  }

  @Override
  public int getValueCapacity() {
    return container.getValueCapacity();
  }

  @Override
  public boolean isEmptyMap() {
    return container.size() == 0;
  }

  @Override
  public MaterializedField getField() {
      return container.getField();
  }

  @Override
  public MapWriter map(String name) {
    FieldWriter writer = fields.get(name.toLowerCase());
    if (writer == null) {
      int vectorCount = container.size();
        MapVector vector = container.addOrGet(name, MapVector.TYPE, MapVector.class);
      if (!unionEnabled) {
        writer = new SingleMapWriter(vector, this);
      } else {
        writer = new PromotableWriter(vector, container);
      }
      if (vectorCount != container.size()) {
        writer.allocate();
      }
      writer.setPosition(${index});
      fields.put(name.toLowerCase(), writer);
    }
    return writer;
  }

  @Override
  public UnionVectorWriter union(String name) {
    FieldWriter writer = fields.get(name.toLowerCase());
    if (writer == null) {
      int vectorCount = container.size();
      UnionVector vector = container.addOrGet(name, Types.optional(MinorType.UNION), UnionVector.class);
      writer = new UnionVectorWriter(vector, this);
      if (vectorCount != container.size()) {
        writer.allocate();
      }
      writer.setPosition(${index});
      fields.put(name.toLowerCase(), writer);
    }
    return (UnionVectorWriter) writer;
  }

  @Override
  public DictWriter dict(String name) {
    FieldWriter writer = fields.get(name.toLowerCase());
    if (writer == null) {
      int vectorCount = container.size();

      DictVector vector = container.addOrGet(name, DictVector.TYPE, DictVector.class);
      writer = new SingleDictWriter(vector, this);

      fields.put(name.toLowerCase(), writer);
      if (vectorCount != container.size()) {
        writer.allocate();
      }
      writer.setPosition(${index});
    }
    return writer;
  }

  @Override
  public void close() throws Exception {
    clear();
    container.close();
  }

  @Override
  public void allocate() {
    container.allocateNew();
    for(final FieldWriter w : fields.values()) {
      w.allocate();
    }
  }

  @Override
  public void clear() {
    container.clear();
    for(final FieldWriter w : fields.values()) {
      w.clear();
    }
  }

  @Override
  public ListWriter list(String name) {
    FieldWriter writer = fields.get(name.toLowerCase());
    int vectorCount = container.size();
    if (writer == null) {
      if (!unionEnabled) {
        writer = new SingleListWriter(name, container, this);
      } else{
        writer = new PromotableWriter(container.addOrGet(name, Types.optional(MinorType.LIST), ListVector.class), container);
      }
      if (container.size() > vectorCount) {
        writer.allocate();
      }
      writer.setPosition(${index});
      fields.put(name.toLowerCase(), writer);
    }
    return writer;
  }
  <#if mode != "Repeated">

  public void setValueCount(int count) {
    container.getMutator().setValueCount(count);
  }

  @Override
  public void setPosition(int index) {
    super.setPosition(index);
    for(final FieldWriter w: fields.values()) {
      w.setPosition(index);
    }
  }

  @Override
  public void start() {
  }

  @Override
  public void end() {
  }
  </#if>

  <#list vv.types as type><#list type.minor as minor>
  <#assign lowerName = minor.class?uncap_first />
  <#if lowerName == "int" ><#assign lowerName = "integer" /></#if>
  <#assign upperName = minor.class?upper_case />
  <#assign capName = minor.class?cap_first />
  <#assign vectName = capName />
  <#assign vectName = "Nullable${capName}" />

  <#if minor.class?contains("Decimal") >
  @Override
  public ${minor.class}Writer ${lowerName}(String name) {
    // returns existing writer
    final FieldWriter writer = fields.get(name.toLowerCase());
    assert writer != null;
    return writer;
  }

  @Override
  public ${minor.class}Writer ${lowerName}(String name, int precision, int scale) {
    final MajorType ${upperName}_TYPE = Types.withPrecisionAndScale(MinorType.${upperName}, DataMode.OPTIONAL, precision, scale);
  <#else>
  private static final MajorType ${upperName}_TYPE = Types.optional(MinorType.${upperName});
  @Override
  public ${minor.class}Writer ${lowerName}(String name) {
  </#if>
    FieldWriter writer = fields.get(name.toLowerCase());
    if (writer == null) {
      ValueVector vector;
      ValueVector currentVector = container.getChild(name);
      if (unionEnabled) {
        ${vectName}Vector v = container.addOrGet(name, ${upperName}_TYPE, ${vectName}Vector.class);
        writer = new PromotableWriter(v, container);
        vector = v;
      } else {
        ${vectName}Vector v = container.addOrGet(name, ${upperName}_TYPE, ${vectName}Vector.class);
        writer = new ${vectName}WriterImpl(v, this);
        vector = v;
      }
      if (currentVector == null || currentVector != vector) {
        vector.allocateNewSafe();
      } 
      writer.setPosition(${index});
      fields.put(name.toLowerCase(), writer);
    }
    return writer;
  }

  </#list></#list>
}
</#list>
