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

package org.apache.ambari.server.state.quicklinks;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Quick links may override host names of host components with host names that come from configuration.
 */
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Host {
  /**
   * The property name that has the host name if the protocol is http
   */
  @JsonProperty("http_property")
  private String httpProperty;

  /**
   * The property name that has the host name if the protocol is https
   */
  @JsonProperty("https_property")
  private String httpsProperty;

  /**
   * The config type where the overridden host name comes from
   */
  @JsonProperty("site")
  private String site;

  public String getHttpProperty() {
    return httpProperty;
  }

  public String getHttpsProperty() {
    return httpsProperty;
  }

  public String getSite() {
    return site;
  }

  public void mergeWithParent(Host parentHost) {
    if(null == parentHost) {
      return;
    }
    if(null == httpProperty && null != parentHost.getHttpProperty()) {
      httpProperty = parentHost.getHttpProperty();
    }
    if(null == httpsProperty && null != parentHost.getHttpsProperty()) {
      httpsProperty = parentHost.getHttpsProperty();
    }
    if(null == site && null != parentHost.getSite()) {
      site = parentHost.getSite();
    }
  }
}
