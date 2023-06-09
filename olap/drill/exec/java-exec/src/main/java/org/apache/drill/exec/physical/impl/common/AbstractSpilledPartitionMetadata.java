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
package org.apache.drill.exec.physical.impl.common;

public abstract class AbstractSpilledPartitionMetadata implements SpilledPartitionMetadata {
  private final int cycle;
  private final int originPartition;
  private final int prevOriginPartition;

  public AbstractSpilledPartitionMetadata(final int cycle,
                                          final int originPartition,
                                          final int prevOriginPartition) {
    this.cycle = cycle;
    this.originPartition = originPartition;
    this.prevOriginPartition = prevOriginPartition;
  }

  @Override
  public int getCycle() {
    return cycle;
  }

  @Override
  public int getOriginPartition() {
    return originPartition;
  }

  @Override
  public int getPrevOriginPartition() {
    return prevOriginPartition;
  }
}
