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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;

/**
 * Component resource definition.
 */
public class ComponentResourceDefinition extends BaseResourceDefinition {

  /**
   * Constructor.
   */
  public ComponentResourceDefinition() {
    super(Resource.Type.Component);
  }

  @Override
  public String getPluralName() {
    return "components";
  }

  @Override
  public String getSingularName() {
    return "component";
  }


  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {
    return Collections.singleton(new SubResourceDefinition(
        Resource.Type.HostComponent, Collections.singleton(Resource.Type.Host), true));
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = super.getPostProcessors();
    listProcessors.add(new ComponentHrefProcessor());

    return listProcessors;
  }

  /**
   * Base resource processor which generates href's.  This is called by the
   * {@link org.apache.ambari.server.api.services.ResultPostProcessor} during post processing of a result.
   */
  private class ComponentHrefProcessor extends BaseHrefPostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      TreeNode<Resource> parent = resultNode.getParent();

      if (parent.getParent() != null && parent.getParent().getObject().getType() == Resource.Type.HostComponent) {
        Resource r = resultNode.getObject();
        Schema schema = ClusterControllerHelper.getClusterController().getSchema(r.getType());
        Object serviceId = r.getPropertyValue(schema.getKeyPropertyId(Resource.Type.Service));
        Object componentId = r.getPropertyValue(schema.getKeyPropertyId(r.getType()));

        href = href.substring(0, href.indexOf("/hosts/") + 1) +
            "services/" + serviceId + "/components/" + componentId;

        resultNode.setProperty("href", href);
      } else {
        super.process(request, resultNode, href);
      }
    }
  }
}
