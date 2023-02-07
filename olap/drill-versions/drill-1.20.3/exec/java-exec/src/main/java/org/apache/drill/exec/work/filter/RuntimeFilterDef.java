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
package org.apache.drill.exec.work.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RuntimeFilterDef {

  private boolean generateBloomFilter;

  private boolean generateMinMaxFilter;

  private List<BloomFilterDef> bloomFilterDefs;

  private boolean sendToForeman;

  private long runtimeFilterIdentifier;

  @JsonCreator
  public RuntimeFilterDef(@JsonProperty("generateBloomFilter") boolean generateBloomFilter, @JsonProperty("generateMinMaxFilter") boolean generateMinMaxFilter,
                          @JsonProperty("bloomFilterDefs") List<BloomFilterDef> bloomFilterDefs, @JsonProperty("sendToForeman") boolean sendToForeman,
                          @JsonProperty("runtimeFilterIdentifier") long runtimeFilterIdentifier) {
    this.generateBloomFilter = generateBloomFilter;
    this.generateMinMaxFilter = generateMinMaxFilter;
    this.bloomFilterDefs = bloomFilterDefs;
    this.sendToForeman = sendToForeman;
    this.runtimeFilterIdentifier = runtimeFilterIdentifier;
  }


  public boolean isGenerateBloomFilter() {
    return generateBloomFilter;
  }

  public void setGenerateBloomFilter(boolean generateBloomFilter) {
    this.generateBloomFilter = generateBloomFilter;
  }

  public boolean isGenerateMinMaxFilter() {
    return generateMinMaxFilter;
  }

  public void setGenerateMinMaxFilter(boolean generateMinMaxFilter) {
    this.generateMinMaxFilter = generateMinMaxFilter;
  }


  public List<BloomFilterDef> getBloomFilterDefs() {
    return bloomFilterDefs;
  }

  public void setBloomFilterDefs(List<BloomFilterDef> bloomFilterDefs) {
    this.bloomFilterDefs = bloomFilterDefs;
  }

  public boolean isSendToForeman() {
    return sendToForeman;
  }

  public void setSendToForeman(boolean sendToForeman) {
    this.sendToForeman = sendToForeman;
  }

  public long getRuntimeFilterIdentifier() {
    return runtimeFilterIdentifier;
  }

  public void setRuntimeFilterIdentifier(long runtimeFilterIdentifier) {
    this.runtimeFilterIdentifier = runtimeFilterIdentifier;
  }
}
