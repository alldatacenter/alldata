/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.mongo.common;

import java.util.List;
import java.util.Map;

public class ChunkInfo {

  private List<String> chunkLocList;

  private String chunkId;

  private Map<String, Object> minFilters;

  private Map<String, Object> maxFilters;

  public ChunkInfo(List<String> chunkLocList, String chunkId) {
    this.chunkLocList = chunkLocList;
    this.chunkId = chunkId;
  }

  public List<String> getChunkLocList() {
    return chunkLocList;
  }

  public String getChunkId() {
    return chunkId;
  }

  public void setMinFilters(Map<String, Object> minFilters) {
    this.minFilters = minFilters;
  }

  public Map<String, Object> getMinFilters() {
    return minFilters;
  }

  public void setMaxFilters(Map<String, Object> maxFilters) {
    this.maxFilters = maxFilters;
  }

  public Map<String, Object> getMaxFilters() {
    return maxFilters;
  }

  @Override
  public String toString() {
    return "ChunkInfo [" + "chunkLocList=" + chunkLocList + ", chunkId="
        + chunkId + ", minFilters=" + minFilters + ", maxFilters=" + maxFilters
        + "]";
  }
}
