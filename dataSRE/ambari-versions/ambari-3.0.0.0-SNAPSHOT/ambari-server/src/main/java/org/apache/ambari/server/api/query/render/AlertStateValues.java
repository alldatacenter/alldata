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
package org.apache.ambari.server.api.query.render;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The {@link AlertStateValues} class holds various information about an alert
 * state, such as the number of instances of that state and the most recent
 * timestamp.
 */
public final class AlertStateValues {
  /**
   * The total count of non-maintenance mode instances.
   */
  @JsonProperty(value = "count")
  @com.fasterxml.jackson.annotation.JsonProperty(value = "count")
  public int Count = 0;

  /**
   * The time of the last state change.
   */
  @JsonProperty(value = "original_timestamp")
  @com.fasterxml.jackson.annotation.JsonProperty(value = "original_timestamp")
  public long Timestamp = 0;

  /**
   * The total count of instances in maintenance mode.
   */
  @JsonProperty(value = "maintenance_count")
  @com.fasterxml.jackson.annotation.JsonProperty(value = "maintenance_count")
  public int MaintenanceCount = 0;

  /**
   * The most recently received text from any instance of the alert.
   */
  @JsonProperty(value = "latest_text")
  @com.fasterxml.jackson.annotation.JsonProperty(value = "latest_text")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String AlertText = null;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AlertStateValues that = (AlertStateValues) o;

    if (Count != that.Count) return false;
    if (Timestamp != that.Timestamp) return false;
    if (MaintenanceCount != that.MaintenanceCount) return false;
    return AlertText != null ? AlertText.equals(that.AlertText) : that.AlertText == null;
  }

  @Override
  public int hashCode() {
    int result = Count;
    result = 31 * result + (int) (Timestamp ^ (Timestamp >>> 32));
    result = 31 * result + MaintenanceCount;
    result = 31 * result + (AlertText != null ? AlertText.hashCode() : 0);
    return result;
  }
}
