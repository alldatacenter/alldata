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

package com.bytedance.bitsail.base.metrics.manager;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformSnapshot;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.min;

/**
 * An optimized {@link Reservoir} based on {@link com.codahale.metrics.SlidingWindowReservoir}.
 * Use {@link AtomicLong} to replace {@code long} so that lock can be ignored.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class LockFreeSlidingWindowReservoir implements Reservoir {
  private final long[] measurements;
  private final AtomicLong count;
  private long sliceStart;

  /**
   * Creates a new {@link LockFreeSlidingWindowReservoir} which stores the last {@code size} measurements.
   *
   * @param size the number of measurements to store
   */
  public LockFreeSlidingWindowReservoir(int size) {
    this.measurements = new long[size];
    this.count = new AtomicLong(0);
    this.sliceStart = System.currentTimeMillis();
  }

  @Override
  public int size() {
    return (int) min(count.get(), measurements.length);
  }

  @Override
  public void update(long value) {
    measurements[((int) (count.getAndIncrement() & 0x7FFFFFFFL) % measurements.length)] = value;
  }

  @Override
  public Snapshot getSnapshot() {
    final long[] values = new long[size()];
    synchronized (this) {
      System.arraycopy(measurements, 0, values, 0, values.length);
    }
    long now = System.currentTimeMillis();
    if (now - sliceStart > 30000L) {
      sliceStart = now;
      count.set(0);
    }
    return new UniformSnapshot(values);
  }
}
