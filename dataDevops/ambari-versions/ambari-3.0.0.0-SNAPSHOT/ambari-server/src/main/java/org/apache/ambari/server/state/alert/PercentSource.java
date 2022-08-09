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
package org.apache.ambari.server.state.alert;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Alert when the source type is defined as {@link SourceType#PERCENT}
 * <p/>
 * Equality checking for instances of this class should be executed on every
 * member to ensure that reconciling stack differences is correct.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PercentSource extends Source {

  @SerializedName("numerator")
  private MetricFractionPart m_numerator = null;

  @SerializedName("denominator")
  private MetricFractionPart m_denominator = null;

  /**
   * Gets the numerator for the percent calculation.
   *
   * @return a metric value representing the numerator (never {@code null}).
   */
  @JsonProperty("numerator")
  public MetricFractionPart getNumerator() {
    return m_numerator;
  }

  /**
   * Gets the denomintor for the percent calculation.
   *
   * @return a metric value representing the denominator (never {@code null}).
   */
  @JsonProperty("denominator")
  public MetricFractionPart getDenominator() {
    return m_denominator;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), m_denominator, m_numerator);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!super.equals(obj)) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    PercentSource other = (PercentSource) obj;
    return Objects.equals(m_denominator, other.m_denominator) &&
      Objects.equals(m_numerator, other.m_numerator);
  }

  /**
   * The {@link MetricFractionPart} class represents either the numerator or the
   * denominator of a fraction.
   * <p/>
   * Equality checking for instances of this class should be executed on every
   * member to ensure that reconciling stack differences is correct.
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public static final class MetricFractionPart {
    @SerializedName("jmx")
    private String m_jmxInfo = null;

    @SerializedName("ganglia")
    private String m_gangliaInfo = null;

    /**
     * @return the jmx info, if this metric is jmx-based
     */
    @JsonProperty("jmx")
    public String getJmxInfo() {
      return m_jmxInfo;
    }

    /**
     * @return the ganglia info, if this metric is ganglia-based
     */
    @JsonProperty("ganglia")
    public String getGangliaInfo() {
      return m_gangliaInfo;
    }

    @Override
    public int hashCode() {
      return Objects.hash(m_gangliaInfo, m_jmxInfo);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      MetricFractionPart other = (MetricFractionPart) obj;
      return Objects.equals(m_gangliaInfo, other.m_gangliaInfo) &&
        Objects.equals(m_jmxInfo, other.m_jmxInfo);
    }

  }
}
