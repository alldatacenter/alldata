/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

/**
 *
 */
package com.qlangtech.tis.trigger.biz.dal.dao;

import java.util.ArrayList;
import java.util.List;

/**
 * @date 2012-7-31
 */
public class AppTrigger {
  private final TriggerJob fullTrigger;
  private final TriggerJob incTrigger;

  public AppTrigger(
    TriggerJob fullTrigger,
    TriggerJob incTrigger) {
    super();
    this.fullTrigger = fullTrigger;
    this.incTrigger = incTrigger;
  }

  public TriggerJob getFullTrigger() {
    return fullTrigger;
  }

  public TriggerJob getIncTrigger() {
    return incTrigger;
  }

  /**
   * dump是否是停止的状态
   *
   * @return
   */
  public boolean isPause() {
    if (fullTrigger != null && !fullTrigger.isStop()) {
      return false;
    }

    if (incTrigger != null && !incTrigger.isStop()) {
      return false;
    }

    return true;
  }

  public List<Long> getJobsId() {
    final List<Long> jobs = new ArrayList<Long>();
    if (this.getFullTrigger() != null) {
      jobs.add(this.getFullTrigger().getJobId());
    }

    if (this.getIncTrigger() != null) {
      jobs.add(this.getIncTrigger().getJobId());
    }
    return jobs;
  }

}
