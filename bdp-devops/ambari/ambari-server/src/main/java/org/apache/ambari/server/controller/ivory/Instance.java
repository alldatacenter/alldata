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
package org.apache.ambari.server.controller.ivory;

/**
 * Ivory mirroring job instance.
 */
public class Instance {

  private final String feedName;
  private final String id;
  private final String status;
  private final String startTime;
  private final String endTime;
  private final String details;
  private final String log;

  /**
   * Construct an instance.
   *
   * @param feedName   the feed name
   * @param id         the id
   * @param status     the status
   * @param startTime  the start time
   * @param endTime    the end time
   * @param details    the details
   * @param log        the log
   */
  public Instance(String feedName, String id, String status, String startTime, String endTime, String details, String log) {
    this.feedName = feedName;
    this.id = id;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.details = details;
    this.log = log;
  }

  /**
   * Get the feed name.
   *
   * @return the feed name
   */
  public String getFeedName() {
    return feedName;
  }

  /**
   * Get the ID.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Get the status.
   *
   * @return the status
   */
  public String getStatus() {
    return status;
  }

  /**
   * Get the start time.
   *
   * @return the start time
   */
  public String getStartTime() {
    return startTime;
  }

  /**
   * Get the end time.
   *
   * @return the end time
   */
  public String getEndTime() {
    return endTime;
  }

  /**
   * Get the details.
   *
   * @return the details
   */
  public String getDetails() {
    return details;
  }

  /**
   * Get the log.
   *
   * @return the log
   */
  public String getLog() {
    return log;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Instance instance = (Instance) o;

    return !(details != null   ? !details.equals(instance.details)     : instance.details != null) &&
           !(endTime != null   ? !endTime.equals(instance.endTime)     : instance.endTime != null) &&
           !(feedName != null  ? !feedName.equals(instance.feedName)   : instance.feedName != null) &&
           !(id != null        ? !id.equals(instance.id)               : instance.id != null) &&
           !(log != null       ? !log.equals(instance.log)             : instance.log != null) &&
           !(startTime != null ? !startTime.equals(instance.startTime) : instance.startTime != null) &&
           !(status != null    ? !status.equals(instance.status)       : instance.status != null);
  }

  @Override
  public int hashCode() {
    int result = feedName != null ? feedName.hashCode() : 0;
    result = 31 * result + (id != null ? id.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    result = 31 * result + (details != null ? details.hashCode() : 0);
    result = 31 * result + (log != null ? log.hashCode() : 0);
    return result;
  }
}
