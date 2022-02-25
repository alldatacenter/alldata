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

package org.apache.ambari.server.controller.jmx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.URLStreamProvider;

public class TestStreamProvider extends URLStreamProvider {

  protected static Map<String, String> FILE_MAPPING = new HashMap<>();

  static {
    FILE_MAPPING.put("50070", "hdfs_namenode_jmx.json");
    FILE_MAPPING.put("50075", "hdfs_datanode_jmx.json");
    FILE_MAPPING.put("50030", "mapreduce_jobtracker_jmx.json");
    FILE_MAPPING.put("50060", "mapreduce_tasktracker_jmx.json");
    FILE_MAPPING.put("60010", "hbase_hbasemaster_jmx.json");
    FILE_MAPPING.put("60011", "hbase_hbasemaster_jmx_2.json");
    FILE_MAPPING.put("8088",  "resourcemanager_jmx.json");
    FILE_MAPPING.put("8480",  "hdfs_journalnode_jmx.json");
    FILE_MAPPING.put("8745",  "storm_rest_api_jmx.json");
  }

  private static String NN_HASTATE_ONLY_JMX = "hdfs_namenode_jmx_ha_only.json";

  /**
   * Delay to simulate response time.
   */
  protected final long delay;

  private String lastSpec;
  private List<String> specs = new ArrayList<>();

  private boolean isLastSpecUpdated;

  public TestStreamProvider() {
    super(1000, 1000, ComponentSSLConfiguration.instance());
    delay = 0;
  }

  public TestStreamProvider(long delay) {
    super(1000, 1000, ComponentSSLConfiguration.instance());
    this.delay = delay;
  }

  @Override
  public InputStream readFrom(String spec) throws IOException {
    specs.add(spec);
    if (!isLastSpecUpdated)
      lastSpec = spec;
    
    isLastSpecUpdated = false;

    String filename = null;
    if (spec.endsWith(":50070/jmx?get=Hadoop:service=NameNode,name=FSNamesystem::tag.HAState")) {
      filename = NN_HASTATE_ONLY_JMX;
    } else {
      filename = FILE_MAPPING.get(getPort(spec));
    }

    if (filename == null) {
      throw new IOException("Can't find JMX source for " + spec);
    }
    if (delay > 0) {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        // do nothing
      }
    }

    return ClassLoader.getSystemResourceAsStream(filename);
  }

  public String getLastSpec() {
    return lastSpec;
  }

  public List<String> getSpecs() {
    return specs;
  }

  private String getPort(String spec) {
    int colonIndex = spec.indexOf(":", 5);
    int slashIndex = spec.indexOf("/", colonIndex);

    return spec.substring(colonIndex + 1, slashIndex);
  }

  @Override
  public InputStream readFrom(String spec, String requestMethod, String params) throws IOException {
    lastSpec = spec + "?" + params;
    isLastSpecUpdated = true;
    return readFrom(spec);
  }
}
