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

package org.apache.ambari.server.orm.entities;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang.builder.EqualsBuilder;

@SuppressWarnings("serial")
public class StageEntityPK implements Serializable {
  private Long requestId;
  private Long stageId;

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public Long getStageId() {
    return stageId;
  }

  public void setStageId(Long stageId) {
    this.stageId = stageId;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    StageEntityPK that = (StageEntityPK) object;
    EqualsBuilder equalsBuilder = new EqualsBuilder();
    equalsBuilder.append(requestId, that.requestId);
    equalsBuilder.append(stageId, that.stageId);
    return equalsBuilder.isEquals();
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestId, stageId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("StageEntityPK{");
    buffer.append("stageId=").append(getStageId());
    buffer.append("requestId=").append(getRequestId());
    buffer.append("}");
    return buffer.toString();
  }
}
