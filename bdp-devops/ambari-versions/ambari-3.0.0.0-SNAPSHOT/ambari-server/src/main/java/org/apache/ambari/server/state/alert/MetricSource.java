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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.ambari.server.controller.jmx.JMXMetricHolder;
import org.apache.ambari.server.state.UriInfo;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Alert when the source type is defined as {@link SourceType#METRIC}
 * <p/>
 * Equality checking for instances of this class should be executed on every
 * member to ensure that reconciling stack differences is correct.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MetricSource extends Source {

  @SerializedName("uri")
  private UriInfo uri = null;

  @SerializedName("jmx")
  private JmxInfo jmxInfo = null;

  @SerializedName("ganglia")
  private String gangliaInfo = null;

  /**
   * @return the jmx info, if this metric is jmx-based
   */
  @JsonProperty("jmx")
  public JmxInfo getJmxInfo() {
    return jmxInfo;
  }

  /**
   * @return the ganglia info, if this metric is ganglia-based
   */
  @JsonProperty("ganglia")
  public String getGangliaInfo() {
    return gangliaInfo;
  }

  /**
   * @return the uri info, which may include port information
   */
  @JsonProperty("uri")
  public UriInfo getUri() {
    return uri;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), gangliaInfo, uri, jmxInfo);
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

    MetricSource other = (MetricSource) obj;
    return Objects.equals(gangliaInfo, other.gangliaInfo) &&
      Objects.equals(uri, other.uri) &&
      Objects.equals(jmxInfo, other.jmxInfo);
  }

  /**
   * Represents the {@code jmx} element in a Metric alert.
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
  public static class JmxInfo {
    @JsonProperty("property_list")
    @SerializedName("property_list")
    private List<String> propertyList;

    @SerializedName("value")
    private String value = "{0}";

    @JsonProperty("url_suffix")
    @SerializedName("url_suffix")
    private String urlSuffix = "/jmx";

    public List<String> getPropertyList() {
      return propertyList;
    }

    public void setPropertyList(List<String> propertyList) {
      this.propertyList = propertyList;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public Value getValue() {
      return new Value(value);
    }

    @Override
    public boolean equals(Object object) {
      if (!JmxInfo.class.isInstance(object)) {
        return false;
      }

      JmxInfo other = (JmxInfo)object;

      List<String> list1 = new ArrayList<>(propertyList);
      List<String> list2 = new ArrayList<>(other.propertyList);

      // !!! even if out of order, this is enough to fail
      return list1.equals(list2);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(propertyList);
    }

    public String getUrlSuffix() {
      return urlSuffix;
    }

    public Optional<Number> eval(JMXMetricHolder jmxMetricHolder) {
      List<Object> metrics = jmxMetricHolder.findAll(propertyList);
      if (metrics.isEmpty()) {
        return Optional.empty();
      } else {
        Object value = getValue().eval(metrics);
        return value instanceof Number ? Optional.of((Number)value) : Optional.empty();
      }
    }
  }

  public static class Value {
    private final String value;

    public Value(String value) {
      this.value = value;
    }

    /**
     * Evaluate an expression like "{0}/({0} + {1}) * 100.0" where each positional argument represent a metrics value.
     * The value is converted to SpEL syntax:
     *  #var0/(#var0 + #var1) * 100.0
     * then it is evaluated in the context of the metrics parameters.
     */
    public Object eval(List<Object> metrics) {
      StandardEvaluationContext context = new StandardEvaluationContext();
      context.setVariables(range(0, metrics.size()).boxed().collect(toMap(i -> "var" + i, metrics::get)));
      return new SpelExpressionParser()
        .parseExpression(value.replaceAll("(\\{(\\d+)\\})", "#var$2"))
        .getValue(context);
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
