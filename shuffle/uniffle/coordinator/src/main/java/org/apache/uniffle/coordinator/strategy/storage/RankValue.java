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

package org.apache.uniffle.coordinator.strategy.storage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The basis for app to select remote storage ranking
 */
public class RankValue {
  AtomicLong costTime;
  AtomicInteger appNum;
  AtomicBoolean isHealthy;

  public RankValue(int appNum) {
    this.costTime = new AtomicLong(0);
    this.appNum = new AtomicInteger(appNum);
    this.isHealthy = new AtomicBoolean(true);
  }

  public RankValue(long costTime, int appNum) {
    this.costTime = new AtomicLong(costTime);
    this.appNum = new AtomicInteger(appNum);
    this.isHealthy = new AtomicBoolean(true);
  }

  public AtomicLong getCostTime() {
    return costTime;
  }

  public AtomicInteger getAppNum() {
    return appNum;
  }

  public AtomicBoolean getHealthy() {
    return isHealthy;
  }

  public void setCostTime(AtomicLong readAndWriteTime) {
    this.costTime = readAndWriteTime;
  }

  public void setHealthy(AtomicBoolean isHealthy) {
    this.isHealthy = isHealthy;
    if (!isHealthy.get()) {
      this.costTime.set(Long.MAX_VALUE);
    }
  }

  @Override
  public String toString() {
    return "RankValue{"
        + "costTime=" + costTime
        + ", appNum=" + appNum
        + ", isHealthy=" + isHealthy + '}';
  }
}
