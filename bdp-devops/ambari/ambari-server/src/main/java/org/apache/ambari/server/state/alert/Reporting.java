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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.ambari.server.alerts.Threshold;
import org.apache.ambari.server.state.AlertState;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link Reporting} class represents the OK/WARNING/CRITICAL structures in
 * an {@link AlertDefinition}.
 * <p/>
 * Equality checking for instances of this class should be executed on every
 * member to ensure that reconciling stack differences is correct.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Reporting {

  /**
   * The OK template.
   */
  @SerializedName("ok")
  private ReportTemplate m_ok;

  /**
   * The WARNING template.
   */
  @SerializedName("warning")
  private ReportTemplate m_warning;

  /**
   * The CRITICAL template.
   */
  @SerializedName("critical")
  private ReportTemplate m_critical;

  /**
   * A label that identifies what units the value is in. For example, this could
   * be "s" for seconds or GB for "Gigabytes".
   */
  @SerializedName("units")
  private String m_units;

  @SerializedName("type")
  private ReportingType m_type;

  /**
   * @return the WARNING structure or {@code null} if none.
   */
  @JsonProperty("warning")
  public ReportTemplate getWarning() {
    return m_warning;
  }

  /**
   * @param warning
   *          the WARNING structure or {@code null} if none.
   */
  public void setWarning(ReportTemplate warning) {
    m_warning = warning;
  }

  /**
   * @return the CRITICAL structure or {@code null} if none.
   */
  @JsonProperty("critical")
  public ReportTemplate getCritical() {
    return m_critical;
  }

  /**
   * @param critical
   *          the CRITICAL structure or {@code null} if none.
   */
  public void setCritical(ReportTemplate critical) {
    m_critical = critical;
  }

  /**
   * @return the OK structure or {@code null} if none.
   */
  @JsonProperty("ok")
  public ReportTemplate getOk() {
    return m_ok;
  }

  /**
   * @param ok
   *          the OK structure or {@code null} if none.
   */
  public void setOk(ReportTemplate ok) {
    m_ok = ok;
  }

  /**
   * Gets a label identifying the units that the values are in. For example,
   * this could be "s" for seconds or GB for "Gigabytes".
   *
   * @return the units, or {@code null} for none.
   */
  @JsonProperty("units")
  public String getUnits() {
    return m_units;
  }

  /**
   * Sets the label that identifies the units that the threshold values are in.
   * For example, this could be "s" for seconds or GB for "Gigabytes".
   *
   * @param units
   *          the units, or {@code null} for none.
   */
  public void setUnits(String units) {
    m_units = units;
  }

  @JsonProperty("type")
  public ReportingType getType() {
    return m_type;
  }

  public void setType(ReportingType m_type) {
    this.m_type = m_type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_critical, m_ok, m_warning, m_type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Reporting other = (Reporting) obj;
    return Objects.equals(m_critical, other.m_critical) &&
      Objects.equals(m_ok, other.m_ok) &&
      Objects.equals(m_warning, other.m_warning) &&
      Objects.equals(m_type, other.m_type);
  }

  public AlertState state(double value) {
    return getThreshold().state(value);
  }

  private Threshold getThreshold() {
    return new Threshold(getOk().getValue(), getWarning().getValue(), getCritical().getValue());
  }

  public String formatMessage(double value, List<Object> args) {
    List<Object> copy = new ArrayList<>(args);
    copy.add(value);
    return MessageFormat.format(message(value), copy.toArray());
  }

  private String message(double value) {
    switch (state(value)) {
      case OK:
        return getOk().getText();
      case WARNING:
        return getWarning().getText();
      case CRITICAL:
        return getCritical().getText();
      case UNKNOWN:
        return "Unknown";
      case SKIPPED:
        return "Skipped";
      default:
        throw new IllegalStateException("Invalid alert state: " + state(value));
    }
  }

  /**
   * The {@link ReportTemplate} class is used to pair a label and threshhold
   * value.
   * <p/>
   * Equality checking for instances of this class should be executed on every
   * member to ensure that reconciling stack differences is correct.
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public static final class ReportTemplate {
    @SerializedName("text")
    private String m_text;

    @SerializedName("value")
    private Double m_value = null;

    /**
     * @return the parameterized text of this template or {@code null} if none.
     */
    @JsonProperty("text")
    public String getText() {
      return m_text;
    }

    /**
     * @param text
     *          the parameterized text of this template or {@code null} if none.
     */
    public void setText(String text) {
      m_text = text;
    }

    /**
     * @return the threshold value for this template or {@code null} if none.
     */
    @JsonProperty("value")
    public Double getValue() {
      return m_value;
    }

    /**
     * @param value
     *          the threshold value for this template or {@code null} if none.
     */
    public void setValue(Double value) {
      m_value = value;
    }

    @Override
    public int hashCode() {
      return Objects.hash(m_text, m_value);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      ReportTemplate other = (ReportTemplate) obj;
      return Objects.equals(m_text, other.m_text) &&
        Objects.equals(m_value, other.m_value);
    }
  }

  public enum ReportingType {
    /**
     * Integers, longs, floats, etc.
     */
    NUMERIC,

    /**
     * A percent value, expessed as a float.
     */
    PERCENT
  }
}
