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

@XmlAccessorType(XmlAccessType.FIELD)
public final class ServicePropertyInfo {
  /**
   * Name of the service property
   */
  private String name;

  /**
   * Value of the service property
   */
  private String value;


  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || !(o instanceof ServicePropertyInfo)) return false;

    ServicePropertyInfo that = (ServicePropertyInfo) o;

    return new EqualsBuilder()
      .append(name, that.name)
      .append(value, that.value)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(name)
      .append(value)
      .toHashCode();
  }
}
