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

package org.apache.drill.exec.store.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonTypeName(XMLFormatPlugin.DEFAULT_NAME)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class XMLFormatConfig implements FormatPluginConfig {

  public final List<String> extensions;
  public final int dataLevel;

  public XMLFormatConfig(@JsonProperty("extensions") List<String> extensions,
                         @JsonProperty("dataLevel") int dataLevel) {
    this.extensions = extensions == null ? Collections.singletonList("xml") : ImmutableList.copyOf(extensions);
    this.dataLevel = Math.max(dataLevel, 1);
  }

  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  public List<String> getExtensions() {
    return extensions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(extensions, dataLevel);
  }

  public XMLBatchReader.XMLReaderConfig getReaderConfig(XMLFormatPlugin plugin) {
    return new XMLBatchReader.XMLReaderConfig(plugin);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    XMLFormatConfig other = (XMLFormatConfig) obj;
    return Objects.equals(extensions, other.extensions)
      && Objects.equals(dataLevel, other.dataLevel);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("extensions", extensions)
      .field("dataLevel", dataLevel)
      .toString();
  }
}