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

import org.apache.drill.exec.vector.complex.writer.FieldWriter;

<@pp.dropOutputFile />
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/impl/RepeatedDictWriter.java" />

<#include "/@includes/license.ftl" />
package org.apache.drill.exec.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
public class RepeatedDictWriter extends AbstractFieldWriter implements BaseWriter.DictWriter {

  final RepeatedDictVector container;

  private final SingleDictWriter dictWriter;
  private int currentChildIndex;

  public RepeatedDictWriter(RepeatedDictVector container, FieldWriter parent) {
    super(parent);
    this.container = Preconditions.checkNotNull(container, "Container cannot be null!");
    this.dictWriter = new SingleDictWriter((DictVector) container.getDataVector(), this);
  }

  @Override
  public void allocate() {
    container.allocateNew();
  }

  @Override
  public void clear() {
    container.clear();
  }

  @Override
  public void close() {
    clear();
    container.close();
  }

  @Override
  public int getValueCapacity() {
    return container.getValueCapacity();
  }

  public void setValueCount(int count){
    container.getMutator().setValueCount(count);
  }

  @Override
  public void startList() {
    // make sure that the current vector can support the end position of this list.
    if (container.getValueCapacity() <= idx()) {
      container.getMutator().setValueCount(idx() + 1);
    }

    // update the repeated vector to state that there is current+1 objects.
    final RepeatedDictHolder h = new RepeatedDictHolder();
    container.getAccessor().get(idx(), h);
    if (h.start >= h.end) {
      container.getMutator().startNewValue(idx());
    }
    currentChildIndex = container.getOffsetVector().getAccessor().get(idx());
  }

  @Override
  public void endList() {
    // noop, we initialize state at start rather than end.
  }

  @Override
  public MaterializedField getField() {
    return container.getField();
  }

  @Override
  public void start() {
    currentChildIndex = container.getMutator().add(idx());
    dictWriter.setPosition(currentChildIndex);
    dictWriter.start();
  }

  @Override
  public void end() {
    dictWriter.end();
  }

  @Override
  public void startKeyValuePair() {
    dictWriter.startKeyValuePair();
  }

  @Override
  public void endKeyValuePair() {
    dictWriter.endKeyValuePair();
  }

  @Override
  public ListWriter list(String name) {
    return dictWriter.list(name);
  }

  @Override
  public MapWriter map(String name) {
    return dictWriter.map(name);
  }

  @Override
  public DictWriter dict(String name) {
    return dictWriter.dict(name);
  }

  <#list vv.types as type>
    <#list type.minor as minor>
      <#assign lowerName = minor.class?uncap_first />
      <#if lowerName == "int" ><#assign lowerName = "integer" /></#if>

  @Override
  public ${minor.class}Writer ${lowerName}(String name) {
    return (FieldWriter) dictWriter.${lowerName}(name);
  }
      <#if minor.class?contains("Decimal") >

  @Override
  public ${minor.class}Writer ${lowerName}(String name, int scale, int precision) {
    return (FieldWriter) dictWriter.${lowerName}(name, scale, precision);
  }
      </#if>
    </#list>
  </#list>
}
