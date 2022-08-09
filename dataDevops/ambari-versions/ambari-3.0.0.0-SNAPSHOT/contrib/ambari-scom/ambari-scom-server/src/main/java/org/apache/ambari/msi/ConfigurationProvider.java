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
package org.apache.ambari.msi;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Configuration provider for a MSI defined cluster.
 */
public class ConfigurationProvider extends BaseResourceProvider {

  protected static final String CONFIGURATION_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Config", "cluster_name");
  public static final String CONFIGURATION_CONFIG_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId(null, "type");
  public static final String CONFIGURATION_CONFIG_TAG_PROPERTY_ID = PropertyHelper.getPropertyId(null, "tag");

  private Map<String, Map<String, String>> allConfigs;

  private static final String DESTINATION = "xml";
  private static final Set<String> clusterConfigurationResources = new HashSet<String>();

  static {
    clusterConfigurationResources.add("hdfs-site");
    clusterConfigurationResources.add("mapred-site");
    clusterConfigurationResources.add("hbase-site");
    clusterConfigurationResources.add("yarn-site");
    clusterConfigurationResources.add("core-site");
  }

  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public void updateProperties(Resource resource, Request request, Predicate predicate) {
    // Do nothing
  }

  @Override
  public int updateProperties(Resource resource, Map<String, Object> properties) {
    // Do nothing
    return -1;
  }

  public ConfigurationProvider(ClusterDefinition clusterDefinition) {
    super(Resource.Type.Configuration, clusterDefinition);
    init();
    initConfigurationResources();
  }

  class ScomConfigConverter implements Converter {
    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
      Map<String, String> map = new HashMap<String, String>();

      while (hierarchicalStreamReader.hasMoreChildren()) {
        hierarchicalStreamReader.moveDown();
        String name = "", value = "";
        while (hierarchicalStreamReader.hasMoreChildren()) {
          hierarchicalStreamReader.moveDown();
          if ("name".equalsIgnoreCase(hierarchicalStreamReader.getNodeName())) {
            name = hierarchicalStreamReader.getValue();
          }
          if ("value".equalsIgnoreCase(hierarchicalStreamReader.getNodeName())) {
            value = hierarchicalStreamReader.getValue();
          }
          hierarchicalStreamReader.moveUp();
        }

        if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
          map.put(name, value);
        }
        hierarchicalStreamReader.moveUp();
      }

      return map;
    }

    @Override
    public boolean canConvert(Class aClass) {
      return AbstractMap.class.isAssignableFrom(aClass);
    }
  }

  @SuppressWarnings("unchecked")
  private void init() {
    allConfigs = new HashMap<String, Map<String, String>>();

    XStream xstream = new XStream(new StaxDriver());
    xstream.alias("configuration", Map.class);
    xstream.registerConverter(new ScomConfigConverter());

    for (String configurationResource : clusterConfigurationResources) {
      String configFileName = configurationResource + "." + DESTINATION;
      InputStream is = ClassLoader.getSystemResourceAsStream(configFileName);
      if (is == null) continue;
      Map<String, String> properties = (HashMap<String, String>) xstream.fromXML(is);
      allConfigs.put(configurationResource, properties);
    }
  }

  private void initConfigurationResources() {
    String clusterName = getClusterDefinition().getClusterName();

    for (String type : allConfigs.keySet()) {
      Resource resource = new ResourceImpl(Resource.Type.Configuration);
      resource.setProperty(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID, clusterName);
      resource.setProperty(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID, type);
      resource.setProperty(CONFIGURATION_CONFIG_TAG_PROPERTY_ID, "version1");

      Map<String, String> properties = allConfigs.get(type);
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        String id = PropertyHelper.getPropertyId("properties", entry.getKey());
        resource.setProperty(id, entry.getValue());
      }

      addResource(resource);
    }
  }
}
