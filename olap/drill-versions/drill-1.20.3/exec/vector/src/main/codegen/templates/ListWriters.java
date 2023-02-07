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
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/impl/${mode}ListWriter.java" />


<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector.complex.impl;
<#if mode == "Single">
  <#assign containerClass = "AbstractContainerVector" />
  <#assign index = "idx()">
<#else>
  <#assign containerClass = "RepeatedListVector" />
  <#assign index = "currentChildIndex">
</#if>


<#include "/@includes/vv_imports.ftl" />

/*
 * This class is generated using FreeMarker and the ${.template_name} template.
 */
public class ${mode}ListWriter extends AbstractFieldWriter {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(${mode}ListWriter.class);

  enum Mode {
    INIT, IN_MAP, IN_LIST, IN_DICT, IN_UNION
    <#list vv.types as type><#list type.minor as minor>,
    IN_${minor.class?upper_case}</#list></#list> }

  private final String name;
  protected final ${containerClass} container;
  private Mode mode = Mode.INIT;
  private FieldWriter writer;
  protected RepeatedValueVector innerVector;

  <#if mode == "Repeated">private int currentChildIndex = 0;</#if>
  public ${mode}ListWriter(String name, ${containerClass} container, FieldWriter parent){
    super(parent);
    this.name = name;
    this.container = container;
  }

  public ${mode}ListWriter(${containerClass} container, FieldWriter parent){
    super(parent);
    this.name = null;
    this.container = container;
  }

  @Override
  public void allocate() {
    if(writer != null) {
      writer.allocate();
    }
    <#if mode == "Repeated">
    container.allocateNew();
    </#if>
  }

  @Override
  public void clear() {
    if (writer != null) {
      writer.clear();
    }
  }

  @Override
  public void close() {
    clear();
    container.close();
    if (innerVector != null) {
      innerVector.close();
    }
  }

  @Override
  public int getValueCapacity() {
    return innerVector == null ? 0 : innerVector.getValueCapacity();
  }

  public void setValueCount(int count){
    if (innerVector != null) {
      innerVector.getMutator().setValueCount(count);
    }
  }

  @Override
  public MapWriter map() {
    switch (mode) {
    case INIT:
      final ValueVector oldVector = container.getChild(name);
      final RepeatedMapVector vector = container.addOrGet(name, RepeatedMapVector.TYPE, RepeatedMapVector.class);
      innerVector = vector;
      writer = new RepeatedMapWriter(vector, this);
      // oldVector will be null if it's first batch being created and it might not be same as newly added vector
      // if new batch has schema change
      if (oldVector == null || oldVector != vector) {
        writer.allocate();
      }
      writer.setPosition(${index});
      mode = Mode.IN_MAP;
      return writer;
    case IN_MAP:
      return writer;
    default:
      throw UserException
        .unsupportedError()
        .message(getUnsupportedErrorMsg("MAP", mode.name()))
        .build(logger);
    }
  }

  @Override
  public DictWriter dict() {
    switch (mode) {
    case INIT:
      final ValueVector oldVector = container.getChild(name);
      final RepeatedDictVector vector = container.addOrGet(name, RepeatedDictVector.TYPE, RepeatedDictVector.class);
      innerVector = vector;
      writer = new RepeatedDictWriter(vector, this);
      // oldVector will be null if it's first batch being created and it might not be same as newly added vector
      // if new batch has schema change
      if (oldVector == null || oldVector != vector) {
        writer.allocate();
      }
      writer.setPosition(${index});
      mode = Mode.IN_DICT;
      return writer;
    case IN_DICT:
      return writer;
    default:
      throw UserException.unsupportedError()
        .message(getUnsupportedErrorMsg("DICT", mode.name()))
        .build(logger);
    }
  }

  @Override
  public ListWriter list() {
    switch (mode) {
    case INIT:
      final ValueVector oldVector = container.getChild(name);
      final RepeatedListVector vector = container.addOrGet(name, RepeatedListVector.TYPE, RepeatedListVector.class);
      innerVector = vector;
      writer = new RepeatedListWriter(null, vector, this);
      // oldVector will be null if it's first batch being created and it might not be same as newly added vector
      // if new batch has schema change
      if (oldVector == null || oldVector != vector) {
        writer.allocate();
      }
      writer.setPosition(${index});
      mode = Mode.IN_LIST;
      return writer;
    case IN_LIST:
      return writer;
    default:
      throw UserException
        .unsupportedError()
        .message(getUnsupportedErrorMsg("LIST", mode.name()))
        .build(logger);
    }
  }

