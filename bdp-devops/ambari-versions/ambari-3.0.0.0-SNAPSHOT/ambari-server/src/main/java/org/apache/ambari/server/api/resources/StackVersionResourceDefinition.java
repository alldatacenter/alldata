/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;

public class StackVersionResourceDefinition extends BaseResourceDefinition {

  public StackVersionResourceDefinition() {
    super(Resource.Type.StackVersion);
  }

  @Override
  public String getPluralName() {
    return "versions";
  }

  @Override
  public String getSingularName() {
    return "version";
  }

  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {

    Set<SubResourceDefinition> children = new HashSet<>();

    children.add(new SubResourceDefinition(Resource.Type.OperatingSystem));
    children.add(new SubResourceDefinition(Resource.Type.StackService));
    children.add(new SubResourceDefinition(Resource.Type.StackLevelConfiguration));
    children.add(new SubResourceDefinition(Resource.Type.RepositoryVersion));
    children.add(new SubResourceDefinition(Resource.Type.StackArtifact));
    children.add(new SubResourceDefinition(Resource.Type.CompatibleRepositoryVersion));
    children.add(new SubResourceDefinition(Resource.Type.Mpack, null, false));
    return children;
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = new ArrayList<>();
    listProcessors.add(new StackVersionHrefProcessor());
    listProcessors.add(new StackVersionPostProcessor());
    return listProcessors;
  }

  /**
   * Post Processing the mpack href when the call comes from stack endpoint to ensure that the
   * href is a backreference to the mpacks end point
   */
  private class StackVersionHrefProcessor extends BaseHrefPostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      if (href.contains("/mpacks/")) {
        ResourceImpl mpack = (ResourceImpl) resultNode.getObject();
        Map<String, Map<String, Object>> mapInfo = mpack.getPropertiesMap();
        Map<String, Object> versionInfo = mapInfo.get("Versions");

        int idx = href.indexOf("mpacks/");
        String stackName = (String)versionInfo.get("stack_name");
        String stackVersion = (String)versionInfo.get("stack_version");
        href = href.substring(0, idx) + "stacks/" + stackName + "/versions/" + stackVersion;
        resultNode.setProperty("href", href);
      } else {
        super.process(request, resultNode, href);
      }
    }
  }

  /***
   * Post processing to change the name of the result node to stack
   */
  private class StackVersionPostProcessor implements PostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      if (href.contains("/mpacks/")) {
        resultNode.setName("stack");
      }
    }
  }

 }
