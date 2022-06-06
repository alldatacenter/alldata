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

package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.Map;

import org.apache.ambari.annotations.ApiIgnore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

/**
 * Provides support for JSON serializaion of {@link Configuration} objects. Can handle both plain JSON and Ambari style
 * flattened JSON such as {@code "hdfs-site/properties/dfs.replication": "3"}. Objects may implement this interface or
 * call its static utility methods.
 */
public interface Configurable {

  String CONFIGURATIONS = "configurations";

  @JsonIgnore
  @ApiIgnore
  void setConfiguration(Configuration configuration);

  @JsonIgnore
  @ApiIgnore
  Configuration getConfiguration();

  @JsonProperty(CONFIGURATIONS)
  @ApiModelProperty(name = CONFIGURATIONS)
  default void setConfigs(Collection<? extends Map<String, ?>> configs) {
    setConfiguration(ConfigurableHelper.parseConfigs(configs));
  }

  @JsonProperty(CONFIGURATIONS)
  @ApiModelProperty(name = CONFIGURATIONS)
  default Collection<Map<String, Map<String, ?>>> getConfigs() {
    return ConfigurableHelper.convertConfigToMap(getConfiguration());
  }

}
