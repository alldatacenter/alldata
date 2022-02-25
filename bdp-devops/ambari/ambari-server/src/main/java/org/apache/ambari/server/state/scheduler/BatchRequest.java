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
package org.apache.ambari.server.state.scheduler;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class BatchRequest implements Comparable<BatchRequest> {
  private Long orderId;
  private Long requestId;
  private Type type;
  private String uri;
  private String body;
  private String status;
  private Integer returnCode;
  private String responseMsg;

  @JsonProperty("order_id")
  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  @JsonProperty("request_id")
  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  @JsonProperty("request_type")
  public String getType() {
    return type.name();
  }

  public void setType(Type type) {
    this.type = type;
  }

  @JsonProperty("request_uri")
  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("request_body")
  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("request_status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("return_code")
  public Integer getReturnCode() {
    return returnCode;
  }

  public void setReturnCode(Integer returnCode) {
    this.returnCode = returnCode;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("response_message")
  public String getResponseMsg() {
    return responseMsg;
  }

  public void setResponseMsg(String responseMsg) {
    this.responseMsg = responseMsg;
  }

  @Override
  public int compareTo(BatchRequest batchRequest) {
    return this.orderId.compareTo(batchRequest.getOrderId());
  }

  public enum Type {
    PUT,
    POST,
    DELETE
  }

  @Override
  public String toString() {
    return "BatchRequest {" +
      "orderId=" + orderId +
      ", type=" + type +
      ", uri='" + uri + '\'' +
      '}';
  }
}
