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
package org.apache.ambari.server.agent;

import org.apache.ambari.server.Role;

public interface DummyHeartbeatConstants {

  String DummyCluster = "cluster1";
  String DummyClusterId = "1";
  String DummyHostname1 = "host1";
  String DummyHostname2 = "host2";
  String DummyHostname3 = "host3";
  String DummyHostname4 = "host4";
  String DummyHostname5 = "host5";
  String DummyOs = "CentOS";
  String DummyOsType = "centos5";
  String DummyOSRelease = "5.8";

  Integer DummyCurrentPingPort = 33555;

  String DummyHostStatus = "I am ok";

  String DummyStackId = "HDP-0.1";
  String DummyRepositoryVersion = "0.1-1234";

  String HDFS = "HDFS";
  String HBASE = "HBASE";

  String DATANODE = Role.DATANODE.name();
  String NAMENODE = Role.NAMENODE.name();
  String SECONDARY_NAMENODE = Role.SECONDARY_NAMENODE.name();
  String HBASE_MASTER = Role.HBASE_MASTER.name();
  String HDFS_CLIENT  = Role.HDFS_CLIENT.name();

}
