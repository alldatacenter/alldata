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
package org.apache.drill.exec.store.sys;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Iterator;

import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.pojo.NonNullable;

public class ThreadsIterator implements Iterator<Object> {

  private boolean beforeFirst = true;
  private final ExecutorFragmentContext context;

  public ThreadsIterator(final ExecutorFragmentContext context) {
    this.context = context;
  }

  @Override
  public boolean hasNext() {
    return beforeFirst;
  }

  @Override
  public Object next() {
    if (!beforeFirst) {
      throw new IllegalStateException();
    }
    beforeFirst = false;
    final ThreadsInfo threadsInfo = new ThreadsInfo();

    final DrillbitEndpoint endpoint = context.getEndpoint();
    threadsInfo.hostname = endpoint.getAddress();
    threadsInfo.user_port = endpoint.getUserPort();

    final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    threadsInfo.total_threads = threadMXBean.getPeakThreadCount();
    threadsInfo.busy_threads = threadMXBean.getThreadCount();
    return threadsInfo;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  public static class ThreadsInfo {
    @NonNullable
    public String hostname;
    public long user_port;
    public long total_threads;
    public long busy_threads;
  }
}
