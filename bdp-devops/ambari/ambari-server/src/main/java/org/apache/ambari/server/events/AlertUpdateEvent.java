/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;

/**
 * Contains info about alerts update. This update will be sent to all subscribed recipients.
 */
public class AlertUpdateEvent extends STOMPEvent {
  /**
   * Alert summaries grouped by cluster id.
   */
  private Map<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> summaries = new HashMap<>();

  public AlertUpdateEvent(Map<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> summaries) {
    super(Type.ALERT);
    this.summaries = summaries;
  }

  public Map<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> getSummaries() {
    return summaries;
  }

  public void setSummaries(Map<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> summaries) {
    this.summaries = summaries;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AlertUpdateEvent that = (AlertUpdateEvent) o;

    return summaries != null ? summaries.equals(that.summaries) : that.summaries == null;
  }

  @Override
  public int hashCode() {
    return summaries != null ? summaries.hashCode() : 0;
  }
}
