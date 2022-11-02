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
package org.apache.ambari.server.api.resources;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;

public class ConfigGroupResourceDefinition extends BaseResourceDefinition {
  /**
   * Constructor.
   */
  public ConfigGroupResourceDefinition() {
    super(Resource.Type.ConfigGroup);
  }

  @Override
  public String getPluralName() {
    return "config_groups";
  }

  @Override
  public String getSingularName() {
    return "config_group";
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = super.getPostProcessors();
    listProcessors.add(new ConfigGroupHrefProcessor());

    return listProcessors;
  }

  private class ConfigGroupHrefProcessor extends BaseHrefPostProcessor {

    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      if (resultNode.getObject().getType() == Resource.Type.ConfigGroup) {

        Resource r = resultNode.getObject();
        Schema schema = ClusterControllerHelper.getClusterController().getSchema(r.getType());
        Object clusterId = r.getPropertyValue(schema.getKeyPropertyId
          (Resource.Type.Cluster));

        Map<String, Object> configGroup = r.getPropertiesMap().get("ConfigGroup");
        String partialUrl = href.substring(0, href.indexOf("/clusters/")
          + "/clusters/".length()) + clusterId;

        for (Map.Entry<String, Object> entry : configGroup.entrySet()) {
          if (entry.getKey().contains("hosts") && entry.getValue() != null) {
            Set<Map<String, Object>> hostSet = (Set<Map<String, Object>>) entry.getValue();

            for (Map<String, Object> hostMap : hostSet) {
              String idx = partialUrl + "/hosts/" + hostMap.get("host_name");
              hostMap.put("href", idx);
            }

          } else if (entry.getKey().contains("desired_configs") && entry
            .getValue() != null) {

            Set<Map<String, Object>> configSet = (Set<Map<String,
              Object>>) entry.getValue();

            for (Map<String, Object> configMap : configSet) {
              String idx = partialUrl + "/configurations?"
                + "type=" + configMap.get("type")
                + "&tag=" + configMap.get("tag");
              configMap.put("href", idx);
            }
          }

        }
      } else {
        super.process(request, resultNode, href);
      }
    }
  }
}
