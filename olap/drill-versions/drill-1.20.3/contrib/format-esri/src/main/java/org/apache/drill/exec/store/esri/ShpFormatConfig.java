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

package org.apache.drill.exec.store.esri;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;

/**
 * Format plugin config for shapefile data files.
 */
@JsonTypeName(ShpFormatPlugin.PLUGIN_NAME)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShpFormatConfig implements FormatPluginConfig {

  private final List<String> extensions;

  @JsonCreator
  public ShpFormatConfig(
      @JsonProperty("extensions") List<String> extensions) {
    this.extensions = extensions == null ?
        ImmutableList.of("shp") : ImmutableList.copyOf(extensions);
  }

  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  public List<String> getExtensions() {
    return extensions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(extensions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ShpFormatConfig other = (ShpFormatConfig) obj;
    return Objects.equals(extensions, other.getExtensions());
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
        .field("extensions", extensions)
        .toString();
  }
}
