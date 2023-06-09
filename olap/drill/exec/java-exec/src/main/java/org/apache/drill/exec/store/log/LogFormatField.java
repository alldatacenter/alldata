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

package org.apache.drill.exec.store.log;

import java.util.Objects;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;


/**
 * The three configuration options for a field are:
 * <ol>
 * <li>The field name</li>
 * <li>The data type (fieldType). Field type defaults to VARCHAR
 * if it is not specified</li>
 * <li>The format string which is used for date/time fields.
 * This field is ignored if used with a non date/time field.</li>
 * </ol>
 */
@JsonTypeName("regexReaderFieldDescription")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class LogFormatField {

  private final String fieldName;
  private final String fieldType;
  private final String format;

  // Required to keep Jackson happy
  public LogFormatField() {
    this("");
  }

  @VisibleForTesting
  public LogFormatField(String fieldName) {
    this(fieldName, "VARCHAR", null);
  }

  public LogFormatField(String fieldName, String fieldType) {
    this(fieldName, fieldType, null);
  }

  public LogFormatField(String fieldName, String fieldType, String format) {
    this.fieldName = fieldName;
    this.fieldType = fieldType;
    this.format = format;
  }

  public String getFieldName() { return fieldName; }

  public String getFieldType() { return fieldType; }

  public String getFormat() { return format; }

  @Override
  public boolean equals(Object o) {
    if (o == null || ! (o instanceof LogFormatField)) {
      return false;
    }
    LogFormatField other = (LogFormatField) o;
    return fieldName.equals(other.fieldName) &&
           Objects.equals(fieldType, other.fieldType) &&
           Objects.equals(format, other.format);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldName, fieldType, format);
  }
}
