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
import java.util.HashMap;
import java.util.List;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.WidgetResponse;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Resource Definition for WidgetLayout types.
 */
public class WidgetLayoutResourceDefinition extends BaseResourceDefinition {

  public WidgetLayoutResourceDefinition() {
    super(Resource.Type.WidgetLayout);
  }

  @Override
  public String getPluralName() {
    return "widget_layouts";
  }

  @Override
  public String getSingularName() {
    return "widget_layout";
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = super.getPostProcessors();
    listProcessors.add(new WidgetLayoutHrefProcessor());

    return listProcessors;
  }

  /**
   * Base resource processor which generates href's.  This is called by the
   * {@link org.apache.ambari.server.api.services.ResultPostProcessor} during post processing of a result.
   */
  private class WidgetLayoutHrefProcessor extends BaseHrefPostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      TreeNode<Resource> parent = resultNode.getParent();

      for (TreeNode<Resource> node : parent.getChildren()) {
        if (node.getObject().getPropertiesMap().get("WidgetLayoutInfo") != null &&
                node.getObject().getPropertiesMap().get("WidgetLayoutInfo").get("widgets") != null) {

          ArrayList widgetsList = (ArrayList) node.getObject().getPropertiesMap().get("WidgetLayoutInfo").get("widgets");
          for (Object widgetObject : widgetsList) {
            HashMap<String, Object> widgetMap = (HashMap) widgetObject;
            if (((WidgetResponse) widgetMap.get("WidgetInfo")).getId() != null) {
              String widgetId = ((WidgetResponse) widgetMap.get("WidgetInfo")).getId().toString();
              String widgetHref = href.substring(0, href.indexOf("/widget_layouts") + 1) +
                      "widgets/" + widgetId;
              widgetMap.put("href", widgetHref);
            }
          }
        }
      }
    }
  }
}
