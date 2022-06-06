/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.ambari.server.api.resources;

import static org.apache.ambari.server.controller.internal.CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID;

import java.util.List;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * ResourceDefinition with a custom href post processor which overrides the URL
 */
public class CompatibleRepositoryVersionDefinition extends SimpleResourceDefinition {

  public CompatibleRepositoryVersionDefinition() {
    super(Resource.Type.CompatibleRepositoryVersion,
      "compatible_repository_version", "compatible_repository_versions",
      Resource.Type.OperatingSystem);
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = super.getPostProcessors();
    listProcessors.add(new CompatibleRepositoryVersionHrefProcessor());
    return listProcessors;
  }

  private class CompatibleRepositoryVersionHrefProcessor extends BaseResourceDefinition.BaseHrefPostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      if (resultNode.getObject().getType() == Resource.Type.CompatibleRepositoryVersion) {
        Resource node = resultNode.getObject();
        Object id = node.getPropertyValue(REPOSITORY_VERSION_ID_PROPERTY_ID);
        Object stackVersion = node.getPropertyValue(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID);
        if (id != null && stackVersion != null) {
          resultNode.setProperty("href", fixHref(href, id, stackVersion));
          return;
        }
      }
      super.process(request, resultNode, href);
    }

    private String fixHref(String href, Object id, Object stackVersion) {
      href = href.replaceAll("/versions/[^/]+/", "/versions/" + stackVersion + "/");
      if (!href.endsWith("/" + id)) {
        href += "/" + id;
      }
      return href;
    }
  }
}
