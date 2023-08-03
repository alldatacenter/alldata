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

package com.netease.arctic.server.dashboard.model;

import java.util.List;
import java.util.Map;

public class UpgradeHiveMeta {
  private Map<String, String> properties;
  private List<PrimaryKeyField> pkList;

  public UpgradeHiveMeta(Map<String, String> properties, List<PrimaryKeyField> pkList) {
    this.properties = properties;
    this.pkList = pkList;
  }

  public UpgradeHiveMeta() {
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public List<PrimaryKeyField> getPkList() {
    return pkList;
  }

  public void setPkList(List<PrimaryKeyField> pkList) {
    this.pkList = pkList;
  }

  public static class PrimaryKeyField {
    private String fieldName;

    public PrimaryKeyField(String fieldName) {
      this.fieldName = fieldName;
    }

    public PrimaryKeyField() {
    }

    public String getFieldName() {
      return fieldName;
    }

    public void setFieldName(String fieldName) {
      this.fieldName = fieldName;
    }
  }
}
