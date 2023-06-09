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
<@pp.changeOutputFile name="org/apache/drill/exec/store/RecordWriter.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.store;

import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.Map;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

/** RecordWriter interface. */
public interface RecordWriter {

  /**
   * Initialize the writer.
   *
   * @param writerOptions Contains key, value pair of settings.
   * @throws IOException
   */
  void init(Map<String, String> writerOptions) throws IOException;

  /**
   * Update the schema in RecordWriter. Called at least once before starting writing the records.
   * @param batch
   * @throws IOException
   */
  void updateSchema(VectorAccessible batch) throws IOException;

  /**
   * Check if the writer should start a new partition, and if so, start a new partition
   */
  public void checkForNewPartition(int index);

  /**
   * Called before starting writing fields in a record.
   * @throws IOException
   */
  void startRecord() throws IOException;

  /** Add the field value given in <code>valueHolder</code> at the given column number <code>fieldId</code>. */
  public FieldConverter getNewMapConverter(int fieldId, String fieldName, FieldReader reader);
  public FieldConverter getNewUnionConverter(int fieldId, String fieldName, FieldReader reader);
  public FieldConverter getNewRepeatedMapConverter(int fieldId, String fieldName, FieldReader reader);
  public FieldConverter getNewRepeatedListConverter(int fieldId, String fieldName, FieldReader reader);
  public FieldConverter getNewDictConverter(int fieldId, String fieldName, FieldReader reader);
  public FieldConverter getNewRepeatedDictConverter(int fieldId, String fieldName, FieldReader reader);

<#list vv.types as type>
  <#list type.minor as minor>
    <#list vv.modes as mode>
  /** Add the field value given in <code>valueHolder</code> at the given column number <code>fieldId</code>. */
  public FieldConverter getNew${mode.prefix}${minor.class}Converter(int fieldId, String fieldName, FieldReader reader);

    </#list>
  </#list>
</#list>

  /**
   * Called after adding all fields in a particular record are added using add{TypeHolder}(fieldId, TypeHolder) methods.
   * @throws IOException
   */
  void endRecord() throws IOException;
  /**
   * Called after adding all the records to perform any post processing related tasks
   * @throws IOException
   */
  void postProcessing() throws IOException;
  void abort() throws IOException;
  void cleanup() throws IOException;

  /**
   * Checks whether this writer supports writing of the given field.
   */
  boolean supportsField(MaterializedField field);
}
