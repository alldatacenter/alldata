/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.service.deploy.worker.congestcontrol;

import java.util.concurrent.atomic.LongAdder;

public class BufferStatusHub extends TimeSlidingHub<BufferStatusHub.BufferStatusNode> {

  public static class BufferStatusNode implements TimeSlidingHub.TimeSlidingNode {

    private final LongAdder numBytes;

    public BufferStatusNode(long numBytes) {
      this.numBytes = new LongAdder();
      if (numBytes != 0L) {
        this.numBytes.add(numBytes);
      }
    }

    public BufferStatusNode() {
      this(0);
    }

    @Override
    public void combineNode(TimeSlidingNode node) {
      BufferStatusNode needToCombined = (BufferStatusNode) node;
      numBytes.add(needToCombined.numBytes());
    }

    @Override
    public void separateNode(TimeSlidingNode node) {
      BufferStatusNode needToCombined = (BufferStatusNode) node;
      numBytes.add(-needToCombined.numBytes());
    }

    @Override
    public TimeSlidingNode clone() {
      return new BufferStatusNode(numBytes());
    }

    public long numBytes() {
      return numBytes.sum();
    }

    @Override
    public String toString() {
      return String.format("BufferStatusNode: {bytes: %s}", numBytes.sum());
    }
  }

  public BufferStatusHub(int timeWindowsInSecs) {
    super(timeWindowsInSecs);
  }

  @Override
  protected BufferStatusNode newEmptyNode() {
    return new BufferStatusNode();
  }

  public long avgBytesPerSec() {
    long currentNumBytes = sum().numBytes();
    if (currentNumBytes > 0) {
      return currentNumBytes * 1000 / (long) getCurrentTimeWindowsInMills();
    }
    return 0L;
  }
}
