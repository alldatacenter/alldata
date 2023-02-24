/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.impala.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;

/**
 * Represent an Impala lineage record in lineage log.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class ImpalaQuery {
  private String queryText;
  private String queryId;
  private String hash;
  private String user;

  // the time stamp is in seconds. It is Unix epoch, which is the number of seconds that have
  // elapsed since January 1, 1970 (midnight UTC/GMT), not counting leap seconds
  private Long timestamp;
  private Long endTime;
  private List<LineageEdge> edges;
  private List<LineageVertex> vertices;

  public List<LineageEdge> getEdges() {
    return edges;
  }

  public List<LineageVertex> getVertices() {
    return vertices;
  }

  public Long getEndTime() {
    return endTime;
  }

  public String getHash() {
    return hash;
  }

  public String getQueryId() {
    return queryId;
  }

  public String getQueryText() {
    return queryText;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public String getUser() {
    return user;
  }

  public void setEdges(List<LineageEdge> edges) {
    this.edges = edges;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }

  public void setQueryText(String queryText) {
    this.queryText = queryText;
  }

  public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

  public void setUser(String user) {
    this.user = user;
  }

  public void setVertices(List<LineageVertex> vertices) {
    this.vertices = vertices;
  }

}
