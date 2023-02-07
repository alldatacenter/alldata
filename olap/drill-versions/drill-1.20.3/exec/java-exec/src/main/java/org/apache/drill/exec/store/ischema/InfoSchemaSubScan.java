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
package org.apache.drill.exec.store.ischema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.exec.physical.base.AbstractSubScan;

public class InfoSchemaSubScan extends AbstractSubScan {

  public static final String OPERATOR_TYPE = "INFO_SCHEMA_SUB_SCAN";

  private final InfoSchemaTableType table;
  private final InfoSchemaFilter filter;

  @JsonCreator
  public InfoSchemaSubScan(@JsonProperty("table") InfoSchemaTableType table,
                           @JsonProperty("filter") InfoSchemaFilter filter) {
    super(null);
    this.table = table;
    this.filter = filter;
  }

  @JsonProperty("table")
  public InfoSchemaTableType getTable() {
    return table;
  }

  @JsonProperty("filter")
  public InfoSchemaFilter getFilter() {
    return filter;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
