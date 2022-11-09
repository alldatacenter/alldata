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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;

import java.util.concurrent.TimeUnit;

/**
 * Record number of code call and execution latency in try block.
 */
public class CallTracer implements AutoCloseable {

  final Counter throughput;
  final Timer latency;
  long startTimeNs;
  long elapsedNs;

  public CallTracer(Counter throughput, Timer latency) {
    this.throughput = throughput;
    this.latency = latency;
    this.startTimeNs = System.nanoTime();
  }

  @Override
  public void close() {
    if (startTimeNs > 0) {
      elapsedNs = System.nanoTime() - startTimeNs;
      latency.update(elapsedNs, TimeUnit.NANOSECONDS);
      throughput.inc();
      startTimeNs = 0;
    }
  }
}

