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
package org.apache.ambari.server.customactions;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The elements within a service's configuration file.
 */
@XmlRootElement(name="actionDefinitions")
public class ActionDefinitionXml {
  
  @XmlElement(name="actionDefinition")
  private List<ActionDefinitionSpec> actionDefinitions = new ArrayList<>();
  
  /**
   * @return the list of properties contained in a configuration file
   */
  public List<ActionDefinitionSpec> actionDefinitions() {
    return actionDefinitions;
  }

}
