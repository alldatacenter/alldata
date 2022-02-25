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
package org.apache.ambari.server.topology.validators;

import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.collect.ImmutableSet;

/**
 * Some configuration values need to have "m" appended to them to be valid values.
 * Required due to AMBARI-4933.
 */
public class UnitValidatedProperty {
  public static final Set<UnitValidatedProperty> ALL = ImmutableSet.<UnitValidatedProperty>builder()
    .add(new UnitValidatedProperty("HDFS", "hadoop-env", "namenode_heapsize"))
    .add(new UnitValidatedProperty("HDFS", "hadoop-env", "namenode_opt_newsize"))
    .add(new UnitValidatedProperty("HDFS", "hadoop-env", "namenode_opt_maxnewsize"))
    .add(new UnitValidatedProperty("HDFS", "hadoop-env", "namenode_opt_permsize"))
    .add(new UnitValidatedProperty("HDFS", "hadoop-env", "namenode_opt_maxpermsize"))
    .add(new UnitValidatedProperty("HDFS", "hadoop-env", "dtnode_heapsize"))
    .add(new UnitValidatedProperty("MAPREDUCE2", "mapred-env","jtnode_opt_newsize"))
    .add(new UnitValidatedProperty("MAPREDUCE2", "mapred-env","jtnode_opt_maxnewsize"))
    .add(new UnitValidatedProperty("MAPREDUCE2", "mapred-env","jtnode_heapsize"))
    .add(new UnitValidatedProperty("HBASE", "hbase-env", "hbase_master_heapsize"))
    .add(new UnitValidatedProperty("HBASE", "hbase-env","hbase_regionserver_heapsize"))
    .add(new UnitValidatedProperty("OOZIE", "oozie-env","oozie_heapsize"))
    .add(new UnitValidatedProperty("OOZIE", "oozie-env", "oozie_permsize"))
    .add(new UnitValidatedProperty("ZOOKEEPER", "zookeeper-env", "zk_server_heapsize"))
    .build();

  private final String configType;
  private final String serviceName;
  private final String propertyName;

  public UnitValidatedProperty(String serviceName, String configType, String propertyName) {
    this.configType = configType;
    this.serviceName = serviceName;
    this.propertyName = propertyName;
  }

  public boolean hasTypeAndName(String configType, String propertyName) {
    return configType.equals(this.getConfigType()) && propertyName.equals(this.getPropertyName());
  }

  public String getConfigType() {
    return configType;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UnitValidatedProperty that = (UnitValidatedProperty) o;
    return new EqualsBuilder()
      .append(configType, that.configType)
      .append(serviceName, that.serviceName)
      .append(propertyName, that.propertyName)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(configType)
      .append(serviceName)
      .append(propertyName)
      .toHashCode();
  }
}
