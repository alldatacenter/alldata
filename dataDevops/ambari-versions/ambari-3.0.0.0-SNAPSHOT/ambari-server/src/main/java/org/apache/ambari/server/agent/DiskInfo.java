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

package org.apache.ambari.server.agent;

import org.codehaus.jackson.annotate.JsonProperty;


/**
 * Information about a mounted disk on a given node
 */

public class DiskInfo {
  String available;
  String mountpoint;
  String device;
  String used;
  String percent;
  String size;
  String type;

  /**
   * DiskInfo object that tracks information about a disk.
   * @param mountpoint
   * @param available
   * @param used
   * @param percent
   * @param size
   */
  public DiskInfo(String device, String mountpoint, String available,
      String used, String percent, String size, String type) {
    this.device = device;
    this.mountpoint = mountpoint;
    this.available = available;
    this.used = used;
    this.percent = percent;
    this.size = size;
    this.type = type;
  }

  /**
   * Needed for Serialization
   */
  public DiskInfo() {}

  @JsonProperty("available")
  @com.fasterxml.jackson.annotation.JsonProperty("available")
  public void setAvailable(String available) {
    this.available = available;
  }
  
  @JsonProperty("available")
  @com.fasterxml.jackson.annotation.JsonProperty("available")
  public String getAvailable() {
    return this.available;
  }

  @JsonProperty("mountpoint")
  @com.fasterxml.jackson.annotation.JsonProperty("mountpoint")
  public String getMountPoint() {
    return this.mountpoint;
  }
  
  @JsonProperty("mountpoint")
  @com.fasterxml.jackson.annotation.JsonProperty("mountpoint")
  public void setMountPoint(String mountpoint) {
    this.mountpoint = mountpoint;
  }

  @JsonProperty("type")
  @com.fasterxml.jackson.annotation.JsonProperty("type")
  public String getType() {
    return this.type;
  }

  @JsonProperty("type")
  @com.fasterxml.jackson.annotation.JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }
  
  @JsonProperty("used")
  @com.fasterxml.jackson.annotation.JsonProperty("used")
  public String getUsed() {
    return this.used;
  }

  @JsonProperty("used")
  @com.fasterxml.jackson.annotation.JsonProperty("used")
  public void setUsed(String used) {
    this.used = used;
  }
  
  @JsonProperty("percent")
  @com.fasterxml.jackson.annotation.JsonProperty("percent")
  public String getPercent() {
    return this.percent;
  }
  
  @JsonProperty("percent")
  @com.fasterxml.jackson.annotation.JsonProperty("percent")
  public void setPercent(String percent) {
    this.percent = percent;
  }
  
  @JsonProperty("size")
  @com.fasterxml.jackson.annotation.JsonProperty("size")
  public String getSize() {
    return this.size;
  }
  
  @JsonProperty("size")
  @com.fasterxml.jackson.annotation.JsonProperty("size")
  public void setSize(String size) {
    this.size = size;
  }

  @JsonProperty("device")
  @com.fasterxml.jackson.annotation.JsonProperty("device")
  public String getDevice() {
    return device;
  }

  @JsonProperty("device")
  @com.fasterxml.jackson.annotation.JsonProperty("device")
  public void setDevice(String device) {
    this.device = device;
  }

  @Override
  public String toString() {
    return "available=" + this.available + ",mountpoint=" + this.mountpoint
         + ",used=" + this.used + ",percent=" + this.percent + ",size=" +
        this.size + ",device=" + this.device +
        ",type=" + this.type;
  }
}
