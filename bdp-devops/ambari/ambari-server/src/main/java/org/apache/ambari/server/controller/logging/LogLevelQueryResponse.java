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
package org.apache.ambari.server.controller.logging;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LogLevelQueryResponse {

  private String startIndex;

  private String pageSize;

  private String totalCount;

  private String resultSize;

  private String queryTimeMS;

  private List<NameValuePair> nameValueList;


  @JsonProperty("startIndex")
  public String getStartIndex() {
    return startIndex;
  }

  @JsonProperty("startIndex")
  public void setStartIndex(String startIndex) {
    this.startIndex = startIndex;
  }

  @JsonProperty("pageSize")
  public String getPageSize() {
    return pageSize;
  }

  @JsonProperty("pageSize")
  public void setPageSize(String pageSize) {
    this.pageSize = pageSize;
  }

  @JsonProperty("totalCount")
  public String getTotalCount() {
    return totalCount;
  }

  @JsonProperty("totalCount")
  public void setTotalCount(String totalCount) {
    this.totalCount = totalCount;
  }

  @JsonProperty("resultSize")
  public String getResultSize() {
    return resultSize;
  }

  @JsonProperty("resultSize")
  public void setResultSize(String resultSize) {
    this.resultSize = resultSize;
  }

  @JsonProperty("queryTimeMS")
  public String getQueryTimeMS() {
    return queryTimeMS;
  }

  @JsonProperty("queryTimeMS")
  public void setQueryTimeMS(String queryTimeMS) {
    this.queryTimeMS = queryTimeMS;
  }


  @JsonProperty("vNameValues")
  public List<NameValuePair> getNameValueList() {
    return nameValueList;
  }

  @JsonProperty("vNameValues")
  public void setNameValueList(List<NameValuePair> nameValueList) {
    this.nameValueList = nameValueList;
  }
}
