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

package com.netease.arctic.server.dashboard.model;

import com.netease.arctic.ams.api.resource.ResourceGroup;

public class OptimizerResourceInfo {
  private ResourceGroup resourceGroup;
  private int occupationCore = 0;
  private long occupationMemory = 0;

  public ResourceGroup getResourceGroup() {
    return resourceGroup;
  }

  public void setResourceGroup(ResourceGroup resourceGroup) {
    this.resourceGroup = resourceGroup;
  }

  public int getOccupationCore() {
    return occupationCore;
  }

  public void setOccupationCore(int occupationCore) {
    this.occupationCore = occupationCore;
  }

  public void addOccupationCore(int occupationCore) {
    this.occupationCore = this.occupationCore + occupationCore;
  }

  public long getOccupationMemory() {
    return occupationMemory;
  }

  public void setOccupationMemory(long occupationMemory) {
    this.occupationMemory = occupationMemory;
  }

  public void addOccupationMemory(long occupationMemory) {
    this.occupationMemory = this.occupationMemory + occupationMemory;
  }
}
