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

import javax.persistence.Column;
import javax.persistence.Id;

public class RequestScheduleBatchRequestEntityPK implements Serializable {
  private Long scheduleId;
  private Long batchId;

  @Id
  @Column(name = "schedule_id", nullable = false, insertable = true, updatable = true)
  public Long getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(Long scheduleId) {
    this.scheduleId = scheduleId;
  }

  @Id
  @Column(name = "batch_id", nullable = false, insertable = true, updatable = true)
  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestScheduleBatchRequestEntityPK that = (RequestScheduleBatchRequestEntityPK) o;

    if (!batchId.equals(that.batchId)) return false;
    if (!scheduleId.equals(that.scheduleId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = scheduleId.hashCode();
    result = 31 * result + batchId.hashCode();
    return result;
  }
}
