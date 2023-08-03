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

package org.apache.ranger.services.hdfs.client;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;


public class HdfsClientTest {

  @Test(expected = IllegalArgumentException.class)
  public void testUsernameNotSpecified() throws IllegalArgumentException {
    Map<String, String> configs = new HashMap<String, String>();
    HdfsClient.validateConnectionConfigs(configs);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPasswordNotSpecified() throws IllegalArgumentException {
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("username", "hdfsuser");
    HdfsClient.validateConnectionConfigs(configs);
  }
  @Test(expected = IllegalArgumentException.class)
  public void testAuthenticationNotSpecified()  throws IllegalArgumentException {
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("username", "hdfsuser");
    configs.put("password", "hdfsuser");
    HdfsClient.validateConnectionConfigs(configs);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFsDefaultNameNotSpecified()  throws IllegalArgumentException {
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("username", "hdfsuser");
    configs.put("password", "hdfsuser");
    configs.put("hadoop.security.authentication", "simple");
    HdfsClient.validateConnectionConfigs(configs);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProxyProviderNotSpecified()  throws IllegalArgumentException {
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("username", "hdfsuser");
    configs.put("password", "hdfsuser");
    configs.put("hadoop.security.authentication", "simple");
    configs.put("fs.default.name", "hdfs://hwqe-1425428405");
    configs.put("dfs.nameservices", "hwqe-1425428405");
    HdfsClient.validateConnectionConfigs(configs);
  }

  @Test(expected = IllegalArgumentException.class)
	public void testNnElementsNotSpecified()  throws IllegalArgumentException {
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("username", "hdfsuser");
    configs.put("password", "hdfsuser");
    configs.put("hadoop.security.authentication", "simple");
    configs.put("fs.default.name", "hdfs://hwqe-1425428405");
    configs.put("dfs.nameservices", "hwqe-1425428405");
    configs.put("dfs.client.failover.proxy.provider.hwqe-1425428405",
      "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
    HdfsClient.validateConnectionConfigs(configs);
	}

  @Test(expected = IllegalArgumentException.class)
	public void testNn1UrlNn2UrlNotSpecified()  throws IllegalArgumentException {
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("username", "hdfsuser");
    configs.put("password", "hdfsuser");
    configs.put("hadoop.security.authentication", "simple");
    configs.put("fs.default.name", "hdfs://hwqe-1425428405");
    configs.put("dfs.nameservices", "hwqe-1425428405");
    configs.put("dfs.client.failover.proxy.provider.hwqe-1425428405",
      "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
    configs.put("dfs.ha.namenodes.hwqe-1425428405", "nn1,nn2");
    HdfsClient.validateConnectionConfigs(configs);
	}

  @Test(expected = IllegalArgumentException.class)
	public void testNn1UrlNotSpecified()  throws IllegalArgumentException {
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("username", "hdfsuser");
    configs.put("password", "hdfsuser");
    configs.put("hadoop.security.authentication", "simple");
    configs.put("fs.default.name", "hdfs://hwqe-1425428405");
    configs.put("dfs.nameservices", "hwqe-1425428405");
    configs.put("dfs.client.failover.proxy.provider.hwqe-1425428405",
      "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
    configs.put("dfs.ha.namenodes.hwqe-1425428405", "nn1,nn2");
    configs.put("dfs.namenode.rpc-address.hwqe-1425428405.nn2", "node-2.example.com:8020");
    HdfsClient.validateConnectionConfigs(configs);
	}

  @Test(expected = IllegalArgumentException.class)
	public void testNn2UrlNotSpecified()  throws IllegalArgumentException {
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("username", "hdfsuser");
    configs.put("password", "hdfsuser");
    configs.put("hadoop.security.authentication", "simple");
    configs.put("fs.default.name", "hdfs://hwqe-1425428405");
    configs.put("dfs.nameservices", "hwqe-1425428405");
    configs.put("dfs.client.failover.proxy.provider.hwqe-1425428405",
      "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
    configs.put("dfs.ha.namenodes.hwqe-1425428405", "nn1,nn2");
    configs.put("dfs.namenode.rpc-address.hwqe-1425428405.nn1", "node-1.example.com:8020");
    HdfsClient.validateConnectionConfigs(configs);
	}

  @Test
  public void testValidNonHaConfig()  throws IllegalArgumentException {

    // username: hdfsuser
    // password: hdfsuser
    // hadoop.security.authentication: simple
    // fs.default.name: hdfs://node-2.example.com:8020

    Map<String, String> configs = new HashMap<String, String>();
    configs.put("username", "hdfsuser");
    configs.put("password", "hdfsuser");
    configs.put("hadoop.security.authentication", "simple");
    configs.put("fs.default.name", "hdfs://node-2.example.com:8020");
    HdfsClient.validateConnectionConfigs(configs);
  }

  @Test
  public void testValidHaConfig()  throws IllegalArgumentException {

    // username: hdfsuser
    // password: hdfsuser
    // hadoop.security.authentication: simple
    // dfs.nameservices: hwqe-1425428405
    // fs.default.name:
    // dfs.ha.namenodes.hwqe-1425428405: nn1,nn2
    // dfs.namenode.rpc-address.hwqe-1425428405.nn2: node-2.example.com:8020
    // dfs.namenode.rpc-address.hwqe-1425428405.nn1:  node-1.example.com:8020

    Map<String, String> configs = new HashMap<String, String>();
    configs.put("username", "hdfsuser");
    configs.put("password", "hdfsuser");
    configs.put("hadoop.security.authentication", "simple");
    configs.put("fs.default.name", "hdfs://node-2.example.com:8020");
    configs.put("fs.default.name", "hdfs://hwqe-1425428405");
    configs.put("dfs.nameservices", "hwqe-1425428405");
    configs.put("dfs.client.failover.proxy.provider.hwqe-1425428405",
      "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
    configs.put("dfs.ha.namenodes.hwqe-1425428405", "nn1,nn2");
    configs.put("dfs.namenode.rpc-address.hwqe-1425428405.nn1", "node-1.example.com:8020");
    configs.put("dfs.namenode.rpc-address.hwqe-1425428405.nn2", "node-2.example.com:8020");
    HdfsClient.validateConnectionConfigs(configs);
  }

}
