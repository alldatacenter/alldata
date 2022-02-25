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
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Represents the log tag at service/component metainfo
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class LogDefinition
{
  private String logId;
  private boolean primary;

  public String getLogId() {
    return logId;
  }

  public boolean isPrimary() {
    return primary;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null)
      return false;

    if (obj == this)
      return true;

    if ( !(obj instanceof LogDefinition) )
      return false;

    LogDefinition other = (LogDefinition) obj;
    return new EqualsBuilder()
        .append(logId, other.logId)
        .append(primary, other.primary)
        .isEquals();
  }

  @Override
  public int hashCode()
  {
    return new HashCodeBuilder(17, 31)
        .append(logId)
        .append(primary)
        .toHashCode();
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
