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
package org.apache.drill.exec.store.image;

import java.util.List;
import java.util.Objects;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(ImageFormatConfig.NAME)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ImageFormatConfig implements FormatPluginConfig {

  public static final String NAME = "image";
  private final List<String> extensions;
  private final boolean fileSystemMetadata;
  private final boolean descriptive;
  private final String timeZone;

  @JsonCreator
  public ImageFormatConfig(
      @JsonProperty("extensions") List<String> extensions,
      @JsonProperty("fileSystemMetadata") Boolean fileSystemMetadata,
      @JsonProperty("descriptive") Boolean descriptive,
      @JsonProperty("timeZone") String timeZone) {
    this.extensions = extensions == null ? ImmutableList.of() : ImmutableList.copyOf(extensions);
    this.fileSystemMetadata = fileSystemMetadata == null || fileSystemMetadata;
    this.descriptive = descriptive == null || descriptive;
    this.timeZone = timeZone;
  }

  @JsonProperty("extensions")
  public List<String> getExtensions() {
    return extensions;
  }

  @JsonProperty("fileSystemMetadata")
  public boolean hasFileSystemMetadata() {
    return fileSystemMetadata;
  }

  @JsonProperty("descriptive")
  public boolean isDescriptive() {
    return descriptive;
  }

  @JsonProperty("timeZone")
  public String getTimeZone() {
    return timeZone;
  }

  @Override
  public int hashCode() {
    return Objects.hash(extensions, fileSystemMetadata, descriptive, timeZone);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ImageFormatConfig other = (ImageFormatConfig) obj;
    return Objects.equals(extensions, other.extensions) &&
           Objects.equals(fileSystemMetadata, other.fileSystemMetadata) &&
           Objects.equals(descriptive, other.descriptive) &&
           Objects.equals(timeZone, other.timeZone);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
        .field("extensions", extensions)
        .field("fileSystemMetadata", fileSystemMetadata)
        .field("descriptive", descriptive)
        .field("timeZone", timeZone)
        .toString();
  }
}