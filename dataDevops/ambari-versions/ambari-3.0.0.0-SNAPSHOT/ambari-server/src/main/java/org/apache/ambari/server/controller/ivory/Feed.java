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

import java.util.Map;

/**
 * Ivory feed.
 */
public class Feed {

  private final String name;
  private final String description;
  private final String status;
  private final String schedule;
  private final String sourceClusterName;
  private final String sourceClusterStart;
  private final String sourceClusterEnd;
  private final String sourceClusterLimit;
  private final String sourceClusterAction;
  private final String targetClusterName;
  private final String targetClusterStart;
  private final String targetClusterEnd;
  private final String targetClusterLimit;
  private final String targetClusterAction;
  private final Map<String, String> properties;

  /**
   * Construct a feed.
   *
   * @param name                 the feed name
   * @param description          the description
   * @param status               the status
   * @param schedule             the schedule
   * @param sourceClusterName    the source cluster name
   * @param sourceClusterStart   the source cluster validity start time
   * @param sourceClusterEnd     the source cluster validity end time
   * @param sourceClusterLimit   the source cluster retention limit
   * @param sourceClusterAction  the source cluster retention action
   * @param targetClusterName    the target cluster name
   * @param targetClusterStart   the target cluster validity start time
   * @param targetClusterEnd     the target cluster validity end time
   * @param targetClusterLimit   the target cluster retention limit
   * @param targetClusterAction  the target cluster retention action
   * @param properties           the properties

   */
  public Feed(String name, String description, String status, String schedule,
              String sourceClusterName, String sourceClusterStart, String sourceClusterEnd,
              String sourceClusterLimit, String sourceClusterAction,
              String targetClusterName, String targetClusterStart, String targetClusterEnd,
              String targetClusterLimit, String targetClusterAction,
              Map<String, String> properties) {
    this.name = name;
    this.description = description;
    this.status = status;
    this.schedule = schedule;
    this.sourceClusterName = sourceClusterName;
    this.sourceClusterStart = sourceClusterStart;
    this.sourceClusterEnd = sourceClusterEnd;
    this.sourceClusterLimit = sourceClusterLimit;
    this.sourceClusterAction = sourceClusterAction;
    this.targetClusterName = targetClusterName;
    this.targetClusterStart = targetClusterStart;
    this.targetClusterEnd = targetClusterEnd;
    this.targetClusterLimit = targetClusterLimit;
    this.targetClusterAction = targetClusterAction;
    this.properties = properties;
  }

  /**
   * Get the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
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
   * Get the schedule.
   *
   * @return the schedule
   */
  public String getSchedule() {
    return schedule;
  }

  /**
   * Get the source cluster name.
   *
   * @return the source cluster name
   */
  public String getSourceClusterName() {
    return sourceClusterName;
  }

  /**
   * Get the source cluster validity start time.
   *
   * @return  the source cluster validity start time
   */
  public String getSourceClusterStart() {
    return sourceClusterStart;
  }

  /**
   * Get the source cluster validity end time.
   *
   * @return  the source cluster validity end time
   */
  public String getSourceClusterEnd() {
    return sourceClusterEnd;
  }

  /**
   * Get the source cluster retention limit.
   *
   * @return the source cluster retention limit
   */
  public String getSourceClusterLimit() {
    return sourceClusterLimit;
  }

  /**
   * Get the source cluster retention action.
   *
   * @return the source cluster retention action
   */
  public String getSourceClusterAction() {
    return sourceClusterAction;
  }

  /**
   * Get the target cluster name.
   *
   * @return the target cluster name
   */
  public String getTargetClusterName() {
    return targetClusterName;
  }

  /**
   * Get the target cluster validity start time.
   *
   * @return  the target cluster validity start time
   */
  public String getTargetClusterStart() {
    return targetClusterStart;
  }

  /**
   * Get the target cluster validity end time.
   *
   * @return  the target cluster validity end time
   */
  public String getTargetClusterEnd() {
    return targetClusterEnd;
  }

  /**
   * Get the target cluster retention limit.
   *
   * @return the target cluster retention limit
   */
  public String getTargetClusterLimit() {
    return targetClusterLimit;
  }

  /**
   * Get the target cluster retention action.
   *
   * @return the target cluster retention action
   */
  public String getTargetClusterAction() {
    return targetClusterAction;
  }

  /**
   * Get the properties.
   *
   * @return the properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Feed feed = (Feed) o;

    return !(description != null ? !description.equals(feed.description) : feed.description != null) &&
        !(name != null              ? !name.equals(feed.name)                           : feed.name != null) &&
        !(schedule != null          ? !schedule.equals(feed.schedule)                   : feed.schedule != null) &&
        !(sourceClusterName != null ? !sourceClusterName.equals(feed.sourceClusterName) : feed.sourceClusterName != null) &&
        !(status != null            ? !status.equals(feed.status)                       : feed.status != null) &&
        !(targetClusterName != null ? !targetClusterName.equals(feed.targetClusterName) : feed.targetClusterName != null);

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (schedule != null ? schedule.hashCode() : 0);
    result = 31 * result + (sourceClusterName != null ? sourceClusterName.hashCode() : 0);
    result = 31 * result + (targetClusterName != null ? targetClusterName.hashCode() : 0);
    return result;
  }
}
