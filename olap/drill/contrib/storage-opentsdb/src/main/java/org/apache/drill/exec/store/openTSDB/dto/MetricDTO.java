/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.openTSDB.dto;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MetricDTO {

  private String metric;
  private Map<String, String> tags;
  private List<String> aggregateTags;
  private Map<String, String> dps;

  public String getMetric() {
    return metric;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public List<String> getAggregateTags() {
    return aggregateTags;
  }

  public Map<String, String> getDps() {
    return dps;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MetricDTO metricDTO = (MetricDTO) o;
    return Objects.equals(metric, metricDTO.metric) &&
        Objects.equals(tags, metricDTO.tags) &&
        Objects.equals(aggregateTags, metricDTO.aggregateTags) &&
        Objects.equals(dps, metricDTO.dps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metric, tags, aggregateTags, dps);
  }

  @Override
  public String toString() {
    return "Table{" +
        "metric='" + metric + '\'' +
        ", tags=" + tags +
        ", aggregateTags=" + aggregateTags +
        ", dps=" + dps +
        '}';
  }
}
