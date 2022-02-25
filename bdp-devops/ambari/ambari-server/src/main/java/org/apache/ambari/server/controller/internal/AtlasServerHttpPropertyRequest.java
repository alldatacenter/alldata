/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.Map;

import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.state.Cluster;

/**
 * Atlas server specific HTTP property request.
 */
public class AtlasServerHttpPropertyRequest extends JsonHttpPropertyRequest {

  private static final String PROPERTY_ENABLE_TLS = "atlas.enableTLS";
  private static final String PROPERTY_SERVER_HTTPS_PORT = "atlas.server.https.port";
  private static final String PROPERTY_SERVER_HTTP_PORT = "atlas.server.http.port";
  private static final String CONFIG_APPLICATION_PROPERTIES = "application-properties";
  private static final String URL_TEMPLATE = "%s://%s:%s/api/atlas/admin/status";

  private static final Map<String, String> PROPERTY_MAPPINGS =
      Collections.singletonMap("Status", "HostRoles/ha_state");


  // ----- Constructors ----------------------------------------------------

  public AtlasServerHttpPropertyRequest() {
    super(PROPERTY_MAPPINGS);
  }


  // ----- PropertyRequest -------------------------------------------------

  @Override
  public String getUrl(Cluster cluster, String hostName)
      throws SystemException {

    Map<String, String> atlasConfig = cluster.getDesiredConfigByType(CONFIG_APPLICATION_PROPERTIES).getProperties();

    boolean useHttps = Boolean.parseBoolean(getConfigValue(atlasConfig, PROPERTY_ENABLE_TLS, "false"));

    String port = useHttps ?
        getConfigValue(atlasConfig, PROPERTY_SERVER_HTTPS_PORT, "21443") :
        getConfigValue(atlasConfig, PROPERTY_SERVER_HTTP_PORT, "21000");

    return String.format(URL_TEMPLATE, useHttps ? "https" : "http", hostName, port);
  }


  // ----- helper methods --------------------------------------------------

  // get a configuration property value
  private String getConfigValue(Map<String, String> atlasConfig, String property, String defaultValue) {
    return atlasConfig.containsKey(property) ? atlasConfig.get(property) : defaultValue;
  }
}
