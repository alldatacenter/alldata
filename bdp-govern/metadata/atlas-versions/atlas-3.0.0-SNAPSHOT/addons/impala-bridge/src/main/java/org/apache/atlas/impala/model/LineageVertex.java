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

/**
 * This represents an entity in Impala's lineage record.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class LineageVertex {
  // id is used to reference this entity. It is used in LineageEdge to specify source and target
  // https://github.com/apache/impala/blob/master/be/src/util/lineage-util.h#L40
  // Impala id is int64. Therefore, define this field as Long
  private Long id;

  // specify the type of the entity, it could be "TABLE", "COLUMN" etc.
  private ImpalaVertexType vertexType;

  // specify the name of the entity
  private String vertexId;

  // It is optional, and could be null. It is only set if the entity is a column, and this field contains metadata of its table.
  private LineageVertexMetadata metadata;

  // It is optional. Its unit in seconds.
  private Long createTime;

  public Long getId() { return id; }

  public ImpalaVertexType getVertexType() {
    return vertexType;
  }

  public String getVertexId() {
    return vertexId;
  }

  public LineageVertexMetadata getMetadata() {
    return metadata;
  }

  public Long getCreateTime() { return createTime; }

  public void setId(Long id) {
    this.id = id;
  }

  public void setVertexType(ImpalaVertexType vertexType) {
    this.vertexType = vertexType;
  }

  public void setVertexId(String vertexId) {
    this.vertexId = vertexId;
  }

  public void setMetadata(LineageVertexMetadata metadata) { this.metadata = metadata; }

  public void setCreateTime(Long createTime) { this.createTime = createTime; }
}