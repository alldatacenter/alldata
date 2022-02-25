/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.state;

import com.google.gson.annotations.SerializedName;

public class ModuleDependency {
  @SerializedName("id")
  private String id;
  @SerializedName("name")
  private String name;
  public enum DependencyType {
    @SerializedName("RUNTIME")
    RUNTIME,
    @SerializedName("INSTALL")
    INSTALL
  }
  @SerializedName("dependencyType")
  private DependencyType dependencyType;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public DependencyType getDependencyType() {
    return dependencyType;
  }

  public void setDependencyType(DependencyType dependencyType) {
    this.dependencyType = dependencyType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ModuleDependency that = (ModuleDependency) o;

    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    return dependencyType != null ? dependencyType.equals(that.dependencyType) : that.dependencyType == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (dependencyType != null ? dependencyType.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ModuleDependency{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", dependencyType='" + dependencyType + '\'' +
            '}';
  }
}
