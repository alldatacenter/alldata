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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents the customCommand tag at service/component metainfo
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomCommandDefinition {

  private String name;
  private String opsDisplayName;
  private CommandScriptDefinition commandScript;
  private boolean background = false;
  private boolean hidden = false;

  public String getName() {
    return name;
  }
  
  public boolean isBackground() {
    return background;
  }

  public boolean isHidden() {
    return hidden;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  public String getOpsDisplayName() {
    return opsDisplayName;
  }

  public CommandScriptDefinition getCommandScript() {
    return commandScript;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (! (obj instanceof CustomCommandDefinition)) {
      return false;
    }

    CustomCommandDefinition rhs = (CustomCommandDefinition) obj;
    return new EqualsBuilder().
            append(name, rhs.name).
            append(commandScript, rhs.commandScript).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 31).
            append(name).
            append(commandScript).toHashCode();
  }
}
