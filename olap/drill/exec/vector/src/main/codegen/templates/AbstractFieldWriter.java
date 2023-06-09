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
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/impl/AbstractFieldWriter.java" />


<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
abstract class AbstractFieldWriter extends AbstractBaseWriter implements FieldWriter {
  AbstractFieldWriter(FieldWriter parent) {
    super(parent);
  }

  @Override
  public void start() {
    throw new IllegalStateException(String.format("You tried to start when you are using a ValueWriter of type %s.", this.getClass().getSimpleName()));
  }

  @Override
  public void end() {
    throw new IllegalStateException(String.format("You tried to end when you are using a ValueWriter of type %s.", this.getClass().getSimpleName()));
  }

  @Override
  public void startList() {
    throw new IllegalStateException(String.format("You tried to start when you are using a ValueWriter of type %s.", this.getClass().getSimpleName()));
  }

  @Override
  public void endList() {
    throw new IllegalStateException(String.format("You tried to end when you are using a ValueWriter of type %s.", this.getClass().getSimpleName()));
  }

  <#list vv.types as type><#list type.minor as minor><#assign name = minor.class?cap_first />
  <#assign fields = minor.fields!type.fields />
  @Override
  public void write(${name}Holder holder) {
    fail("${name}");
  }

  public void write${minor.class}(<#list fields as field>${field.type} ${field.name}<#if field_has_next>, </#if></#list>) {
    fail("${name}");
  }

  <#if minor.class?contains("Decimal") >
  public void write${minor.class}(BigDecimal value) {
    fail("${name}");
  }
  </#if>

  </#list></#list>

  public void writeNull() {
    fail("${name}");
  }

  /**
   * This implementation returns {@code false}.
   * <p>  
   *   Must be overridden by map writers.
   * </p>  
   */
  @Override
  public boolean isEmptyMap() {
    return false;
  }

  @Override
  public MapWriter map() {
    fail("Map");
    return null;
  }

  @Override
  public DictWriter dict() {
    fail("Dict");
    return null;
  }

  @Override
  public ListWriter list() {
    fail("List");
    return null;
  }

  @Override
  public MapWriter map(String name) {
    fail("Map");
    return null;
  }

  @Override
  public DictWriter dict(String name) {
    fail("Dict");
    return null;
  }

  @Override
  public FieldWriter getKeyWriter() {
    fail("KeyWriter");
    return null;
  }

  @Override
  public FieldWriter getValueWriter() {
    fail("ValueWriter");
    return null;
  }

  @Override
  public void startKeyValuePair() {
    fail("startKeyValuePair()");
  }

  @Override
  public void endKeyValuePair() {
    fail("endKeyValuePair()");
  }

  @Override
  public ListWriter list(String name) {
    fail("List");
    return null;
  }

  @Override
  public UnionVectorWriter union(String name) {
    fail("Union");
    return null;
  }

  @Override
  public UnionVectorWriter union() {
    fail("Union");
    return null;
  }

  <#list vv.types as type><#list type.minor as minor>
  <#assign lowerName = minor.class?uncap_first />
  <#if lowerName == "int" ><#assign lowerName = "integer" /></#if>
  <#assign upperName = minor.class?upper_case />
  <#assign capName = minor.class?cap_first />
  <#if minor.class?contains("Decimal") >
  @Override
  public ${capName}Writer ${lowerName}(String name, int precision, int scale) {
    fail("${capName}");
    return null;
  }

  @Override
  public ${capName}Writer ${lowerName}(int precision, int scale) {
    fail("${capName}");
    return null;
  }
  </#if>

  @Override
  public ${capName}Writer ${lowerName}(String name) {
    fail("${capName}");
    return null;
  }

  @Override
  public ${capName}Writer ${lowerName}() {
    fail("${capName}");
    return null;
  }

  </#list></#list>

  public void copyReader(FieldReader reader) {
    fail("Copy FieldReader");
  }

  public void copyReaderToField(String name, FieldReader reader) {
    fail("Copy FieldReader to STring");
  }

  private void fail(String name) {
    throw new IllegalArgumentException(String.format("You tried to write a %s type when you are using a ValueWriter of type %s.", name, this.getClass().getSimpleName()));
  }
}
