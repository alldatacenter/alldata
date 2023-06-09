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
import org.apache.drill.exec.schema.json.jackson.JacksonHelper;

import org.apache.drill.shaded.guava.com.google.common.base.MoreObjects;

public class NamedField extends Field {
  final MajorType keyType;
  String fieldName;

  public NamedField(RecordSchema parentSchema, String prefixFieldName, String fieldName, MajorType fieldType) {
    this(parentSchema, prefixFieldName, fieldName, fieldType, JacksonHelper.STRING_TYPE);
  }

  public NamedField(RecordSchema parentSchema,
                    String prefixFieldName,
                    String fieldName,
                    MajorType fieldType,
                    MajorType keyType) {
    super(parentSchema, fieldType, prefixFieldName);
    this.fieldName = fieldName;
    this.keyType = keyType;
  }

  @Override
  public String getFieldName() {
    return fieldName;
  }

  @Override
  protected MoreObjects.ToStringHelper addAttributesToHelper(MoreObjects.ToStringHelper helper) {
    return helper.add("keyType", keyType);
  }
}
