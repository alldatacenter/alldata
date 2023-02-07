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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import java.util.List;

@JsonTypeName(PcapngFormatConfig.NAME)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Deprecated // for backward compatibility
public class  PcapngFormatConfig extends PcapFormatConfig {
  private static final List<String> DEFAULT_EXTNS = ImmutableList.of("pcapng");
  public static final String NAME = "pcapng";

  @JsonCreator
  public PcapngFormatConfig(@JsonProperty("extensions") List<String> extensions,
                            @JsonProperty("stat") boolean stat) {
    super(extensions == null ? DEFAULT_EXTNS : ImmutableList.copyOf(extensions), stat, null);
  }
}
