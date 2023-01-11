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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.authorization.presto.authorizer;

import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigDescription;

public class RangerConfig {
  private String keytab;
  private String principal;
  private boolean useUgi = false;
  private String hadoopConfigPath;

  public String getKeytab() { return keytab; }

  @Config("ranger.keytab")
  @ConfigDescription("Keytab for authentication against Ranger")
  @SuppressWarnings("unused")
  public RangerConfig setKeytab(String keytab) {
    this.keytab = keytab;
    return this;
  }

  public String getPrincipal() { return principal; }

  @Config("ranger.principal")
  @ConfigDescription("Principal for authentication against Ranger with keytab")
  @SuppressWarnings("unused")
  public RangerConfig setPrincipal(String principal) {
    this.principal = principal;
    return this;
  }

  public boolean isUseUgi() { return useUgi; }

  @Config("ranger.use_ugi")
  @ConfigDescription("Use Hadoop User Group Information instead of Presto groups")
  @SuppressWarnings("unused")
  public RangerConfig setUseUgi(boolean useUgi) {
    this.useUgi = useUgi;
    return this;
  }

  @Config("ranger.hadoop_config")
  @ConfigDescription("Path to hadoop configuration. Defaults to presto-ranger-site.xml in classpath")
  @SuppressWarnings("unused")
  public RangerConfig setHadoopConfigPath(String hadoopConfigPath) {
    this.hadoopConfigPath = hadoopConfigPath;
    return this;
  }

  public String getHadoopConfigPath() { return hadoopConfigPath; }
}
