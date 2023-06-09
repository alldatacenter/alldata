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
package org.apache.drill.exec.store.mapr.db;

import org.apache.drill.exec.store.mapr.TableFormatPluginConfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("maprdb")
@JsonInclude(Include.NON_DEFAULT)
public class MapRDBFormatPluginConfig extends TableFormatPluginConfig {

  public boolean allTextMode = false;
  public boolean enablePushdown = true;
  public boolean ignoreSchemaChange = false;
  public boolean readAllNumbersAsDouble = false;
  public boolean disableCountOptimization = false;
  public boolean readTimestampWithZoneOffset = false;
  /* This flag is a switch to do special handling in case of
   * no columns in the query exists in the maprdb table. This flag
   * can get deprecated once it is observed that this special handling
   * is not regressing performance of reading maprdb table.
   */
  public boolean nonExistentFieldSupport = true;
  public String index = "";

  @Override
  public int hashCode() {
    int result = (allTextMode ? 1231 : 1237);
    result = 31 * result + (enablePushdown ? 1231 : 1237);
    result = 31 * result + (ignoreSchemaChange ? 1231 : 1237);
    result = 31 * result + (readAllNumbersAsDouble ? 1231 : 1237);
    result = 31 * result + (disableCountOptimization ? 1231 : 1237);
    result = 31 * result + (nonExistentFieldSupport ? 1231 : 1237);
    result = 31 * result + (readTimestampWithZoneOffset ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    MapRDBFormatPluginConfig other = (MapRDBFormatPluginConfig) obj;
    if (readAllNumbersAsDouble != other.readAllNumbersAsDouble) {
      return false;
    } else if (allTextMode != other.allTextMode) {
      return false;
    } else if (ignoreSchemaChange != other.ignoreSchemaChange) {
      return false;
    } else if (enablePushdown != other.enablePushdown) {
      return false;
    } else if (disableCountOptimization != other.disableCountOptimization) {
      return false;
    } else if (nonExistentFieldSupport != other.nonExistentFieldSupport) {
      return false;
    } else if (!index.equals(other.index)) {
      return false;
    } else if (readTimestampWithZoneOffset != other.readTimestampWithZoneOffset) {
      return false;
    }
    return true;
  }


  @Override
  protected boolean impEquals(Object obj) {
    MapRDBFormatPluginConfig other = (MapRDBFormatPluginConfig) obj;
    if (readAllNumbersAsDouble != other.readAllNumbersAsDouble) {
      return false;
    } else if (allTextMode != other.allTextMode) {
      return false;
    } else if (ignoreSchemaChange != other.ignoreSchemaChange) {
      return false;
    } else if (enablePushdown != other.enablePushdown) {
      return false;
    } else if (disableCountOptimization != other.disableCountOptimization) {
      return false;
    } else if (nonExistentFieldSupport != other.nonExistentFieldSupport) {
      return false;
    } else if (!index.equals(other.index)) {
      return false;
    } else if (readTimestampWithZoneOffset != other.readTimestampWithZoneOffset) {
      return false;
    }
    return true;
  }

  public boolean isReadAllNumbersAsDouble() {
    return readAllNumbersAsDouble;
  }

  public boolean isReadTimestampWithZoneOffset() {
    return readTimestampWithZoneOffset;
  }

  public boolean isAllTextMode() {
    return allTextMode;
  }

  public boolean disableCountOptimization() {
    return disableCountOptimization;
  }

  public boolean isEnablePushdown() {
    return enablePushdown;
  }

  public boolean isNonExistentFieldSupport() { return nonExistentFieldSupport; }

  public boolean isIgnoreSchemaChange() {
    return ignoreSchemaChange;
  }

  public String getIndex() { return this.index; }
}
