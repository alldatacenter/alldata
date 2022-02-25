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
 * Represents the configFiles tag at service/component metainfo
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ClientConfigFileDefinition {

  private String type;
  private String fileName;
  private String dictionaryName;
  private boolean optional = false;

  public boolean isOptional() {
    return optional;
  }

  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getDictionaryName() {
    return dictionaryName;
  }

  public void setDictionaryName(String dictionaryName) {
    this.dictionaryName = dictionaryName;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (! (obj instanceof ClientConfigFileDefinition)) {
      return false;
    }

    ClientConfigFileDefinition rhs = (ClientConfigFileDefinition) obj;
    return new EqualsBuilder().
            append(type, rhs.type).
            append(fileName, rhs.fileName).
        append(dictionaryName, rhs.dictionaryName).
        append(optional, rhs.optional).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 31).
            append(type).
            append(fileName).
        append(dictionaryName).
        append(optional).toHashCode();
  }
}
