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
package com.qlangtech.tis.runtime.module.misc;

import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.solrdao.ISchemaField;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-12-14
 */
public class SchemaField implements ISchemaField {

  // 字段编号（頁面操作會話過程中不可變）
  private int id;

  // 是否是主键
  private boolean uniqueKey;

  // 是否是分组键
  private boolean sharedKey;

  private String name;

  private String fieldtype;

  private boolean indexed = false;

  private boolean stored = true;

  private boolean required;

  private boolean multiValue = false;

  // 如果选择了string分词
  private String textAnalysis;

  // 当选择了String类型之后，可以选择String类型的分词类型
  private boolean split;

  // 是否开通docvalue
  private boolean docval;
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Override
  public boolean isMultiValue() {
    return this.multiValue;
  }

  @Override
  public boolean isDynamic() {
    return StringUtils.indexOf(this.name, "*") > -1;
  }

  @Override
  public String getTisFieldTypeName() {
    return this.fieldtype;
  }

  @Override
  public boolean isSharedKey() {
    return sharedKey;
  }

  public void setSharedKey(boolean sharedKey) {
    this.sharedKey = sharedKey;
  }

  @Override
  public boolean isUniqueKey() {
    return uniqueKey;
  }

  public void setUniqueKey(boolean uniqueKey) {
    this.uniqueKey = uniqueKey;
  }

  @Override
  public boolean isDocValue() {
    return this.docval;
  }

  public void setDocval(boolean docval) {
    this.docval = docval;
  }

  private String defaultValue;

  public boolean isSplit() {
    return split;
  }

  public void setSplit(boolean split) {
    this.split = split;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  @Override
  public void serialVisualType2Json(JSONObject f) {
    throw new UnsupportedOperationException();
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void setTokenizerType(String value) {
    this.textAnalysis = value;
  }

  public String getTokenizerType() {
    return this.textAnalysis;
  }

  public Boolean getSortable() {
    return this.isDocValue();
  }

  @Override
  public boolean equals(Object obj) {
    return StringUtils.equals(name, ((SchemaField) obj).name);
  }

  public String getPropertyName() {
    StringBuffer result = new StringBuffer();
    boolean isLetterGap = false;
    char[] nameChar = this.name.toCharArray();
    for (int i = 0; i < nameChar.length; i++) {
      if (isLetterGap) {
        result.append(Character.toUpperCase(nameChar[i]));
        isLetterGap = ('_' == nameChar[i]);
        continue;
      }
      if (isLetterGap = ('_' == nameChar[i])) {
        continue;
      }
      // if (isLetterGap) {
      // result.append(Character.toUpperCase(this.name.charAt(i)));
      // } else {
      result.append(nameChar[i]);
      // }
    }
    return result.toString();
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFieldtype() {
    return this.getTisFieldTypeName();
  }

  public void setFieldtype(String fieldtype) {
    this.fieldtype = fieldtype;
  }

  public boolean isIndexed() {
    return indexed;
  }

  public void setIndexed(boolean indexed) {
    this.indexed = indexed;
  }

  public boolean getMultiValue() {
    return this.multiValue;
  }

  public void setMultiValue(boolean multiValue) {
    this.multiValue = multiValue;
  }

  public boolean isStored() {
    return stored;
  }

  public void setStored(Boolean stored) {
    this.stored = stored;
  }

  @Override
  public String toString() {
    return "{" +
      "name='" + name + '\'' +
      ", fieldtype='" + fieldtype + '\'' +
      ", indexed=" + indexed +
      ", stored=" + stored +
      ", textAnalysis='" + textAnalysis + '\'' +
      ", docval=" + docval +
      '}';
  }
}
