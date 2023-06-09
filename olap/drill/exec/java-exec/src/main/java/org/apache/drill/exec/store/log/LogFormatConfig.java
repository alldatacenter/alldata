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

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.shaded.guava.com.google.common.base.Objects;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(LogFormatPlugin.PLUGIN_NAME)
public class LogFormatConfig implements FormatPluginConfig {

  private final String regex;
  private final String extension;
  private final int maxErrors;
  private final List<LogFormatField> schema;

  @JsonCreator
  public LogFormatConfig(
      @JsonProperty("regex") String regex,
      @JsonProperty("extension") String extension,
      @JsonProperty("maxErrors") Integer maxErrors,
      @JsonProperty("schema") List<LogFormatField> schema) {
    this.regex = regex;
    this.extension = extension;
    this.maxErrors = maxErrors == null ? 10 : maxErrors;
    this.schema = schema == null
        ? ImmutableList.of() : schema;
  }

  public String getRegex() {
    return regex;
  }

  public String getExtension() {
    return extension;
  }

  public int getMaxErrors() {
    return maxErrors;
  }

  public List<LogFormatField> getSchema() {
    return schema;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    LogFormatConfig other = (LogFormatConfig) obj;
    return Objects.equal(regex, other.regex) &&
           Objects.equal(maxErrors, other.maxErrors) &&
           Objects.equal(schema, other.schema) &&
           Objects.equal(extension, other.extension);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(regex, maxErrors, schema, extension);
  }

  @JsonIgnore
  public boolean hasSchema() {
    return schema != null && ! schema.isEmpty();
  }

  @JsonIgnore
  public List<String> getFieldNames() {
    List<String> result = new ArrayList<>();
    if (! hasSchema()) {
      return result;
    }

    for (LogFormatField field : schema) {
      result.add(field.getFieldName());
    }
    return result;
  }

  @JsonIgnore
  public String getDataType(int fieldIndex) {
    LogFormatField field = getField(fieldIndex);
    return field == null ? null : field.getFieldType();
  }

  @JsonIgnore
  public LogFormatField getField(int fieldIndex) {
    if (schema == null || fieldIndex >= schema.size()) {
      return null;
    }
    return schema.get(fieldIndex);
  }

  @JsonIgnore
  public String getDateFormat(int fieldIndex) {
    LogFormatField field = getField(fieldIndex);
    return field == null ? null : field.getFormat();
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
        .field("regex", regex)
        .field("extension", extension)
        .field("schema", schema)
        .field("maxErrors", maxErrors)
        .toString();
  }
}
