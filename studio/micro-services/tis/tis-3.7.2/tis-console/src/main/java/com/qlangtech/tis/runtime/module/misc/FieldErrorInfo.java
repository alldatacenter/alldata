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

/**
 * Schema编辑页面小白模式下页面校验
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年5月11日
 */
public class FieldErrorInfo {

  private final int id;
  // 包括： 字段为空，字段重复，字段名称正则式
  private boolean fieldNameError;
  // 字段重复了
//  private boolean duplicateError;
//  private boolean namePatternError;

  private boolean fieldTypeError;

  // 至少设置一个，可查询，存储，可排序的属性
  private boolean fieldPropRequiredError;

  public FieldErrorInfo(int id) {
    this.id = id;
  }

  public boolean isFieldPropRequiredError() {
    return fieldPropRequiredError;
  }

  public void setFieldPropRequiredError(boolean fieldPropRequiredError) {
    this.fieldPropRequiredError = fieldPropRequiredError;
  }

  public boolean isFieldNameError() {
    return fieldNameError;
  }

  public void setFieldNameError(boolean fieldNameError) {
    this.fieldNameError = fieldNameError;
  }


  public int getId() {
    return this.id;
  }


//  public boolean isDuplicateError() {
//    return duplicateError;
//  }
//
//  public boolean isNamePatternError() {
//    return namePatternError;
//  }

//  public void setNamePatternError(boolean namePatternError) {
//    this.namePatternError = namePatternError;
//  }
//
//  public void setDuplicateError(boolean duplicateError) {
//    this.duplicateError = duplicateError;
//  }

  public boolean isFieldTypeError() {
    return fieldTypeError;
  }

  public void setFieldTypeError(boolean fieldTypeError) {
    this.fieldTypeError = fieldTypeError;
  }

  // public boolean isFieldInputBlank() {
//    return fieldInputBlank;
//  }

//  public void setFieldInputBlank(boolean fieldInputBlank) {
//    this.fieldInputBlank = fieldInputBlank;
//  }
}
