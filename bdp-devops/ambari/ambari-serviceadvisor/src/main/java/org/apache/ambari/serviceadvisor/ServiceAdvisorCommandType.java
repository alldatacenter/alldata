/**
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


package org.apache.ambari.serviceadvisor;


// TODO, use this class instead of org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommandType
public enum ServiceAdvisorCommandType {

  RECOMMEND_COMPONENT_LAYOUT("recommend-component-layout"),

  VALIDATE_COMPONENT_LAYOUT("validate-component-layout"),

  RECOMMEND_CONFIGURATIONS("recommend-configurations"),

  RECOMMEND_CONFIGURATIONS_FOR_SSO("recommend-configurations-for-sso"),

  RECOMMEND_CONFIGURATIONS_FOR_KERBEROS("recommend-configurations-for-kerberos"),

  RECOMMEND_CONFIGURATION_DEPENDENCIES("recommend-configuration-dependencies"),

  VALIDATE_CONFIGURATIONS("validate-configurations");
  private final String name;

  private ServiceAdvisorCommandType(String name) {
    this.name = name;
  }

  public String getValue() {
    return this.name.toLowerCase();
  }

  @Override
  public String toString() {
    return this.name;
  }

  /**
   * Instead of Enum.valueOf("value"), use this method instead to map the string to the correct Enum.
   * @param name Name with lowercase and dashes.
   * @return Enum that matches the string.
   */
  public static ServiceAdvisorCommandType getEnum(String name) {
    for (ServiceAdvisorCommandType v : values()) {
      if (v.getValue().equalsIgnoreCase(name.replace("_", "-"))) {
        return v;
      }
    }
    throw new IllegalArgumentException();
  }
}