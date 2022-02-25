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

package org.apache.ambari.server.api.services.stackadvisor.validations;

import java.util.Set;

import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorResponse;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.base.MoreObjects;

/**
 * Validation response POJO.
 */
public class ValidationResponse extends StackAdvisorResponse {

  @JsonProperty
  private Set<ValidationItem> items;

  public Set<ValidationItem> getItems() {
    return items;
  }

  public void setItems(Set<ValidationItem> items) {
    this.items = items;
  }

  public static class ValidationItem {
    @JsonProperty
    private String type;

    @JsonProperty
    private String level;

    @JsonProperty
    private String message;

    @JsonProperty("component-name")
    private String componentName;

    @JsonProperty
    private String host;

    @JsonProperty("config-type")
    private String configType;

    @JsonProperty("config-name")
    private String configName;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getLevel() {
      return level;
    }

    public void setLevel(String level) {
      this.level = level;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getComponentName() {
      return componentName;
    }

    public void setComponentName(String componentName) {
      this.componentName = componentName;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public String getConfigType() {
      return configType;
    }

    public void setConfigType(String configType) {
      this.configType = configType;
    }

    public String getConfigName() {
      return configName;
    }

    public void setConfigName(String configName) {
      this.configName = configName;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
        .add("type", type)
        .add("level", level)
        .add("message", message)
        .add("componentName", componentName)
        .add("host", host)
        .add("configType", configType)
        .add("configName", configName)
        .toString();
    }
  }

}
