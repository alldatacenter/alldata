/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.dashboard.model;

import org.apache.iceberg.PartitionField;
import org.apache.iceberg.Schema;


public class AMSPartitionField {
  String field;
  String sourceField;
  String transform;
  Integer fieldId;
  Integer sourceFieldId;

  public AMSPartitionField() {
  }

  public AMSPartitionField(String field, String sourceField, String transform, Integer fieldId, Integer sourceFieldId) {
    this.field = field;
    this.sourceField = sourceField;
    this.transform = transform;
    this.fieldId = fieldId;
    this.sourceFieldId = sourceFieldId;
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public String getSourceField() {
    return sourceField;
  }

  public void setSourceField(String sourceField) {
    this.sourceField = sourceField;
  }

  public String getTransform() {
    return transform;
  }

  public void setTransform(String transform) {
    this.transform = transform;
  }

  public Integer getFieldId() {
    return fieldId;
  }

  public void setFieldId(Integer fieldId) {
    this.fieldId = fieldId;
  }

  public Integer getSourceFieldId() {
    return sourceFieldId;
  }

  public void setSourceFieldId(Integer sourceFieldId) {
    this.sourceFieldId = sourceFieldId;
  }

  public static AMSPartitionField buildFromPartitionSpec(Schema schema, PartitionField pf) {
    return new Builder()
            .field(pf.name())
            .sourceField(schema.findColumnName(pf.sourceId()))
            .transform(pf.transform().toString())
            .fieldId(pf.fieldId())
            .sourceFieldId(pf.sourceId())
            .build();
  }

  public static class Builder {
    String field;
    String sourceField;
    String transform;
    Integer fieldId;
    Integer sourceFieldId;

    public Builder field(String field) {
      this.field = field;
      return this;
    }

    public Builder sourceField(String sourceField) {
      this.sourceField = sourceField;
      return this;
    }

    public Builder transform(String transform) {
      this.transform = transform;
      return this;
    }

    public Builder fieldId(Integer fieldId) {
      this.fieldId = fieldId;
      return this;
    }

    public Builder sourceFieldId(Integer sourceFieldId) {
      this.sourceFieldId = sourceFieldId;
      return this;
    }

    public AMSPartitionField build() {
      return new AMSPartitionField(field, sourceField, transform, fieldId, sourceFieldId);
    }
  }
}