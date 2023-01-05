/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.runtime.pojo;

import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.solrdao.ISchemaField;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-12-14
 */
public class SchemaField implements ISchemaField {

  private String name;

  private String type;

  private String defaultVal;

  private boolean indexed;

  private boolean stored;

  private boolean required;

  private boolean docValue;

  private boolean multiValue;

  private boolean sharedKey;
  private boolean uniqueKey;

  @Override
  public boolean isSharedKey() {
    return this.sharedKey;
  }

  @Override
  public boolean isUniqueKey() {
    return this.uniqueKey;
  }

  @Override
  public boolean isMultiValue() {
    return this.multiValue;
  }

  @Override
  public boolean isDynamic() {
    return StringUtils.indexOf(this.name, "*") > -1;
  }

  public void setMultiValue(boolean multiValue) {
    this.multiValue = multiValue;
  }

  @Override
  public boolean equals(Object obj) {
    return StringUtils.equals(name, ((SchemaField) obj).name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isIndexed() {
    return indexed;
  }

  public void setIndexed(boolean indexed) {
    this.indexed = indexed;
  }

  public boolean isStored() {
    return stored;
  }

  public void setStored(boolean stored) {
    this.stored = stored;
  }

  @Override
  public String getTisFieldTypeName() {
    return this.type;
  }

  @Override
  public String getTokenizerType() {
    return null;
  }

  public void setDocValue(boolean docValue) {
    this.docValue = docValue;
  }

  @Override
  public boolean isDocValue() {
    return this.docValue;
  }

  @Override
  public String getDefaultValue() {
    return this.defaultVal;
  }

  @Override
  public void serialVisualType2Json(JSONObject f) {
    throw new UnsupportedOperationException();
  }

  public void setSharedKey(boolean sharedKey) {
    this.sharedKey = sharedKey;
  }

  public void setUniqueKey(boolean uniqueKey) {
    this.uniqueKey = uniqueKey;
  }
}
