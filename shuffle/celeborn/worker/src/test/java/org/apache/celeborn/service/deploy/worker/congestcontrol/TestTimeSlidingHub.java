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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestTimeSlidingHub {

  private static class DummyTimeSlidingHub
      extends TimeSlidingHub<DummyTimeSlidingHub.DummyTimeSlidingNode> {

    public static class DummyTimeSlidingNode implements TimeSlidingHub.TimeSlidingNode {

      private final AtomicInteger value;

      public DummyTimeSlidingNode(int initialValue) {
        this.value = new AtomicInteger(initialValue);
      }

      @Override
      public void combineNode(TimeSlidingNode node) {
        DummyTimeSlidingNode temp = (DummyTimeSlidingNode) node;
        this.value.getAndUpdate(v -> v + temp.getValue());
      }

      @Override
      public void separateNode(TimeSlidingNode node) {
        DummyTimeSlidingNode temp = (DummyTimeSlidingNode) node;
        this.value.getAndUpdate(v -> v - temp.getValue());
      }

      @Override
      public TimeSlidingNode clone() {
        return new DummyTimeSlidingNode(this.value.get());
      }

      public int getValue() {
        return value.get();
      }
    }

    private long dummyTimestamp = 0L;

    public DummyTimeSlidingHub(int timeWindowsInSecs) {
      super(timeWindowsInSecs);
    }

    @Override
    protected DummyTimeSlidingNode newEmptyNode() {
      return null;
    }

    @Override
    protected long currentTimeMillis() {
      return dummyTimestamp;
    }

    public void setDummyTimestamp(long timestamp) {
      this.dummyTimestamp = timestamp;
    }
  }

  @Test
  public void testContinuousTime() {
    DummyTimeSlidingHub hub = new DummyTimeSlidingHub(3);

    hub.setDummyTimestamp(0L);
    hub.add(new DummyTimeSlidingHub.DummyTimeSlidingNode(1));
    Assert.assertEquals(1, hub.sum().getLeft().getValue());

    hub.setDummyTimestamp(1000L);
    hub.add(new DummyTimeSlidingHub.DummyTimeSlidingNode(2));
    Assert.assertEquals(3, hub.sum().getLeft().getValue());

    hub.setDummyTimestamp(2200L);
    hub.add(new DummyTimeSlidingHub.DummyTimeSlidingNode(3));
    Assert.assertEquals(6, hub.sum().getLeft().getValue());

    hub.setDummyTimestamp(2400L);
    hub.add(new DummyTimeSlidingHub.DummyTimeSlidingNode(4));
    Assert.assertEquals(10, hub.sum().getLeft().getValue());

    // Should remove the value 1
    hub.setDummyTimestamp(3000L);
    hub.add(new DummyTimeSlidingHub.DummyTimeSlidingNode(5));
    Assert.assertEquals(14, hub.sum().getLeft().getValue());

    // Should remove the value 2
    hub.setDummyTimestamp(4000L);
    hub.add(new DummyTimeSlidingHub.DummyTimeSlidingNode(6));
    Assert.assertEquals(18, hub.sum().getLeft().getValue());

    // Should remove the value 3 and 4
    hub.setDummyTimestamp(5000L);
    hub.add(new DummyTimeSlidingHub.DummyTimeSlidingNode(7));
    Assert.assertEquals(18, hub.sum().getLeft().getValue());
  }

  @Test
  public void testTimeExceedTimeWindow() {
    DummyTimeSlidingHub hub = new DummyTimeSlidingHub(3);

    hub.setDummyTimestamp(0L);
    hub.add(new DummyTimeSlidingHub.DummyTimeSlidingNode(1));
    Assert.assertEquals(1, hub.sum().getLeft().getValue());

    hub.setDummyTimestamp(10000L);
    hub.add(new DummyTimeSlidingHub.DummyTimeSlidingNode(2));
    Assert.assertEquals(2, hub.sum().getLeft().getValue());
  }
}
