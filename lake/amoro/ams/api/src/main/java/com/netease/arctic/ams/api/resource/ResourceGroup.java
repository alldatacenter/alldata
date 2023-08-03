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

package com.netease.arctic.ams.api.resource;

import com.netease.arctic.ams.api.Constants;
import org.apache.curator.shaded.com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

public class ResourceGroup {
  private String name;
  private String container;
  private Map<String, String> properties;

  protected ResourceGroup() {
  }

  private ResourceGroup(String name, String container) {
    this.name = name;
    this.container = container;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  protected void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public String getContainer() {
    return container;
  }

  //generate inner builder class, use addProperties instead of set
  public static class Builder {
    private final String name;
    private final String container;
    private final Map<String, String> properties = new HashMap<>();

    public Builder(String name, String container) {
      Preconditions.checkArgument(
          name != null && container != null,
          "Resource group name and container name can not be null");
      this.name = name;
      this.container = container;
    }

    public Builder(String name) {
      Preconditions.checkArgument(
          name != null,
          "Resource group name can not be null");
      this.name = name;
      this.container = Constants.EXTERNAL_RESOURCE_CONTAINER;
    }

    public String getName() {
      return name;
    }

    public String getContainer() {
      return container;
    }

    public Builder addProperty(String key, String value) {
      this.properties.put(key, value);
      return this;
    }

    public Builder addProperties(Map<String, String> properties) {
      this.properties.putAll(properties);
      return this;
    }

    public ResourceGroup build() {
      ResourceGroup resourceGroup = new ResourceGroup(name, container);
      resourceGroup.setProperties(properties);
      return resourceGroup;
    }
  }
}
