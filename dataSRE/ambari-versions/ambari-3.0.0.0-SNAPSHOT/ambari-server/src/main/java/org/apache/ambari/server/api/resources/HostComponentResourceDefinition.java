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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

/**
 * Host_Component resource definition.
 */
public class HostComponentResourceDefinition extends BaseResourceDefinition {

  /**
   * Constructor.
   */
  public HostComponentResourceDefinition() {
    super(Resource.Type.HostComponent);
  }

  @Override
  public String getPluralName() {
    return "host_components";
  }

  @Override
  public String getSingularName() {
    return "host_component";
  }


  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {
    Set<SubResourceDefinition> setSubResources = new HashSet<>();

    setSubResources.add(new SubResourceDefinition(Resource.Type.Component,
        Collections.singleton(Resource.Type.Service), false));
   
    setSubResources.add(new SubResourceDefinition(Resource.Type.HostComponentProcess));
    
    return setSubResources;
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = new ArrayList<>();
    listProcessors.add(new HostComponentHrefProcessor());
    listProcessors.add(new HostComponentHostProcessor());

    return listProcessors;
  }
  /**
   * Host_Component resource processor which is responsible for generating href's for host components.
   * This is called by the ResultPostProcessor during post processing of a result.
   */
  private class HostComponentHrefProcessor extends BaseHrefPostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      if (! href.contains("/hosts/")) {
        Resource r = resultNode.getObject();
        Schema schema = ClusterControllerHelper.getClusterController().getSchema(r.getType());
        Object host = r.getPropertyValue(schema.getKeyPropertyId(Resource.Type.Host));
        Object hostComponent = r.getPropertyValue(schema.getKeyPropertyId(r.getType()));

        int idx = href.indexOf("clusters/") + "clusters/".length();
        idx = href.indexOf("/", idx) + 1;

        href = href.substring(0, idx) +
            "hosts/" + host + "/host_components/" + hostComponent;

        resultNode.setProperty("href", href);
      } else {
        super.process(request, resultNode, href);
      }
    }
  }

  /**
   * Host_Component resource processor which is responsible for generating a host section for host components.
   * This is called by the ResultPostProcessor during post processing of a result.
   */
  private class HostComponentHostProcessor implements PostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      //todo: look at partial request fields to ensure that hosts should be returned
      if (request.getResource().getResourceDefinition().getType() == getType()) {
        // only add host if query host_resource was directly queried
        String nodeHref = resultNode.getStringProperty("href");
        resultNode.getObject().setProperty(PropertyHelper.getPropertyId("host", "href"),
            nodeHref.substring(0, nodeHref.indexOf("/host_components/")));
      }
    }
  }
}
