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

import org.apache.ambari.server.state.UriInfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


/**
 * Alert when the source type is defined as {@link SourceType#SERVER}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ServerSource extends ParameterizedSource {

  @SerializedName("class")
  private String m_class;

  @SerializedName("uri")
  private UriInfo uri = null;

  @SerializedName("jmx")
  private MetricSource.JmxInfo jmxInfo = null;


  /**
   * Gets the fully qualified classname specified in the source.
   */
  @JsonProperty("class")
  public String getSourceClass() {
    return m_class;
  }

  public MetricSource.JmxInfo getJmxInfo() {
    return jmxInfo;
  }

  public UriInfo getUri() {
    return uri;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), m_class);
  }

  /**
   * {@inheritDoc}
   */
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

    ServerSource other = (ServerSource) obj;
    return Objects.equals(m_class, other.m_class);
  }

}
