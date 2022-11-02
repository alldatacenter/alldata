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
import java.util.List;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;

public class RootServiceHostComponentResourceDefinition extends
    BaseResourceDefinition {

  public RootServiceHostComponentResourceDefinition(Type resourceType) {
    super(resourceType);
  }

  public RootServiceHostComponentResourceDefinition() {
    super(Resource.Type.RootServiceHostComponent);
  }

  @Override
  public String getPluralName() {
    return "hostComponents";
  }

  @Override
  public String getSingularName() {
    return "hostComponent";
  }
  
  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = new ArrayList<>();
    listProcessors.add(new RootServiceHostComponentHrefProcessor());

    return listProcessors;
  }
  
  private class RootServiceHostComponentHrefProcessor extends BaseHrefPostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      Resource r = resultNode.getObject();
      Schema schema = ClusterControllerHelper.getClusterController().getSchema(r.getType());
      Object host = r.getPropertyValue(schema.getKeyPropertyId(Resource.Type.Host));
      Object hostComponent = r.getPropertyValue(schema.getKeyPropertyId(r.getType()));

      int idx = href.indexOf("services/") + "services/".length();
      idx = href.indexOf("/", idx) + 1;

      href = href.substring(0, idx) + "hosts/" + host + "/hostComponents/" + hostComponent;

      resultNode.setProperty("href", href);
    }
  }
}
