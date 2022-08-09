/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
* http://www.apache.org/licenses/LICENSE-2.0
 * 
* Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.ambari.server.testing;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

/**
 * 
 * Monitoring of deadlocks thread
 * Please note. This class can not be used outside of tests
 */
public class DeadlockWarningThread extends Thread {

  private final List<String> errorMessages;
  private int MAX_STACK_DEPTH = 30;
  private int SLEEP_TIME_MS = 3000;
  private Collection<Thread> monitoredThreads = null;
  private boolean deadlocked = false;
  private static final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();

  public List<String> getErrorMessages() {
    return errorMessages;
  }

  public boolean isDeadlocked() {
    return deadlocked;
  }

  public DeadlockWarningThread(Collection<Thread> monitoredThreads, int maxStackDepth, int sleepTimeMS) {
    this.errorMessages = new ArrayList<>();
    this.monitoredThreads = monitoredThreads;
    this.MAX_STACK_DEPTH = maxStackDepth;
    this.SLEEP_TIME_MS = sleepTimeMS;
    start();
  }

  public DeadlockWarningThread(Collection<Thread> monitoredThreads) {
    this(monitoredThreads, 30, 3000);
  }

  public String getThreadsStacktraces(Collection<Long> ids) {
    StringBuilder errBuilder = new StringBuilder();
      for (long id : ids) {
        ThreadInfo ti = mbean.getThreadInfo(id, MAX_STACK_DEPTH);
        errBuilder.append("Deadlocked Thread:\n").
                append("------------------\n").
                append(ti).append('\n');
        for (StackTraceElement ste : ti.getStackTrace()) {
          errBuilder.append('\t').append(ste);
        }
        errBuilder.append('\n');
      }
    return errBuilder.toString();
  }
 
  
  @Override
  public void run() {
    while (true) {
      try {
        Thread.sleep(SLEEP_TIME_MS);
      } catch (InterruptedException ex) {
      }
      long[] ids = mbean.findMonitorDeadlockedThreads();
      StringBuilder errBuilder = new StringBuilder();
      if (ids != null && ids.length > 0) {
          errBuilder.append(getThreadsStacktraces(Arrays.asList(ArrayUtils.toObject(ids))));
          errorMessages.add(errBuilder.toString());
          System.out.append(errBuilder.toString());
         //Exit if deadlocks have been found         
          deadlocked = true;
          break;
      } else {
        //Exit if all monitored threads were finished
        boolean hasLive = false;
        boolean hasRunning = false;
        for (Thread monTh : monitoredThreads) {
          State state = monTh.getState();
          if (state != State.TERMINATED && state != State.NEW) {
            hasLive = true;
          }
          if (state == State.RUNNABLE || state == State.TIMED_WAITING) {
            hasRunning = true;
            break;
          }
        }

        if (!hasLive) {
          deadlocked = false;
          break;
        } else if (!hasRunning) {
          List<Long> tIds = new ArrayList<>();
          for (Thread monitoredThread : monitoredThreads) {
            State state = monitoredThread.getState();
            if (state == State.WAITING || state == State.BLOCKED) {
              tIds.add(monitoredThread.getId());
            }
          }
          errBuilder.append(getThreadsStacktraces(tIds));
          errorMessages.add(errBuilder.toString());
          deadlocked = true;
          break;
        }
      }
    }
  }  
}
