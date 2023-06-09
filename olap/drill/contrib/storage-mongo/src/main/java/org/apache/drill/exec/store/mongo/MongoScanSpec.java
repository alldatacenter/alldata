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
package org.apache.drill.exec.store.mongo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.exec.planner.logical.DrillTableSelection;

import java.util.ArrayList;
import java.util.List;

public class MongoScanSpec implements DrillTableSelection {
  private final String dbName;
  private final String collectionName;

  private String filters;

  private List<String> operations = new ArrayList<>();

  @JsonCreator
  public MongoScanSpec(@JsonProperty("dbName") String dbName,
      @JsonProperty("collectionName") String collectionName) {
    this.dbName = dbName;
    this.collectionName = collectionName;
  }

  public MongoScanSpec(String dbName, String collectionName, String filters, List<String> operations) {
    this.dbName = dbName;
    this.collectionName = collectionName;
    this.filters = filters;
    this.operations = operations;
  }

  public String getDbName() {
    return this.dbName;
  }

  public String getCollectionName() {
    return this.collectionName;
  }

  public String getFilters() {
    return this.filters;
  }

  public List<String> getOperations() {
    return this.operations;
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("dbName", dbName)
      .field("collectionName", collectionName)
      .field("filters", filters)
      .field("operations", operations)
      .toString();
  }

  @Override
  public String digest() {
    return toString();
  }
}
