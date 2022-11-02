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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.ambari.server.state.AlertState;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


/**
 * The {@link ParameterizedSource} is used for alerts where the logic of
 * computing the {@link AlertState} is dependant on user-specified parameters.
 * For example, the parameters might be threshold values.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class ParameterizedSource extends Source {

  /**
   * A list of all of the alert parameters, if any.
   */
  @SerializedName("parameters")
  List<AlertParameter> m_parameters;

  /**
   * Gets a list of the optional parameters which govern how a parameterized
   * alert behaves. These are usually threshold values.
   *
   * @return the list of parameters, or an empty list if none.
   */
  @JsonProperty("parameters")
  public List<AlertParameter> getParameters() {
    if (null == m_parameters) {
      return Collections.emptyList();
    }

    return m_parameters;
  }

  /**
   * The {@link AlertParameter} class represents a single parameter that can be
   * passed into an alert which takes parameters.
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public static class AlertParameter {
    @SerializedName("name")
    private String m_name;

    @SerializedName("display_name")
    private String m_displayName;

    @SerializedName("units")
    private String m_units;

    @SerializedName("value")
    private Object m_value;

    @SerializedName("description")
    private String m_description;

    @SerializedName("type")
    private AlertParameterType m_type;

    @SerializedName("visibility")
    private AlertParameterVisibility m_visibility;

    /**
     * If this alert parameter controls a threshold, then its specified here,
     * otherwise it's {@code null}.
     */
    @SerializedName("threshold")
    private AlertState m_threshold;

    /**
     * Gets the unique name of the parameter.
     *
     * @return the name
     */
    @JsonProperty("name")
    public String getName() {
      return m_name;
    }

    /**
     * Gets the human readable name of the parameter.
     *
     * @return the displayName
     */
    @JsonProperty("display_name")
    public String getDisplayName() {
      return m_displayName;
    }

    /**
     * Gets the display units of the paramter.
     *
     * @return the units
     */
    @JsonProperty("units")
    public String getUnits() {
      return m_units;
    }

    /**
     * Gets the value of the parameter.
     *
     * @return the value
     */
    @JsonProperty("value")
    public Object getValue() {
      return m_value;
    }

    /**
     * Gets the description of the parameter.
     *
     * @return the description
     */
    @JsonProperty("description")
    public String getDescription() {
      return m_description;
    }

    @JsonProperty("type")
    public AlertParameterType getType() {
      return m_type;
    }

    /**
     * Gets the visibility of the parameter.
     *
     * @return the visibility
     */
    @JsonProperty("visibility")
    public AlertParameterVisibility getVisibility() {
      return m_visibility;
    }

    /**
     * Gets the threshold that this parameter directly controls, or {@code null}
     * for none.
     *
     * @return the threshold, or {@code null}.
     */
    @JsonProperty("threshold")
    public AlertState getThreshold() {
      return m_threshold;
    }

    @Override
    public int hashCode() {
      return Objects.hash(m_description, m_displayName, m_name, m_threshold, m_type, m_units, m_value, m_visibility);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      AlertParameter other = (AlertParameter) obj;
      return Objects.equals(m_description, other.m_description) &&
        Objects.equals(m_displayName, other.m_displayName) &&
        Objects.equals(m_name, other.m_name) &&
        Objects.equals(m_threshold, other.m_threshold) &&
        Objects.equals(m_type, other.m_type) &&
        Objects.equals(m_units, other.m_units) &&
        Objects.equals(m_value, other.m_value) &&
        Objects.equals(m_visibility, other.m_visibility);
    }
  }

  /**
   * The {@link AlertParameterType} enum represents the value type.
   */
  public enum AlertParameterType {
    /**
     * String
     */
    STRING,

    /**
     * Integers, longs, floats, etc.
     */
    NUMERIC,

    /**
     * A percent value, expessed as a float.
     */
    PERCENT
  }

  /**
   * The {@link AlertParameterVisibility} enum represents the visibility of
   * alert parameters.
   */
  public enum AlertParameterVisibility {
    VISIBLE, HIDDEN, READ_ONLY
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof ParameterizedSource)) return false;

    ParameterizedSource that = (ParameterizedSource) o;

    return new EqualsBuilder()
        .appendSuper(super.equals(o))
        .append(m_parameters, that.m_parameters)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .appendSuper(super.hashCode())
        .append(m_parameters)
        .toHashCode();
  }
}
