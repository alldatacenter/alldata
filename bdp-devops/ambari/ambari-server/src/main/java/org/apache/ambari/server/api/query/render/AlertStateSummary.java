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

import org.apache.ambari.server.state.AlertState;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * The {@link AlertStateSummary} class holds information about each possible
 * alert state.
 */
public final class AlertStateSummary {
  /**
   * The {@link AlertState#OK} state information.
   */
  @JsonProperty(value = "OK")
  @com.fasterxml.jackson.annotation.JsonProperty(value = "OK")
  public final AlertStateValues Ok = new AlertStateValues();

  /**
   * The {@link AlertState#WARNING} state information.
   */
  @JsonProperty(value = "WARNING")
  @com.fasterxml.jackson.annotation.JsonProperty(value = "WARNING")
  public final AlertStateValues Warning = new AlertStateValues();

  /**
   * The {@link AlertState#CRITICAL} state information.
   */
  @JsonProperty(value = "CRITICAL")
  @com.fasterxml.jackson.annotation.JsonProperty(value = "CRITICAL")
  public final AlertStateValues Critical = new AlertStateValues();

  /**
   * The {@link AlertState#UNKNOWN} state information.
   */
  @JsonProperty(value = "UNKNOWN")
  @com.fasterxml.jackson.annotation.JsonProperty(value = "UNKNOWN")
  public final AlertStateValues Unknown = new AlertStateValues();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AlertStateSummary that = (AlertStateSummary) o;

    if (Ok != null ? !Ok.equals(that.Ok) : that.Ok != null) return false;
    if (Warning != null ? !Warning.equals(that.Warning) : that.Warning != null) return false;
    if (Critical != null ? !Critical.equals(that.Critical) : that.Critical != null) return false;
    return Unknown != null ? Unknown.equals(that.Unknown) : that.Unknown == null;
  }

  @Override
  public int hashCode() {
    int result = Ok != null ? Ok.hashCode() : 0;
    result = 31 * result + (Warning != null ? Warning.hashCode() : 0);
    result = 31 * result + (Critical != null ? Critical.hashCode() : 0);
    result = 31 * result + (Unknown != null ? Unknown.hashCode() : 0);
    return result;
  }
}