  @Override
  public UnionVectorWriter union() {
    switch (mode) {
      case INIT:
        final ValueVector oldVector = container.getChild(name);
        final ListVector vector = container.addOrGet(name, Types.optional(MinorType.LIST), ListVector.class);
        innerVector = vector;

        writer = new UnionVectorListWriter(vector, this);

        // oldVector will be null if it's first batch being created and it might not be same as newly added vector
        // if new batch has schema change
        if (oldVector == null || oldVector != vector) {
          writer.allocate();
        }
        writer.setPosition(idx());
        mode = Mode.IN_UNION;
        return (UnionVectorWriter) writer;
      case IN_UNION:
        return (UnionVectorWriter) writer;
      default:
        throw UserException.unsupportedError()
              .message(getUnsupportedErrorMsg("UNION", mode.name()))
              .build(logger);
    }
  }

  <#list vv.types as type><#list type.minor as minor>
  <#assign lowerName = minor.class?uncap_first />
  <#assign upperName = minor.class?upper_case />
  <#assign capName = minor.class?cap_first />
  <#if lowerName == "int" ><#assign lowerName = "integer" /></#if>

  <#if minor.class?contains("Decimal") >
  @Override
  public ${capName}Writer ${lowerName}() {
    // returns existing writer
    assert mode == Mode.IN_${upperName};
    return writer;
  }

  @Override
  public ${capName}Writer ${lowerName}(int precision, int scale) {
    final MajorType ${upperName}_TYPE = Types.withPrecisionAndScale(MinorType.${upperName}, DataMode.REPEATED, precision, scale);
  <#else>
  private static final MajorType ${upperName}_TYPE = Types.repeated(MinorType.${upperName});

  @Override
  public ${capName}Writer ${lowerName}() {
  </#if>
    switch (mode) {
    case INIT:
      final ValueVector oldVector = container.getChild(name);
      final Repeated${capName}Vector vector = container.addOrGet(name, ${upperName}_TYPE, Repeated${capName}Vector.class);
      innerVector = vector;
      writer = new Repeated${capName}WriterImpl(vector, this);
      // oldVector will be null if it's first batch being created and it might not be same as newly added vector
      // if new batch has schema change
      if (oldVector == null || oldVector != vector) {
        writer.allocate();
      }
      writer.setPosition(${index});
      mode = Mode.IN_${upperName};
      return writer;
    case IN_${upperName}:
      return writer;
    default:
      throw UserException
         .unsupportedError()
         .message(getUnsupportedErrorMsg("${upperName}", mode.name()))
         .build(logger);
    }
  }

  </#list></#list>
  @Override
  public MaterializedField getField() {
    return container.getField();
  }
  <#if mode == "Repeated">

  @Override
  public void startList() {
    final RepeatedListVector list = (RepeatedListVector) container;
    final RepeatedListVector.RepeatedMutator mutator = list.getMutator();

    // make sure that the current vector can support the end position of this list.
    if(container.getValueCapacity() <= idx()) {
      mutator.setValueCount(idx()+1);
    }

    // update the repeated vector to state that there is current+1 objects.
    final RepeatedListHolder h = new RepeatedListHolder();
    list.getAccessor().get(idx(), h);
    if (h.start >= h.end) {
      mutator.startNewValue(idx());
    }
    currentChildIndex = container.getMutator().add(idx());
    if(writer != null) {
      writer.setPosition(currentChildIndex);
      if (mode == Mode.IN_DICT) {
        writer.startList();
      }
    }
  }

  @Override
  public void endList() {
    // noop, we initialize state at start rather than end.
  }
  <#else>

  @Override
  public void setPosition(int index) {
    super.setPosition(index);
    if(writer != null) {
      writer.setPosition(index);
    }
  }

  @Override
  public void startList() {
    switch (mode) {
      case IN_DICT:
        writer.startList();
        break;
      case IN_UNION:
        innerVector.getMutator().startNewValue(idx());
        break;
    }
  }

  @Override
  public void endList() {
    if (mode == Mode.IN_DICT) {
      writer.endList();
    }
  }
  </#if>

  private String getUnsupportedErrorMsg(String expected, String found) {
    final String f = found.substring(3);
    return String.format("In a list of type %s, encountered a value of type %s. "+
      "Drill does not support lists of different types.",
       f, expected
    );
  }
}
</#list>
