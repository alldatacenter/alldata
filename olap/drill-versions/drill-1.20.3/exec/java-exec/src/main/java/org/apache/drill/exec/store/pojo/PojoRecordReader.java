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
package org.apache.drill.exec.store.pojo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonTypeName;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.physical.impl.OutputMutator;

/**
 * Reads values from the given list of pojo instances.
 * Fields writers are determined based on pojo field class types.
 *
 * @param <T> pojo class type
 */
@JsonTypeName("PojoRecordReader")
public class PojoRecordReader<T> extends AbstractPojoRecordReader<T> {

  private final Class<T> pojoClass;
  private final List<Field> fields;

  public PojoRecordReader(Class<T> pojoClass, List<T> records) {
    super(records);
    this.pojoClass = pojoClass;
    this.fields = new ArrayList<>();
  }

  public PojoRecordReader(Class<T> pojoClass, List<T> records, int maxRecordToRead) {
    super(records, maxRecordToRead);
    this.pojoClass = pojoClass;
    this.fields = new ArrayList<>();
  }

  /**
   * Creates writers based on pojo field class types. Ignores static fields.
   *
   * @param output output mutator
   * @return list of pojo writers
   */
  @Override
  protected List<PojoWriter> setupWriters(OutputMutator output) throws ExecutionSetupException {
    List<PojoWriter> writers = new ArrayList<>();
    Field[] declaredFields = pojoClass.getDeclaredFields();
    for (Field field : declaredFields) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      writers.add(initWriter(field.getType(), field.getName(), output));
      fields.add(field);
    }
    return writers;
  }

  @Override
  protected Object getFieldValue(T row, int fieldPosition) {
    try {
      return fields.get(fieldPosition).get(row);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new DrillRuntimeException("Failure while trying to use PojoRecordReader.", e);
    }
  }

  @Override
  public String toString() {
    return "PojoRecordReader{" +
        "pojoClass = " + pojoClass +
        ", recordCount = " + records.size() +
        "}";
  }
}
