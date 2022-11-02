/**
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

package org.apache.ambari.view.utils.hdfs;

import com.google.common.base.Strings;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.ambari.view.utils.ambari.NoClusterAssociatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the Authentication parameters of HDFS based on ViewContext.
 * Currently supports only SIMPLE authorization. KERBEROS is not supported
 * because proxyuser can be arbitrary, so can't be determined from configuration.
 */
public class AuthConfigurationBuilder {

  protected static final Logger LOG = LoggerFactory.getLogger(AuthConfigurationBuilder.class);
  private Map<String, String> params = new HashMap<String, String>();

  private ViewContext context;

  public AuthConfigurationBuilder(ViewContext context) {
    this.context = context;
  }

  /**
   * Converts auth params as semicolon separated string to Map.
   * If auth params are not provided, tries to determine them
   * from Ambari configuration.
   */
  private void parseProperties() throws HdfsApiException {
    String auth;
    auth = context.getProperties().get("webhdfs.auth");

    if (Strings.isNullOrEmpty(auth)) {
      if (context.getCluster() != null) {
        auth = getConfigurationFromAmbari();
      } else {
        auth = "auth=SIMPLE";
        LOG.warn(String.format("HDFS090 Authentication parameters could not be determined. %s assumed.", auth));
      }
    }
    LOG.debug("Hdfs auth params : {}", auth);
    parseAuthString(auth);
  }

  private void parseAuthString(String auth) {
    for (String param : auth.split(";")) {
      String[] keyvalue = param.split("=");
      if (keyvalue.length != 2) {
        LOG.error("HDFS050 Can not parse authentication param " + param + " in " + auth);
        continue;
      }
      params.put(keyvalue[0], keyvalue[1]);
    }
  }

  /**
   * Determine configuration from Ambari.
   */
  private String getConfigurationFromAmbari() throws NoClusterAssociatedException {
    String authMethod = context.getCluster().getConfigurationValue(
        "core-site", "hadoop.security.authentication");

    String authString = String.format("auth=%s", authMethod);

    String proxyUser = context.getCluster().getConfigurationValue("cluster-env","ambari_principal_name");
    if(proxyUser != null && !authMethod.equalsIgnoreCase("SIMPLE")){
      authString = authString + String.format(";proxyuser=%s",proxyUser.split("@")[0]);
    }
    return authString;
  }

  /**
   * Build the auth configuration
   * @return Map of auth properties
   * @throws HdfsApiException
   */
  public Map<String, String> build() throws HdfsApiException {
    parseProperties();
    return params;
  }
}
