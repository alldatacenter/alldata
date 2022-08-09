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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@IdClass(RequestScheduleBatchRequestEntityPK.class)
@Entity
@Table(name = "requestschedulebatchrequest")
@NamedQueries({
  @NamedQuery(name = "findByScheduleId", query = "SELECT batchreqs FROM " +
    "RequestScheduleBatchRequestEntity  batchreqs WHERE batchreqs.scheduleId=:id")
})
public class RequestScheduleBatchRequestEntity {
  @Id
  @Column(name = "schedule_id", nullable = false, insertable = true, updatable = true)
  private Long scheduleId;

  @Id
  @Column(name = "batch_id", nullable = false, insertable = true, updatable = true)
  private Long batchId;

  @Column(name = "request_id")
  private Long requestId;

  @Column(name = "request_type", length = 255)
  private String requestType;

  @Column(name = "request_uri", length = 1024)
  private String requestUri;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "request_body")
  private byte[] requestBody;

  @Column(name = "request_status", length = 255)
  private String requestStatus;

  @Column(name = "return_code")
  private Integer returnCode;

  @Column(name = "return_message", length = 2000)
  private String returnMessage;

  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "schedule_id", referencedColumnName = "schedule_id", nullable = false, insertable = false, updatable = false) })
  private RequestScheduleEntity requestScheduleEntity;

  public Long getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(Long scheduleId) {
    this.scheduleId = scheduleId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public String getRequestType() {
    return requestType;
  }

  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }

  public String getRequestUri() {
    return requestUri;
  }

  public void setRequestUri(String requestUri) {
    this.requestUri = requestUri;
  }

  public byte[] getRequestBody() {
    return requestBody;
  }

  public String getRequestBodyAsString() {
    return requestBody != null ? new String(requestBody) : null;
  }

  public void setRequestBody(byte[] requestBody) {
    this.requestBody = requestBody;
  }

  public void setRequestBody(String requestBodyStr) {
    if (requestBodyStr != null) {
      requestBody = requestBodyStr.getBytes();
    }
  }

  public String getRequestStatus() {
    return requestStatus;
  }

  public void setRequestStatus(String requestStatus) {
    this.requestStatus = requestStatus;
  }

  public Integer getReturnCode() {
    return returnCode;
  }

  public void setReturnCode(Integer returnCode) {
    this.returnCode = returnCode;
  }

  public String getReturnMessage() {
    return returnMessage;
  }

  public void setReturnMessage(String returnMessage) {
    this.returnMessage = returnMessage;
  }

  public RequestScheduleEntity getRequestScheduleEntity() {
    return requestScheduleEntity;
  }

  public void setRequestScheduleEntity(RequestScheduleEntity requestScheduleEntity) {
    this.requestScheduleEntity = requestScheduleEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestScheduleBatchRequestEntity that = (RequestScheduleBatchRequestEntity) o;

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

  @Override
  public String toString() {
    return "RequestScheduleBatchRequestEntity{" +
      "scheduleId=" + scheduleId +
      ", batchId=" + batchId +
      ", requestId=" + requestId +
      ", requestType='" + requestType + '\'' +
      ", requestUri='" + requestUri + '\'' +
      ", requestStatus='" + requestStatus + '\'' +
      ", returnCode=" + returnCode +
      ", returnMessage='" + returnMessage + '\'' +
      '}';
  }
}
