/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.stack;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.utils.HTTPUtils;
import org.apache.ambari.server.utils.HostAndPort;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * I represent a nameServiceId that belongs to HDFS. Multiple namenodes may belong to the same nameServiceId.
 * Each NameNode has either an http or https address.
 */
public class NameService {

  public static class NameNode {
    private final String uniqueId;
    private final String address;
    private final boolean encrypted;
    private final String propertyName;

    public static NameNode fromConfig(String nameServiceId, String nnUniqueId, ConfigHelper config, Cluster cluster) {
      String namenodeFragment = "dfs.namenode." + (isEncrypted(config, cluster) ? "https-address" : "http-address") + ".{0}.{1}";
      String propertyName = MessageFormat.format(namenodeFragment, nameServiceId, nnUniqueId);
      return new NameNode(
        nnUniqueId,
        config.getValueFromDesiredConfigurations(cluster, ConfigHelper.HDFS_SITE, propertyName),
        isEncrypted(config, cluster),
        propertyName);
    }

    NameNode(String uniqueId, String address, boolean encrypted, String propertyName) {
      this.uniqueId = uniqueId;
      this.address = address;
      this.encrypted = encrypted;
      this.propertyName = propertyName;
    }

    public String getHost() {
      return getAddress().host.toLowerCase();
    }

    public int getPort() {
      return getAddress().port;
    }

    private HostAndPort getAddress() {
      HostAndPort hp = HTTPUtils.getHostAndPortFromProperty(address);
      if (hp == null) {
        throw new IllegalArgumentException("Could not parse host and port from " + address);
      }
      return hp;
    }

    private static boolean isEncrypted(ConfigHelper config, Cluster cluster) {
      String policy = config.getValueFromDesiredConfigurations(cluster, ConfigHelper.HDFS_SITE, "dfs.http.policy");
      return (policy != null && policy.equalsIgnoreCase(ConfigHelper.HTTPS_ONLY));
    }

    public boolean isEncrypted() {
      return encrypted;
    }

    public String getPropertyName() {
      return propertyName;
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this)
        .append("uniqueId", uniqueId)
        .append("address", address)
        .append("encrypted", encrypted)
        .append("propertyName", propertyName)
        .toString();
    }
  }

  public final String nameServiceId;
  /**
   * NameNodes in this nameservice.
   * The minimum number of NameNodes for HA is two, but more can be configured.
   */
  private final List<NameNode> nameNodes;

  public static List<NameService> fromConfig(ConfigHelper config, Cluster cluster) {
    return nameServiceIds(config, cluster).stream()
      .map(nameServiceId -> nameService(nameServiceId, config, cluster))
      .collect(Collectors.toList());
  }

  private static List<String> nameServiceIds(ConfigHelper config, Cluster cluster) {
    return separateByComma(config, cluster, "dfs.internal.nameservices");
  }

  private static NameService nameService(String nameServiceId, ConfigHelper config, Cluster cluster) {
    List<NameNode> namenodes = nnUniqueIds(nameServiceId, config, cluster).stream()
      .map(nnUniquId -> NameNode.fromConfig(nameServiceId, nnUniquId, config, cluster))
      .collect(Collectors.toList());
    return new NameService(nameServiceId, namenodes);
  }

  private static List<String> nnUniqueIds(String nameServiceId, ConfigHelper config, Cluster cluster) {
    return separateByComma(config, cluster, "dfs.ha.namenodes." + nameServiceId);
  }

  private static List<String> separateByComma(ConfigHelper config, Cluster cluster, String propertyName) {
    String propertyValue = config.getValueFromDesiredConfigurations(cluster, ConfigHelper.HDFS_SITE, propertyName);
    return isBlank(propertyValue)
      ? Collections.emptyList()
      : Arrays.asList(propertyValue.split(","));
  }

  private NameService(String nameServiceId, List<NameNode> nameNodes) {
    this.nameServiceId = nameServiceId;
    this.nameNodes = nameNodes;
  }

  public List<NameNode> getNameNodes() {
    return nameNodes;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("nameServiceId", nameServiceId)
      .append("nameNodes", getNameNodes())
      .toString();
  }
}
