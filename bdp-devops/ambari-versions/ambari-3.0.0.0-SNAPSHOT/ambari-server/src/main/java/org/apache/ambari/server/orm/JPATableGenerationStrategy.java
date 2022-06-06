/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm;

public enum JPATableGenerationStrategy {
  //create tables which don't exist
  CREATE("create"),
  //creates tables which not exist, add missing columns
  CREATE_OR_EXTEND("createOrExtend"),
  //drop and create all tables
  DROP_AND_CREATE("dropAndCreate"),
  //don't create tables
  NONE("none");

  private String value;

  JPATableGenerationStrategy(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static JPATableGenerationStrategy fromString(String value) {
    for (JPATableGenerationStrategy strategy : values()) {
      if (strategy.value.equalsIgnoreCase(value)) {
        return strategy;
      }
    }
    return NONE;
  }
}
