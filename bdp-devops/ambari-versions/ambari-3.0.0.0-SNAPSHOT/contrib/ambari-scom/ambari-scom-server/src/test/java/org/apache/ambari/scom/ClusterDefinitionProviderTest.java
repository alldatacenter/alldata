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

package org.apache.ambari.scom;

import org.apache.ambari.server.configuration.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

/**
 * ClusterDefinitionProvider tests.
 */
public class ClusterDefinitionProviderTest {

  public static ClusterDefinitionProvider getProvider(String filename, String clusterName, String versionId) {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty(ClusterDefinitionProvider.SCOM_CLUSTER_DEFINITION_FILENAME, filename);
    ambariProperties.setProperty(ClusterDefinitionProvider.SCOM_CLUSTER_NAME, clusterName);
    ambariProperties.setProperty(ClusterDefinitionProvider.SCOM_VERSION_ID, versionId);

    Configuration configuration =  new TestConfiguration(ambariProperties);

    ClusterDefinitionProvider streamProvider = new ClusterDefinitionProvider();

    streamProvider.init(configuration);

    return streamProvider;
  }

  @Test
  public void testGetFileName() throws Exception {
    ClusterDefinitionProvider provider = getProvider("myFile", "myCluster", "myVersion");
    Assert.assertEquals("myFile", provider.getFileName());
  }

  @Test
  public void testGetClusterName() throws Exception {
    ClusterDefinitionProvider provider = getProvider("myFile", "myCluster", "myVersion");
    Assert.assertEquals("myCluster", provider.getClusterName());
  }

  @Test
  public void testGetVersionId() throws Exception {
    ClusterDefinitionProvider provider = getProvider("myFile", "myCluster", "myVersion");
    Assert.assertEquals("myVersion", provider.getVersionId());
  }

  @Test
  public void testGetInputStream() throws Exception {
    ClusterDefinitionProvider provider = getProvider("clusterproperties.txt", "myCluster", "myVersion");
    InputStream inputStream = provider.getInputStream();
    Assert.assertNotNull(inputStream);
  }

  private static class TestConfiguration extends Configuration {

    private TestConfiguration(Properties properties) {
      super(properties);
    }

    @Override
    protected void loadSSLParams() {
    }
  }
}
