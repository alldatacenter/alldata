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

package org.apache.drill.exec.store.hdf5;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonTypeName(HDF5FormatPlugin.DEFAULT_NAME)
public class HDF5FormatConfig implements FormatPluginConfig {

  private final List<String> extensions;
  private final String defaultPath;
  private final boolean showPreview;

  @JsonCreator
  public HDF5FormatConfig(
      @JsonProperty("extensions") List<String> extensions,
      @JsonProperty("defaultPath") String defaultPath,
      @JsonProperty("showPreview") boolean showPreview) {
    this.extensions = extensions == null
        ? Collections.singletonList("h5")
        : ImmutableList.copyOf(extensions);
    this.defaultPath = defaultPath;
    this.showPreview = showPreview;
  }

  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  public List<String> getExtensions() {
    return extensions;
  }

  public String getDefaultPath() {
    return defaultPath;
  }

  public boolean showPreview() { return showPreview; }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    HDF5FormatConfig other = (HDF5FormatConfig) obj;
    return Objects.equals(extensions, other.getExtensions()) &&
      Objects.equals(defaultPath, other.defaultPath) &&
      Objects.equals(showPreview, other.showPreview);
  }

  @Override
  public int hashCode() {
    return Objects.hash(extensions, defaultPath, showPreview);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("extensions", extensions)
      .field("default path", defaultPath)
      .field("show preview", showPreview)
      .toString();
  }
}
