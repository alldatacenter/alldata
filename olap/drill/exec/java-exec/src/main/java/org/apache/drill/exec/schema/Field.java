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
package org.apache.drill.exec.schema;

import org.apache.drill.common.types.TypeProtos.MajorType;

import org.apache.drill.shaded.guava.com.google.common.base.MoreObjects;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;

import java.util.Objects;

public abstract class Field {
  final String prefixFieldName;
  MajorType fieldType;
  RecordSchema schema;
  RecordSchema parentSchema;
  boolean read;

  public Field(RecordSchema parentSchema, MajorType type, String prefixFieldName) {
    fieldType = type;
    this.prefixFieldName = prefixFieldName;
    this.parentSchema = parentSchema;
  }

  public abstract String getFieldName();

  public String getFullFieldName() {
    String fieldName = getFieldName();
    if(Strings.isNullOrEmpty(prefixFieldName)) {
      return fieldName;
    } else if(Strings.isNullOrEmpty(fieldName)) {
      return prefixFieldName;
    } else {
      return prefixFieldName + "." + getFieldName();
    }
  }

  public void setRead(boolean read) {
    this.read = read;
  }

  protected abstract MoreObjects.ToStringHelper addAttributesToHelper(MoreObjects.ToStringHelper helper);

  MoreObjects.ToStringHelper getAttributesStringHelper() {
    return MoreObjects.toStringHelper(this).add("type", fieldType)
        .add("fullFieldName", getFullFieldName())
        .add("schema", schema == null ? null : schema.toSchemaString()).omitNullValues();
  }

  @Override
  public String toString() {
    return addAttributesToHelper(getAttributesStringHelper()).toString();
  }

  public RecordSchema getAssignedSchema() {
    return schema;
  }

  public void assignSchemaIfNull(RecordSchema newSchema) {
    if (!hasSchema()) {
      schema = newSchema;
    }
  }

  public boolean isRead() {
    return read;
  }

  public boolean hasSchema() {
    return schema != null;
  }

  public MajorType getFieldType() {
    return fieldType;
  }

  public void setFieldType(MajorType fieldType) {
    this.fieldType = fieldType;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    } else if (that == null || getClass() != that.getClass()) {
      return false;
    }
    Field thatField = (Field)that;
    return Objects.equals(getFullFieldName(), thatField.getFullFieldName());
  }

  @Override
  public int hashCode() {
    return getFullFieldName().hashCode();
  }
}
