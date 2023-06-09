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
<@pp.changeOutputFile name="org/apache/drill/exec/record/RecordValueAccessor.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.record;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.vector.*;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

/** Wrapper around VectorAccessible to iterate over the records and fetch fields within a record. */
public class RecordValueAccessor {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RecordValueAccessor.class);

  private VectorAccessible batch;
  private int currentIndex;
  private ValueVector[] vectors;

  public RecordValueAccessor(VectorAccessible batch) {
    this.batch = batch;

    resetIterator();
    initVectorWrappers();
  }

  public void resetIterator() {
    currentIndex = -1;
  }

  private void initVectorWrappers() {
    BatchSchema schema = batch.getSchema();
    vectors = new ValueVector[schema.getFieldCount()];
    int fieldId = 0;
    for (MaterializedField field : schema) {
      Class<?> vectorClass = TypeHelper.getValueVectorClass(field.getType().getMinorType(), field.getType().getMode());
      vectors[fieldId] = batch.getValueAccessorById(vectorClass, fieldId).getValueVector();
      fieldId++;
    }
  }

  public boolean next() {
    return ++currentIndex < batch.getRecordCount();
  }

  public void getFieldById(int fieldId, ComplexHolder holder) {
    holder.isSet = vectors[fieldId].getAccessor().isNull(currentIndex) ? 1 : 0;
    holder.reader = (vectors[fieldId]).getReader();
    holder.reader.setPosition(currentIndex);
  }

<#list vv.types as type>
  <#list type.minor as minor>
    <#list vv.modes as mode>
  public void getFieldById(int fieldId, ${mode.prefix}${minor.class}Holder holder){
    ((${mode.prefix}${minor.class}Vector) vectors[fieldId]).getAccessor().get(currentIndex, holder);
  }

    </#list>
  </#list>
</#list>
}
