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

package org.apache.ambari.server.state;

import org.codehaus.jackson.annotate.JsonProperty;

public class ChangedConfigInfo {

  private String type;

  private String name;

  @JsonProperty("old_value")
  private String oldValue;

  public ChangedConfigInfo() {

  }

  public ChangedConfigInfo(String type, String name) {
    this(type, name, null);
  }

  public ChangedConfigInfo(String type, String name, String oldValue) {
    this.type = type;
    this.name = name;
    this.oldValue = oldValue;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public String getOldValue() {
    return oldValue;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ChangedConfigInfo that = (ChangedConfigInfo) o;

    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    return !(oldValue != null ? !oldValue.equals(that.oldValue) : that.oldValue != null);

  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (oldValue != null ? oldValue.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ChangedConfigInfo{" +
        "type='" + type + '\'' +
        ", name='" + name + '\'' +
        ", oldValue='" + oldValue + '\'' +
        '}';
  }

}

