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
package org.apache.drill.exec;

import org.apache.drill.exec.coord.zk.PathUtils;
import org.apache.zookeeper.client.ZooKeeperSaslClient;
import org.apache.zookeeper.server.ZooKeeperSaslServer;

import static org.apache.zookeeper.Environment.JAAS_CONF_KEY;

public class ZookeeperTestUtil {

  private static final String LOGIN_CONF_RESOURCE_PATHNAME = "login.conf";

  /**
   * Sets zookeeper server and client SASL test config properties.
   */
  public static void setZookeeperSaslTestConfigProps() {
    System.setProperty(ZooKeeperSaslServer.LOGIN_CONTEXT_NAME_KEY, "DrillTestServerForUnitTests");
    System.setProperty(ZooKeeperSaslClient.LOGIN_CONTEXT_NAME_KEY, "DrillTestClientForUnitTests");
  }

  /**
   * Specifies JAAS configuration file.
   */
  public static void setJaasTestConfigFile() {
    String configPath = PathUtils.getPathWithProtocol(ClassLoader.getSystemResource(LOGIN_CONF_RESOURCE_PATHNAME));
    System.setProperty(JAAS_CONF_KEY, configPath);
  }
}
