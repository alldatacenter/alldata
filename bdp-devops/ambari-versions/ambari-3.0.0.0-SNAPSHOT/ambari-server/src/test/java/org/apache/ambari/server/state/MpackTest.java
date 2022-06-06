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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

public class MpackTest {
  @Test
  public void testMpacks() {
    Mpack mpack = new Mpack();
    mpack.setName("name");
    mpack.setResourceId((long)100);
    mpack.setDescription("desc");
    mpack.setVersion("3.0");
    mpack.setMpackUri("abc.tar.gz");
    mpack.setRegistryId(new Long(100));

    Assert.assertEquals("name", mpack.getName());
    Assert.assertEquals(new Long(100), mpack.getResourceId());
    Assert.assertEquals("desc", mpack.getDescription());
    Assert.assertEquals("abc.tar.gz", mpack.getMpackUri());
    Assert.assertEquals(new Long(100), mpack.getRegistryId());

  }

  @Test
  public void testMpacksUsingGson() {
    String mpackJsonContents = "{\n" +
            "  \"definition\": \"hdpcore-1.0.0-b16-definition.tar.gz\",\n" +
            "  \"description\": \"Hortonworks Data Platform Core\",\n" +
            "  \"id\": \"hdpcore\",\n" +
            "  \"modules\": [\n" +
            "    {\n" +
            "      \"category\": \"SERVER\",\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"id\": \"zookeeper_server\",\n" +
            "          \"version\": \"3.4.0.0-b17\",\n" +
            "          \"name\": \"ZOOKEEPER_SERVER\",\n" +
            "          \"category\": \"MASTER\",\n" +
            "          \"isExternal\": \"False\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"definition\": \"zookeeper-3.4.0.0-b17-definition.tar.gz\",\n" +
            "      \"dependencies\": [\n" +
            "        {\n" +
            "          \"id\": \"zookeeper_clients\",\n" +
            "          \"name\": \"ZOOKEEPER_CLIENTS\",\n" +
            "          \"dependencyType\": \"INSTALL\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"description\": \"Centralized service which provides highly reliable distributed coordination\",\n" +
            "      \"displayName\": \"ZooKeeper\",\n" +
            "      \"id\": \"zookeeper\",\n" +
            "      \"name\": \"ZOOKEEPER\",\n" +
            "      \"version\": \"3.4.0.0-b17\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"name\": \"HDPCORE\",\n" +
            "  \"prerequisites\": {\n" +
            "    \"max-ambari-version\": \"3.1.0.0\",\n" +
            "    \"min-ambari-version\": \"3.0.0.0\"\n" +
            "  },\n" +
            "  \"version\": \"1.0.0-b16\"\n" +
            "}";
    HashMap<String, String> expectedPrereq = new HashMap<>();
    expectedPrereq.put("min-ambari-version","3.0.0.0");

    expectedPrereq.put("max-ambari-version","3.1.0.0");
    ArrayList<Module> expectedModules = new ArrayList<>();
    Module zkfc = new Module();
    //nifi.setType(.PackletType.SERVICE_PACKLET);
    zkfc.setVersion("3.4.0.0-b17");
    zkfc.setDefinition("zookeeper-3.4.0.0-b17-definition.tar.gz");
    zkfc.setName("ZOOKEEPER");
    zkfc.setId("zookeeper");
    zkfc.setDisplayName("ZooKeeper");
    zkfc.setDescription("Centralized service which provides highly reliable distributed coordination");
    zkfc.setCategory(Module.Category.SERVER);
    ModuleDependency moduleDependency = new ModuleDependency();
    moduleDependency.setId("zookeeper_clients");
    moduleDependency.setName("ZOOKEEPER_CLIENTS");
    moduleDependency.setDependencyType(ModuleDependency.DependencyType.INSTALL);
    ArrayList moduleDepList = new ArrayList();
    moduleDepList.add(moduleDependency);
    zkfc.setDependencies(moduleDepList);
    ArrayList compList = new ArrayList();
    ModuleComponent zk_server = new ModuleComponent();
    zk_server.setId("zookeeper_server");
    zk_server.setName("ZOOKEEPER_SERVER");
    zk_server.setCategory("MASTER");
    zk_server.setIsExternal(Boolean.FALSE);
    zk_server.setVersion("3.4.0.0-b17");
    compList.add(zk_server);
    zkfc.setComponents(compList);
    expectedModules.add(zkfc);

    Gson gson = new Gson();
    Mpack mpack = gson.fromJson(mpackJsonContents, Mpack.class);
    Assert.assertEquals("HDPCORE", mpack.getName());
    Assert.assertEquals("1.0.0-b16", mpack.getVersion());
    Assert.assertEquals("Hortonworks Data Platform Core", mpack.getDescription());
    Assert.assertEquals(expectedPrereq, mpack.getPrerequisites());
    Assert.assertEquals(expectedModules.toString(), mpack.getModules().toString());
  }

}
