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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BloomFilterDef {
  //bloom filter size in bytes
  private int numBytes;
  //true send to itself, false send to foreman
  private boolean local;

  private String probeField;

  private String buildField;
  //TODO
  @JsonIgnore
  private Double leftNDV;
  //TODO
  @JsonIgnore
  private Double rightNDV;

  @JsonCreator
  public BloomFilterDef(@JsonProperty("numBytes") int numBytes, @JsonProperty("local") boolean local, @JsonProperty("probeField")
                        String probeField, @JsonProperty("buildField") String buildField){
    this.numBytes = numBytes;
    this.local = local;
    this.probeField = probeField;
    this.buildField = buildField;
  }


  public int getNumBytes() {
    return numBytes;
  }

  public boolean isLocal() {
    return local;
  }

  public void setLocal(boolean local) {
    this.local = local;
  }

  public String getProbeField() {
    return probeField;
  }

  public String toString() {
    return "BF:{numBytes=" + numBytes + ",send2Foreman=" + !local + ",probeField= " + probeField + ",buildField= " + buildField + " }";
  }

  @JsonIgnore
  public Double getLeftNDV() {
    return leftNDV;
  }

  public void setLeftNDV(Double leftNDV) {
    this.leftNDV = leftNDV;
  }

  @JsonIgnore
  public Double getRightNDV() {
    return rightNDV;
  }

  public void setRightNDV(Double rightNDV) {
    this.rightNDV = rightNDV;
  }

  public String getBuildField()
  {
    return buildField;
  }

}
