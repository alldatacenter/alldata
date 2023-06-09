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

package org.apache.drill.exec.store.excel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.exec.store.excel.ExcelBatchReader.ExcelReaderConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonTypeName(ExcelFormatPlugin.DEFAULT_NAME)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ExcelFormatConfig implements FormatPluginConfig {

  // This is the theoretical maximum number of rows in an Excel spreadsheet
  private final int MAX_ROWS = 1_048_576;

  private final List<String> extensions;
  private final int headerRow;
  private final int lastRow;
  private final int firstColumn;
  private final int lastColumn;
  private final boolean allTextMode;
  private final String sheetName;

  // Omitted properties take reasonable defaults
  @JsonCreator
  public ExcelFormatConfig(
      @JsonProperty("extensions") List<String> extensions,
      @JsonProperty("headerRow") Integer headerRow,
      @JsonProperty("lastRow") Integer lastRow,
      @JsonProperty("firstColumn") Integer firstColumn,
      @JsonProperty("lastColumn") Integer lastColumn,
      @JsonProperty("allTextMode") Boolean allTextMode,
      @JsonProperty("sheetName") String sheetName) {
    this.extensions = extensions == null
        ? Collections.singletonList("xlsx")
        : ImmutableList.copyOf(extensions);
    this.headerRow = headerRow == null ? 0 : headerRow;
    this.lastRow = lastRow == null ? MAX_ROWS :
      Math.min(MAX_ROWS, lastRow);
    this.firstColumn = firstColumn == null ? 0 : firstColumn;
    this.lastColumn = lastColumn == null ? 0 : lastColumn;
    this.allTextMode = allTextMode == null ? false : allTextMode;
    this.sheetName = sheetName == null ? "" : sheetName;
  }

  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  public List<String> getExtensions() {
    return extensions;
  }

  public int getHeaderRow() {
    return headerRow;
  }

  public int getLastRow() {
    return lastRow;
  }

  public int getFirstColumn() {
    return firstColumn;
  }

  public int getLastColumn() {
    return lastColumn;
  }

  public boolean getAllTextMode() {
    return allTextMode;
  }

  public String getSheetName() {
    return sheetName;
  }

  public ExcelReaderConfig getReaderConfig(ExcelFormatPlugin plugin) {
    ExcelReaderConfig readerConfig = new ExcelReaderConfig(plugin);
    return readerConfig;
  }

  @Override
  public int hashCode() {
    return Objects.hash(extensions, headerRow, lastRow,
        firstColumn, lastColumn, allTextMode, sheetName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ExcelFormatConfig other = (ExcelFormatConfig) obj;
    return Objects.equals(extensions, other.extensions)
      && Objects.equals(headerRow, other.headerRow)
      && Objects.equals(lastRow, other.lastRow)
      && Objects.equals(firstColumn, other.firstColumn)
      && Objects.equals(lastColumn, other.lastColumn)
      && Objects.equals(allTextMode, other.allTextMode)
      && Objects.equals(sheetName, other.sheetName);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
        .field("extensions", extensions)
        .field("sheetName", sheetName)
        .field("headerRow", headerRow)
        .field("lastRow", lastRow)
        .field("firstColumn", firstColumn)
        .field("lastColumn", lastColumn)
        .field("allTextMode", allTextMode)
        .toString();
  }
}
