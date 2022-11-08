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
 *
 * Original Files: alibaba/DataX (https://github.com/alibaba/DataX)
 * Copyright: Copyright 1999-2022 Alibaba Group Holding Ltd.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.base.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created at 2018/5/21.
 */
public class VMInfo {
  private static final int MILL_TO_SECOND = 1000;
  public static final Object LOCK = new Object();
  static final long MB = 1024 * 1024;
  static final long GB = 1024 * 1024 * 1024;
  private static final Logger LOG = LoggerFactory.getLogger(VMInfo.class);
  private static VMInfo vmInfo;
  private final OperatingSystemMXBean osMXBean;
  private final RuntimeMXBean runtimeMXBean;
  private final List<GarbageCollectorMXBean> garbageCollectorMXBeanList;
  private final List<MemoryPoolMXBean> memoryPoolMXBeanList;
  private final String osInfo;
  private final String jvmInfo;
  private final int totalProcessorCount;
  private final PhyOSStatus startPhyOSStatus;
  private final ProcessCpuStatus processCpuStatus = new ProcessCpuStatus();
  private final ProcessGCStatus processGCStatus = new ProcessGCStatus();
  private final ProcessMemoryStatus processMemoryStatus = new ProcessMemoryStatus();
  //ms
  private long lastUpTime = 0;
  //nano
  private long lastProcessCpuTime = 0;
  private VMInfo() {
    osMXBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
    runtimeMXBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
    garbageCollectorMXBeanList = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
    memoryPoolMXBeanList = java.lang.management.ManagementFactory.getMemoryPoolMXBeans();

    osInfo = runtimeMXBean.getVmVendor() + " " + runtimeMXBean.getSpecVersion() + " " + runtimeMXBean.getVmVersion();
    jvmInfo = osMXBean.getName() + " " + osMXBean.getArch() + " " + osMXBean.getVersion();
    totalProcessorCount = osMXBean.getAvailableProcessors();

    startPhyOSStatus = new PhyOSStatus();
    LOG.info("VMInfo# operatingSystem class => " + osMXBean.getClass().getName());
    if (VMInfo.isSunOsMBean(osMXBean)) {
      {
        startPhyOSStatus.totalPhysicalMemory = VMInfo.getLongFromOperatingSystem(osMXBean, "getTotalPhysicalMemorySize");
        startPhyOSStatus.freePhysicalMemory = VMInfo.getLongFromOperatingSystem(osMXBean, "getFreePhysicalMemorySize");
        startPhyOSStatus.maxFileDescriptorCount = VMInfo.getLongFromOperatingSystem(osMXBean, "getMaxFileDescriptorCount");
        startPhyOSStatus.currentOpenFileDescriptorCount = VMInfo.getLongFromOperatingSystem(osMXBean, "getOpenFileDescriptorCount");
      }
    }

    for (GarbageCollectorMXBean garbage : garbageCollectorMXBeanList) {
      GCStatus gcStatus = new GCStatus();
      gcStatus.name = garbage.getName();
      processGCStatus.gcStatusMap.put(garbage.getName(), gcStatus);
    }

    if (memoryPoolMXBeanList != null && !memoryPoolMXBeanList.isEmpty()) {
      for (MemoryPoolMXBean pool : memoryPoolMXBeanList) {
        MemoryStatus memoryStatus = new MemoryStatus();
        memoryStatus.name = pool.getName();
        memoryStatus.initSize = pool.getUsage().getInit();
        memoryStatus.maxSize = pool.getUsage().getMax();
        processMemoryStatus.memoryStatusMap.put(pool.getName(), memoryStatus);
      }
    }
  }

  /**
   * @return null or vmInfo. null is something error, job no care it.
   */
  public static VMInfo getVmInfo() {
    if (vmInfo == null) {
      synchronized (LOCK) {
        if (vmInfo == null) {
          try {
            vmInfo = new VMInfo();
          } catch (Exception e) {
            LOG.warn("no need care, the fail is ignored : vmInfo init failed " + e.getMessage(), e);
          }
        }
      }

    }
    return vmInfo;
  }

  public static boolean isSunOsMBean(OperatingSystemMXBean operatingSystem) {
    final String className = operatingSystem.getClass().getName();

    return "com.sun.management.UnixOperatingSystem".equals(className);
  }

  public static long getLongFromOperatingSystem(OperatingSystemMXBean operatingSystem, String methodName) {
    try {
      final Method method = operatingSystem.getClass().getMethod(methodName, (Class<?>[]) null);
      method.setAccessible(true);
      return (Long) method.invoke(operatingSystem, (Object[]) null);
    } catch (final Exception e) {
      LOG.info(String.format("OperatingSystemMXBean %s failed, Exception = %s ", methodName, e.getMessage()));
    }

    return -1;
  }

  public static MemoryUsage getHeapMemoryUsage() {
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    return memoryMXBean.getHeapMemoryUsage();
  }

  @Override
  public String toString() {
    return "the machine info  => \n\n"
        + "\tosInfo:\t" + osInfo + "\n"
        + "\tjvmInfo:\t" + jvmInfo + "\n"
        + "\tcpu num:\t" + totalProcessorCount + "\n\n"
        + startPhyOSStatus.toString() + "\n"
        + processGCStatus + "\n"
        + processMemoryStatus + "\n";
  }

  public String totalString() {
    return (processCpuStatus.getTotalString() + processGCStatus.getTotalString());
  }

  private static class PhyOSStatus {
    long totalPhysicalMemory = -1;
    long freePhysicalMemory = -1;
    long maxFileDescriptorCount = -1;
    long currentOpenFileDescriptorCount = -1;

    @Override
    public String toString() {
      return String.format("\ttotalPhysicalMemory:\t%,.2fG\n"
              + "\tfreePhysicalMemory:\t%,.2fG\n"
              + "\tmaxFileDescriptorCount:\t%s\n"
              + "\tcurrentOpenFileDescriptorCount:\t%s\n",
          (float) totalPhysicalMemory / GB, (float) freePhysicalMemory / GB, maxFileDescriptorCount, currentOpenFileDescriptorCount);
    }
  }

  private static class ProcessGCStatus {
    final Map<String, GCStatus> gcStatusMap = new HashMap<>();

    public String toString() {
      return "\tGC Names\t" + gcStatusMap.keySet() + "\n";
    }

    public String getDeltaString() {
      StringBuilder sb = new StringBuilder();
      sb.append("\n\t [delta gc info] => \n");
      sb.append("\t\t ");
      sb.append(String.format("%-20s | %-18s | %-18s | %-18s | %-18s | %-18s | %-18s | %-18s | %-18s \n", "NAME",
          "curDeltaGCCount", "totalGCCount", "maxDeltaGCCount", "minDeltaGCCount", "curDeltaGCTime", "totalGCTime",
          "maxDeltaGCTime", "minDeltaGCTime"));
      for (GCStatus gc : gcStatusMap.values()) {
        sb.append("\t\t ");
        sb.append(String.format("%-20s | %-18s | %-18s | %-18s | %-18s | %-18s | %-18s | %-18s | %-18s \n",
            gc.name, gc.curDeltaGCCount, gc.totalGCCount, gc.maxDeltaGCCount, gc.minDeltaGCCount,
            String.format("%,.3fs", (float) gc.curDeltaGCTime / MILL_TO_SECOND),
            String.format("%,.3fs", (float) gc.totalGCTime / MILL_TO_SECOND),
            String.format("%,.3fs", (float) gc.maxDeltaGCTime / MILL_TO_SECOND),
            String.format("%,.3fs", (float) gc.minDeltaGCTime / MILL_TO_SECOND)));

      }
      return sb.toString();
    }

    public String getTotalString() {
      StringBuilder sb = new StringBuilder();
      sb.append("\n\t [total gc info] => \n");
      sb.append("\t\t ");
      sb.append(String.format("%-20s | %-18s | %-18s | %-18s | %-18s | %-18s | %-18s \n", "NAME", "totalGCCount",
          "maxDeltaGCCount", "minDeltaGCCount", "totalGCTime", "maxDeltaGCTime", "minDeltaGCTime"));
      for (GCStatus gc : gcStatusMap.values()) {
        sb.append("\t\t ");
        sb.append(String.format("%-20s | %-18s | %-18s | %-18s | %-18s | %-18s | %-18s \n",
            gc.name, gc.totalGCCount, gc.maxDeltaGCCount, gc.minDeltaGCCount,
            String.format("%,.3fs", (float) gc.totalGCTime / MILL_TO_SECOND),
            String.format("%,.3fs", (float) gc.maxDeltaGCTime / MILL_TO_SECOND),
            String.format("%,.3fs", (float) gc.minDeltaGCTime / MILL_TO_SECOND)));

      }
      return sb.toString();
    }
  }

  private static class ProcessMemoryStatus {
    final Map<String, MemoryStatus> memoryStatusMap = new HashMap<>();

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("\t");
      sb.append(String.format("%-30s | %-30s | %-30s \n", "MEMORY_NAME", "allocation_size", "init_size"));
      for (MemoryStatus ms : memoryStatusMap.values()) {
        sb.append("\t");
        sb.append(String.format("%-30s | %-30s | %-30s \n",
            ms.name, String.format("%,.2fMB", (float) ms.maxSize / MB), String.format("%,.2fMB", (float) ms.initSize / MB)));
      }
      return sb.toString();
    }

    public String getDeltaString() {
      StringBuilder sb = new StringBuilder();
      sb.append("\n\t [delta memory info] => \n");
      sb.append("\t\t ");
      sb.append(String.format("%-30s | %-30s | %-30s | %-30s | %-30s \n", "NAME", "used_size", "used_percent", "max_used_size", "max_percent"));
      for (MemoryStatus ms : memoryStatusMap.values()) {
        sb.append("\t\t ");
        sb.append(String.format("%-30s | %-30s | %-30s | %-30s | %-30s \n",
            ms.name, String.format("%,.2f", (float) ms.usedSize / MB) + "MB",
            String.format("%,.2f", ms.percent) + "%",
            String.format("%,.2f", (float) ms.maxUsedSize / MB) + "MB",
            String.format("%,.2f", ms.maxPercent) + "%"));

      }
      return sb.toString();
    }
  }

  private static class GCStatus {
    String name;
    long maxDeltaGCCount = -1;
    long minDeltaGCCount = -1;
    long curDeltaGCCount;
    long totalGCCount = 0;
    long maxDeltaGCTime = -1;
    long minDeltaGCTime = -1;
    long curDeltaGCTime;
    long totalGCTime = 0;

    public void setCurTotalGcCount(long curTotalGcCount) {
      this.curDeltaGCCount = curTotalGcCount - totalGCCount;
      this.totalGCCount = curTotalGcCount;

      if (maxDeltaGCCount < curDeltaGCCount) {
        maxDeltaGCCount = curDeltaGCCount;
      }

      if (minDeltaGCCount == -1 || minDeltaGCCount > curDeltaGCCount) {
        minDeltaGCCount = curDeltaGCCount;
      }
    }

    public void setCurTotalGcTime(long curTotalGcTime) {
      this.curDeltaGCTime = curTotalGcTime - totalGCTime;
      this.totalGCTime = curTotalGcTime;

      if (maxDeltaGCTime < curDeltaGCTime) {
        maxDeltaGCTime = curDeltaGCTime;
      }

      if (minDeltaGCTime == -1 || minDeltaGCTime > curDeltaGCTime) {
        minDeltaGCTime = curDeltaGCTime;
      }
    }
  }

  private static class MemoryStatus {
    String name;
    long initSize;
    long maxSize;
    long committedSize;
    long usedSize;
    float percent;
    long maxUsedSize = -1;
    float maxPercent = 0;

    void setMaxMinUsedSize(long curUsedSize) {
      if (maxUsedSize < curUsedSize) {
        maxUsedSize = curUsedSize;
      }
      this.usedSize = curUsedSize;
    }

    void setMaxMinPercent(float curPercent) {
      if (maxPercent < curPercent) {
        maxPercent = curPercent;
      }
      this.percent = curPercent;
    }
  }

  private class ProcessCpuStatus {
    float maxDeltaCpu = -1;
    float minDeltaCpu = -1;
    float curDeltaCpu = -1;
    float averageCpu = -1;

    public void setMaxMinCpu(float curCpu) {
      this.curDeltaCpu = curCpu;
      if (maxDeltaCpu < curCpu) {
        maxDeltaCpu = curCpu;
      }

      if (minDeltaCpu == -1 || minDeltaCpu > curCpu) {
        minDeltaCpu = curCpu;
      }
    }

    public String getDeltaString() {
      StringBuilder sb = new StringBuilder();
      sb.append("\n\t [delta cpu info] => \n");
      sb.append("\t\t");
      sb.append(String.format("%-30s | %-30s | %-30s | %-30s \n", "curDeltaCpu", "averageCpu", "maxDeltaCpu", "minDeltaCpu"));
      sb.append("\t\t");
      sb.append(String.format("%-30s | %-30s | %-30s | %-30s \n",
          String.format("%,.2f%%", processCpuStatus.curDeltaCpu),
          String.format("%,.2f%%", processCpuStatus.averageCpu),
          String.format("%,.2f%%", processCpuStatus.maxDeltaCpu),
          String.format("%,.2f%%\n", processCpuStatus.minDeltaCpu)));

      return sb.toString();
    }

    public String getTotalString() {
      StringBuilder sb = new StringBuilder();
      sb.append("\n\t [total cpu info] => \n");
      sb.append("\t\t");
      sb.append(String.format("%-30s | %-30s | %-30s \n", "averageCpu", "maxDeltaCpu", "minDeltaCpu"));
      sb.append("\t\t");
      sb.append(String.format("%-30s | %-30s | %-30s \n",
          String.format("%,.2f%%", processCpuStatus.averageCpu),
          String.format("%,.2f%%", processCpuStatus.maxDeltaCpu),
          String.format("%,.2f%%\n", processCpuStatus.minDeltaCpu)));

      return sb.toString();
    }

  }

}