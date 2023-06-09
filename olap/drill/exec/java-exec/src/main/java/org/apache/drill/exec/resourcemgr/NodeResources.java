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
package org.apache.drill.exec.resourcemgr;

/**
 * Provides resources for a node in cluster. Currently it is used to support only 2 kind of resources:
 * <ul>
 * <li>Memory</li>
 * <li>Virtual CPU count</li>
 * </ul>
 * It also has a version field to support extensibility in future to add other resources like network, disk, etc
 */
public class NodeResources {

  private final int version;

  private final long memoryInBytes;

  private final int numVirtualCpu;

  public NodeResources(long memoryInBytes, int numVirtualCpu) {
    this.memoryInBytes = memoryInBytes;
    this.numVirtualCpu = numVirtualCpu;
    this.version = 1;
  }

  public NodeResources(long memoryInBytes, int numPhysicalCpu, int vFactor) {
    this(memoryInBytes, numPhysicalCpu * vFactor);
  }

  public long getMemoryInBytes() {
    return memoryInBytes;
  }

  public long getMemoryInMB() {
    return Math.round((memoryInBytes / 1024L) / 1024L);
  }

  public long getMemoryInGB() {
    return Math.round(getMemoryInMB() / 1024L);
  }

  public int getNumVirtualCpu() {
    return numVirtualCpu;
  }

  @Override
  public String toString() {
    return "{ Version: " + version + ", MemoryInBytes: " + memoryInBytes + ", VirtualCPU: " + numVirtualCpu + " }";
  }
}
