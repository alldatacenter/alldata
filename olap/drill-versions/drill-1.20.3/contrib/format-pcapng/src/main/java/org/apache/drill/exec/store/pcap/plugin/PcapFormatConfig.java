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
package org.apache.drill.exec.store.pcap.plugin;

import java.util.List;
import java.util.Objects;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(PcapFormatConfig.NAME)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PcapFormatConfig implements FormatPluginConfig {
  private static final List<String> DEFAULT_EXTNS = ImmutableList.of("pcap");

  public static final String NAME = "pcap";
  private final List<String> extensions;
  private final boolean stat;
  private final boolean sessionizeTCPStreams;

  @JsonCreator
  public PcapFormatConfig(@JsonProperty("extensions") List<String> extensions,
                          @JsonProperty("stat") boolean stat,
                          @JsonProperty("sessionizeTCPStreams") Boolean sessionizeTCPStreams) {
    this.extensions = extensions == null ? DEFAULT_EXTNS : ImmutableList.copyOf(extensions);
    this.stat = stat;
    this.sessionizeTCPStreams = sessionizeTCPStreams != null && sessionizeTCPStreams;
  }

  @JsonProperty("extensions")
  public List<String> getExtensions() {
    return extensions;
  }

  @JsonProperty("stat")
  public boolean getStat() {
    return this.stat;
  }

  @JsonProperty("sessionizeTCPStreams")
  public boolean getSessionizeTCPStreams() {
    return sessionizeTCPStreams;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PcapFormatConfig that = (PcapFormatConfig) o;
    return Objects.equals(extensions, that.extensions) && Objects.equals(stat, that.getStat()) &&
            Objects.equals(sessionizeTCPStreams, that.sessionizeTCPStreams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(extensions, stat, sessionizeTCPStreams);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
            .field("extensions", extensions)
            .field("stat", stat)
            .field("sessionizeTCPStreams", sessionizeTCPStreams)
            .toString();
  }
}
