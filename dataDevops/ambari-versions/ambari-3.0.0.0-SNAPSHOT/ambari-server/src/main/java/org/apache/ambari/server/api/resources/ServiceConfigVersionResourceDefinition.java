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

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;

public class ServiceConfigVersionResourceDefinition extends BaseResourceDefinition {
  /**
   * Constructor.
   *
   */
  public ServiceConfigVersionResourceDefinition() {
    super(Resource.Type.ServiceConfigVersion);
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = super.getPostProcessors();
    listProcessors.add(new HrefProcessor());

    return listProcessors;
  }

  @Override
  public String getPluralName() {
    return "service_config_versions";
  }

  @Override
  public String getSingularName() {
    return "service_config_version";
  }

  private class HrefProcessor extends BaseHrefPostProcessor {

    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      if (resultNode.getObject().getType() == Resource.Type.ServiceConfigVersion) {

        if (! href.endsWith("/")) {
          href += '/';
        }

        String clustersToken = "/clusters";
        int idx = href.indexOf(clustersToken) + clustersToken.length() + 1;
        idx = href.indexOf("/", idx) + 1;

        String serviceName = (String) resultNode.getObject().getPropertyValue("service_name");
        Long version = (Long) resultNode.getObject().getPropertyValue("service_config_version");
        href = href.substring(0, idx)
            + "configurations/service_config_versions?service_name="
            + serviceName + "&service_config_version=" + version;

        resultNode.setProperty("href", href);
      } else {
        super.process(request, resultNode, href);
      }

    }
  }
}
