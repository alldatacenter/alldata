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
package org.apache.drill.exec.physical.impl.aggregate;

import org.apache.drill.exec.physical.impl.common.AbstractSpilledPartitionMetadata;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class HashAggSpilledPartition extends AbstractSpilledPartitionMetadata {
  private final int spilledBatches;
  private final String spillFile;

  public HashAggSpilledPartition(final int cycle,
                                 final int originPartition,
                                 final int prevOriginPartition,
                                 final int spilledBatches,
                                 final String spillFile) {
    super(cycle, originPartition, prevOriginPartition);

    this.spilledBatches = spilledBatches;
    this.spillFile = Preconditions.checkNotNull(spillFile);
  }

  public int getSpilledBatches() {
    return spilledBatches;
  }

  public String getSpillFile() {
    return spillFile;
  }

  @Override
  public String makeDebugString() {
    return String.format("Start reading spilled partition %d (prev %d) from cycle %d.",
      this.getOriginPartition(), this.getPrevOriginPartition(), this.getCycle());
  }
}
