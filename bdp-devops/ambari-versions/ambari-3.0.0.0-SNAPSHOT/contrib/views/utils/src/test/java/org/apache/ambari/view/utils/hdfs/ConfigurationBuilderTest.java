/**
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

package org.apache.ambari.view.utils.hdfs;

import com.google.common.base.Joiner;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.ambari.view.utils.hdfs.ConfigurationBuilder.HA_NAMENODES_INSTANCE_PROPERTY;
import static org.apache.ambari.view.utils.hdfs.ConfigurationBuilder.NAMENODE_HTTPS_NN_CLUSTER_PROPERTY;
import static org.apache.ambari.view.utils.hdfs.ConfigurationBuilder.NAMENODE_HTTPS_NN_INSTANCE_PROPERTY;
import static org.apache.ambari.view.utils.hdfs.ConfigurationBuilder.NAMENODE_HTTP_NN_CLUSTER_PROPERTY;
import static org.apache.ambari.view.utils.hdfs.ConfigurationBuilder.NAMENODE_HTTP_NN_INSTANCE_PROPERTY;
import static org.apache.ambari.view.utils.hdfs.ConfigurationBuilder.NAMENODE_RPC_NN_CLUSTER_PROPERTY;
import static org.apache.ambari.view.utils.hdfs.ConfigurationBuilder.NAMENODE_RPC_NN_INSTANCE_PROPERTY;
import static org.apache.ambari.view.utils.hdfs.ConfigurationBuilder.NAMESERVICES_INSTANCE_PROPERTY;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurationBuilderTest extends EasyMockSupport {
  @Test
  public void testAddProtocolMissing() throws Exception {
    String normalized = ConfigurationBuilder.addProtocolIfMissing("namenode.example.com:50070");
    assertEquals(normalized, "webhdfs://namenode.example.com:50070");
  }

  @Test
  public void testAddProtocolPresent() throws Exception {
    String normalized = ConfigurationBuilder.addProtocolIfMissing("webhdfs://namenode.example.com");
    assertEquals(normalized, "webhdfs://namenode.example.com");
  }

  @Test
  public void testAddPortMissing() throws Exception {
    String normalized = ConfigurationBuilder.addPortIfMissing("webhdfs://namenode.example.com");
    assertEquals(normalized, "webhdfs://namenode.example.com:50070");
  }

  @Test
  public void testAddPortPresent() throws Exception {
    String normalized = ConfigurationBuilder.addPortIfMissing("webhdfs://namenode.example.com:50070");
    assertEquals(normalized, "webhdfs://namenode.example.com:50070");
  }

  @Test
  public void testGetEncryptionKeyProviderUri() throws Exception {
    //For View with an associated cluster must return the following KeyProvider
    //For View with NO cluster associated with it, getEncryptionKeyProviderUri() won't be called

    String keyProvider = "kms://http@localhost:16000/kms";
    Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getConfigurationValue("hdfs-site", "dfs.encryption.key.provider.uri")).andReturn(keyProvider);
    replay(cluster);

    ViewContext viewContext = createNiceMock(ViewContext.class);
    expect(viewContext.getCluster()).andReturn(cluster).anyTimes();
    Map<String, String> instanceProperties = new HashMap<>();
    expect(viewContext.getProperties()).andReturn(instanceProperties).anyTimes();
    replay(viewContext);

    ConfigurationBuilder configurationBuilder = new ConfigurationBuilder(viewContext);
    String encryptionKeyProviderUri = configurationBuilder.getEncryptionKeyProviderUri();

    assertEquals(encryptionKeyProviderUri, keyProvider);
  }

  @Test
  public void testCopyHAProperties() throws Exception {
    Map<String, String> properties = new HashMap();
    String[] nnrpc = new String[]{"nn1rpc", "nn2rpc", "nn3rpc"};
    String[] nnhttp = new String[]{"nn1http", "nn2http", "nn3http"};
    String[] nnhttps = new String[]{"nn1https", "nn2https", "nn3https"};

    String nameservice = "mycluster";
    String nameNodesString = "nn1,nn2,nn3";
    String[] namenodes = nameNodesString.split(",");
    properties.put(NAMESERVICES_INSTANCE_PROPERTY, nameservice);
    properties.put(HA_NAMENODES_INSTANCE_PROPERTY, nameNodesString);
    properties.put(NAMENODE_RPC_NN_INSTANCE_PROPERTY, Joiner.on(",").join(Arrays.asList(nnrpc)));
    properties.put(NAMENODE_HTTP_NN_INSTANCE_PROPERTY, Joiner.on(",").join(Arrays.asList(nnhttp)));
    properties.put(NAMENODE_HTTPS_NN_INSTANCE_PROPERTY, Joiner.on(",").join(Arrays.asList(nnhttps)));

    String defaultFS = "webhdfs://" + nameservice;
    Cluster cluster = mock(Cluster.class);
    ViewContext viewContext = mock(ViewContext.class);
    when(viewContext.getCluster()).thenReturn(null);
    when(viewContext.getProperties()).thenReturn(properties);

    ConfigurationBuilder configurationBuilder = new ConfigurationBuilder(viewContext);
    configurationBuilder.copyHAProperties(defaultFS);

    for(int i = 0 ; i < nnhttp.length; i++) {
      Assert.assertEquals("name node rpc address not correct.", nnrpc[i], configurationBuilder.conf.get(String.format(NAMENODE_RPC_NN_CLUSTER_PROPERTY, nameservice, namenodes[i])));
      Assert.assertEquals("name node http address not correct.", nnhttp[i], configurationBuilder.conf.get(String.format(NAMENODE_HTTP_NN_CLUSTER_PROPERTY, nameservice, namenodes[i])));
      Assert.assertEquals("name node https address not correct.", nnhttps[i], configurationBuilder.conf.get(String.format(NAMENODE_HTTPS_NN_CLUSTER_PROPERTY, nameservice, namenodes[i])));
    }
  }
}
