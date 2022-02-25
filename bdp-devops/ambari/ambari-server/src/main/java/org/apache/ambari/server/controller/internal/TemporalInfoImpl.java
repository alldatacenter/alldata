/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import java.util.Date;

import org.apache.ambari.server.controller.spi.TemporalInfo;

/**
* Temporal query data.
*/
public class TemporalInfoImpl implements TemporalInfo {
  private final long m_startTime;
  private final long m_endTime;
  private final long m_step;
  private final long startTimeMillis;
  private final long endTimeMillis;

  public TemporalInfoImpl(long startTime, long endTime, long step) {
    m_startTime = startTime;
    m_endTime = endTime;
    m_step = step;

    if (startTime < 9999999999l) {
      startTimeMillis = startTime * 1000;
    } else {
      startTimeMillis = startTime;
    }

    if (endTime < 9999999999l) {
      endTimeMillis = endTime * 1000;
    } else {
      endTimeMillis = endTime;
    }
  }

  @Override
  public Long getStartTime() {
    return m_startTime;
  }

  @Override
  public Long getEndTime() {
    return m_endTime;
  }

  @Override
  public Long getStep() {
    return m_step;
  }

  @Override
  public Long getStartTimeMillis() {
    return startTimeMillis;
  }

  @Override
  public Long getEndTimeMillis() {
    return endTimeMillis;
  }

  @Override
  public String toString() {
    return "TemporalInfoImpl{" +
      "m_startTime = " + new Date(getStartTimeMillis()) +
      ", m_endTime = " + new Date(getEndTimeMillis()) +
      ", m_step = " + m_step +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TemporalInfoImpl that = (TemporalInfoImpl) o;
    return m_endTime == that.m_endTime &&
           m_startTime == that.m_startTime &&
           m_step == that.m_step;
  }

  @Override
  public int hashCode() {
    int result = (int) (m_startTime ^ (m_startTime >>> 32));
    result = 31 * result + (int) (m_endTime ^ (m_endTime >>> 32));
    result = 31 * result + (int) (m_step ^ (m_step >>> 32));
    return result;
  }
}
