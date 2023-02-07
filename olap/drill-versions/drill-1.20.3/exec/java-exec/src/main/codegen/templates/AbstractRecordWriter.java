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
import java.lang.UnsupportedOperationException;

<@pp.dropOutputFile />
<@pp.changeOutputFile name="org/apache/drill/exec/store/AbstractRecordWriter.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.store;

import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.planner.physical.WriterPrel;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.vector.BitVector;
import org.apache.drill.exec.vector.BitVector.Accessor;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import java.io.IOException;
import java.lang.UnsupportedOperationException;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
public abstract class AbstractRecordWriter implements RecordWriter {

  private Accessor newPartitionVector;

  protected void setPartitionVector(BitVector newPartitionVector) {
    this.newPartitionVector = newPartitionVector.getAccessor();
  }

  protected boolean newPartition(int index) {
    return newPartitionVector.get(index) == 1;
  }

  public void checkForNewPartition(int index) {
    // no op
  }

  @Override
  public FieldConverter getNewMapConverter(int fieldId, String fieldName, FieldReader reader) {
    throw new UnsupportedOperationException("Doesn't support writing Map'");
  }

  @Override
  public FieldConverter getNewUnionConverter(int fieldId, String fieldName, FieldReader reader) {
    throw new UnsupportedOperationException("Doesn't support writing Union type'");
  }

  @Override
  public FieldConverter getNewRepeatedMapConverter(int fieldId, String fieldName, FieldReader reader) {
    throw new UnsupportedOperationException("Doesn't support writing RepeatedMap");
  }

  @Override
  public FieldConverter getNewRepeatedListConverter(int fieldId, String fieldName, FieldReader reader) {
    throw new UnsupportedOperationException("Doesn't support writing RepeatedList");
  }

  @Override
  public FieldConverter getNewDictConverter(int fieldId, String fieldName, FieldReader reader) {
    throw new UnsupportedOperationException("Doesn't support writing Dict");
  }

  @Override
  public FieldConverter getNewRepeatedDictConverter(int fieldId, String fieldName, FieldReader reader) {
    throw new UnsupportedOperationException("Doesn't support writing RepeatedDict");
  }

<#list vv.types as type>
  <#list type.minor as minor>
    <#list vv.modes as mode>
  @Override
  public FieldConverter getNew${mode.prefix}${minor.class}Converter(int fieldId, String fieldName, FieldReader reader) {
    throw new UnsupportedOperationException("Doesn't support writing '${mode.prefix}${minor.class}'");
  }
    </#list>
  </#list>
</#list>

  @Override
  public void postProcessing() throws IOException {
    // no op
  }

  @Override
  public boolean supportsField(MaterializedField field) {
    return !field.getName().equalsIgnoreCase(WriterPrel.PARTITION_COMPARATOR_FIELD);
  }
}
