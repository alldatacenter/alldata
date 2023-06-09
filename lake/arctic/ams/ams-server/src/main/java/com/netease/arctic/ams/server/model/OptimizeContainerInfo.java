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

package com.netease.arctic.ams.server.model;

import java.util.List;
import java.util.Map;

public class OptimizeContainerInfo {
  String name;
  String type;
  Map<String,String> properties;

  List<OptimizeGroup> optimizeGroup;

  public OptimizeContainerInfo() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public List<OptimizeGroup> getOptimizeGroup() {
    return optimizeGroup;
  }

  public void setOptimizeGroup(List<OptimizeGroup> optimizeGroup) {
    this.optimizeGroup = optimizeGroup;
  }

  public static class OptimizeGroup {
    String name;
    Integer tmMemory;
    Integer jmMemory;

    public OptimizeGroup(String name, Integer tmMemory, Integer jmMemory) {
      this.name = name;
      this.tmMemory = tmMemory;
      this.jmMemory = jmMemory;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Integer getTmMemory() {
      return tmMemory;
    }

    public void setTmMemory(Integer tmMemory) {
      this.tmMemory = tmMemory;
    }

    public Integer getJmMemory() {
      return jmMemory;
    }

    public void setJmMemory(Integer jmMemory) {
      this.jmMemory = jmMemory;
    }
  }
}
