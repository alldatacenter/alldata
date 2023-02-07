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
package org.apache.drill.exec.store.druid.druid;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;

public class DruidScanResponse {

  final private String segementId;
  final private ArrayList<String> columns;
  final private ArrayList<ObjectNode> events;

  public DruidScanResponse(String segementId, ArrayList<String> columns, ArrayList<ObjectNode> events) {
    this.segementId = segementId;
    this.columns = columns;
    this.events = events;
  }

  public String getSegementId() {
    return segementId;
  }

  public ArrayList<String> getColumns() {
    return columns;
  }

  public ArrayList<ObjectNode> getEvents() {
    return events;
  }

}
