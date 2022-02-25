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
package org.apache.ambari.server.stack.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 */
public abstract class ServerSideActionTask extends Task {

  @XmlAttribute(name="class")
  public String implClass;

  @XmlElement(name = "parameter")
  public List<TaskParameter> parameters;

  public Map<String, String> getParameters(){
    Map<String, String> result = new HashMap<>();
    if (parameters != null) {
      for (TaskParameter parameter : parameters) {
        result.put(parameter.name, parameter.value);
      }
    }
    return result;
  }

  public static final String actionVerb = "Executing";

  public String getImplementationClass() {
    return implClass;
  }

  public void setImplClass(String implClass) {
    this.implClass = implClass;
  }

  @XmlElement(name="message")
  public List<String> messages = new ArrayList<>();

  @Override
  public String getActionVerb() {
    return actionVerb;
  }
}
