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

package com.bytedance.bitsail.base.metrics;

import java.util.concurrent.TimeUnit;

/**
 * Interface for reporters that actively send out data periodically.
 */
public interface Scheduled {

  /**
   * return schedule delay time
   *
   * @return the delay between the termination of one
   * execution and the commencement of the next
   */
  default long getDelay() {
    return 1;
  }

  /**
   * return schedule delay time unit
   *
   * @return time unit
   */
  default TimeUnit getDelayTimeUnit() {
    return TimeUnit.MINUTES;
  }
}
