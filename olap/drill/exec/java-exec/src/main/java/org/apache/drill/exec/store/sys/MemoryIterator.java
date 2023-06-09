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


import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.pojo.NonNullable;

public class MemoryIterator implements Iterator<Object> {

  private boolean beforeFirst = true;
  private final ExecutorFragmentContext context;

  public MemoryIterator(final ExecutorFragmentContext context) {
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
    final MemoryInfo memoryInfo = new MemoryInfo();

    final DrillbitEndpoint endpoint = context.getEndpoint();
    memoryInfo.hostname = endpoint.getAddress();
    memoryInfo.user_port = endpoint.getUserPort();

    final MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    memoryInfo.heap_current = heapMemoryUsage.getUsed();
    memoryInfo.heap_max = heapMemoryUsage.getMax();

    BufferPoolMXBean directBean = getDirectBean();
    memoryInfo.jvm_direct_current = directBean.getMemoryUsed();

    // We need the memory used by the root allocator for the Drillbit
    memoryInfo.direct_current = context.getRootAllocator().getAllocatedMemory();
    memoryInfo.direct_max = DrillConfig.getMaxDirectMemory();
    return memoryInfo;
  }

  private BufferPoolMXBean getDirectBean() {
    List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
    for (BufferPoolMXBean b : pools) {
      if (b.getName().equals("direct")) {
        return b;
      }
    }
    throw new IllegalStateException("Unable to find direct buffer bean.  JVM must be too old.");
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  public static class MemoryInfo {
    @NonNullable
    public String hostname;
    public long user_port;
    public long heap_current;
    public long heap_max;
    public long direct_current;
    public long jvm_direct_current;
    public long direct_max;
  }
}
